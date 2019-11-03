package com.atguigu.gmall0311.payment.serivce.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall0311.bean.OrderInfo;
import com.atguigu.gmall0311.bean.PaymentInfo;
import com.atguigu.gmall0311.bean.enums.PaymentStatus;
import com.atguigu.gmall0311.config.ActiveMQUtil;
import com.atguigu.gmall0311.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall0311.service.OrderService;
import com.atguigu.gmall0311.service.PaymentService;
import com.atguigu.gmall0311.util.HttpClient;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    
    @Reference
    private OrderService orderService;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    // 服务号Id
    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String partner;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;


    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {

        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery) {
        return paymentInfoMapper.selectOne(paymentInfoQuery);
    }

    @Override
    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUPD) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);

        paymentInfoMapper.updateByExampleSelective(paymentInfoUPD,example);
    }

    @Override
    public boolean refund(String orderId) {
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        // 根据orderId 查询OrderInfo
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        // 设置map 封装参数
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("refund_amount",0.01);
        map.put("refund_reason","不够绿");
        map.put("out_request_no","HZ01RF001");
        request.setBizContent(JSON.toJSONString(map));
//        request.setBizContent("{" +
//                "\"out_trade_no\":\"20150320010101001\"," +
//                "\"trade_no\":\"2014112611001004680073956707\"," +
//                "\"refund_amount\":200.12," +
//                "\"refund_currency\":\"USD\"," +
//                "\"refund_reason\":\"正常退款\"," +
//                "\"out_request_no\":\"HZ01RF001\"," +
//                "\"operator_id\":\"OP001\"," +
//                "\"store_id\":\"NJ_S_001\"," +
//                "\"terminal_id\":\"NJ_T_001\"," +
//                "      \"goods_detail\":[{" +
//                "        \"goods_id\":\"apple-01\"," +
//                "\"alipay_goods_id\":\"20010001\"," +
//                "\"goods_name\":\"ipad\"," +
//                "\"quantity\":1," +
//                "\"price\":2000," +
//                "\"goods_category\":\"34543238\"," +
//                "\"categories_tree\":\"124868003|126232002|126252004\"," +
//                "\"body\":\"特价手机\"," +
//                "\"show_url\":\"http://www.alipay.com/xxx.jpg\"" +
//                "        }]," +
//                "      \"refund_royalty_parameters\":[{" +
//                "        \"royalty_type\":\"transfer\"," +
//                "\"trans_out\":\"2088101126765726\"," +
//                "\"trans_out_type\":\"userId\"," +
//                "\"trans_in_type\":\"userId\"," +
//                "\"trans_in\":\"2088101126708402\"," +
//                "\"amount\":0.1," +
//                "\"amount_percentage\":100," +
//                "\"desc\":\"分账给2088101126708402\"" +
//                "        }]," +
//                "\"org_pid\":\"2088101117952222\"" +
//                "  }");
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){

            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }

    }

    @Override
    public Map createNative(String orderId, String totalAmount) {
        // 根据 api 文档 制作参数
        // 将参数以xml 发送给支付接口
        // 获取支付的结果
        HashMap<String, String> map = new HashMap<>();
        map.put("appid",appid);
        map.put("mch_id",partner);
        map.put("nonce_str", WXPayUtil.generateNonceStr());
        map.put("body","买袜子");
        map.put("out_trade_no",orderId);
        map.put("total_fee",totalAmount);
        map.put("spbill_create_ip","127.0.0.1");
        map.put("notify_url","http://trade.gmall.com/trade");
        map.put("trade_type","NATIVE");

        try {
            // 将map 转换为xml
            String xmlParam  = WXPayUtil.generateSignedXml(map, partnerkey);
            // 发送到接口上
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            // 设置https
            httpClient.setHttps(true);
            // 发送xmlParam
            httpClient.setXmlParam(xmlParam);
            // 设置发送方式
            httpClient.post();

            // 获取结果
            String content = httpClient.getContent();
            // 将其转换为map
            Map<String, String> xmlToMap = WXPayUtil.xmlToMap(content);

            // 声明一个map 来存储需要使用的信息
            HashMap<String, Object> resultMap = new HashMap<>();
            // 获取xmlToMap 中的数据
            resultMap.put("code_url",xmlToMap.get("code_url"));
            resultMap.put("total_fee",totalAmount);
            resultMap.put("out_trade_no",orderId);

            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }

    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        // 创建链接，并打开链接
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            // 创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 创建队列
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            // 创建消息提供者
            MessageProducer producer = session.createProducer(payment_result_queue);
            // 创建消息对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("orderId",paymentInfo.getOrderId());
            activeMQMapMessage.setString("result",result);

            // 发送消息
            producer.send(activeMQMapMessage);
            // 提交
            session.commit();
            // 关闭
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }


    }

    @Override
    public boolean checkPayment(PaymentInfo paymentInfoQuery) {
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        // 什么一个map 集合来存储数据
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfoQuery.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        // 指的在支付宝中有该笔交易 相当于异步回调
        if(response.isSuccess()){
            System.out.println("调用成功");
            if ("TRADE_SUCCESS".equals(response.getTradeStatus()) || "TRADE_FINISHED".equals(response.getTradeStatus())){
                System.out.println("支付成功");
                // 更新交易记录的状态 paymentInfo
                // 改支付状态
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                updatePaymentInfo(paymentInfoQuery.getOutTradeNo(),paymentInfoUpd);
                // paymentInfoQuery 必须有orderId 才能正常发送消息队列给订单！
                sendPaymentResult(paymentInfoQuery,"success");
                return true;
            }

        } else {
            System.out.println("调用失败");
            return  false;
        }

        return false;
    }

    // 生成二维码之后调用该方法
    @Override
    public void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount) {
        // 创建工厂连接，并打开连接
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            // 创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 创建队列
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            // 创建消息提供者
            MessageProducer producer = session.createProducer(payment_result_check_queue);

            // 创建发送消息的对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("outTradeNo",outTradeNo);
            activeMQMapMessage.setInt("delaySec",delaySec);
            activeMQMapMessage.setInt("checkCount",checkCount);

            // 设置延迟队列的时间
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delaySec*1000);
            // 发送消息
            producer.send(activeMQMapMessage);

            //必须提交
            session.commit();

            // 关闭：
            producer.close();
            session.close();
            connection.close();




        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closePayment(String orderId) {
        // update paymentInfo set PaymentStatus=PaymentStatus.ClOSED where orderId = ?
        // paymentInfo 更新的内容
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);
        // example 设置条件
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId",orderId);
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }
}
