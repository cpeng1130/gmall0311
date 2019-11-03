package com.atguigu.gmall0311.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall0311.bean.OrderInfo;
import com.atguigu.gmall0311.bean.PaymentInfo;
import com.atguigu.gmall0311.bean.enums.PaymentStatus;
import com.atguigu.gmall0311.payment.config.AlipayConfig;
import com.atguigu.gmall0311.payment.config.IdWorker;
import com.atguigu.gmall0311.service.OrderService;
import com.atguigu.gmall0311.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.alipay.api.AlipayConstants.SIGN_TYPE;
import static org.apache.catalina.manager.Constants.CHARSET;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;
    
    @Reference
    private PaymentService paymentService;

    @Autowired
    private  AlipayClient alipayClient;

    @RequestMapping("index")
    public String index(HttpServletRequest request){
        String orderId = request.getParameter("orderId");

        OrderInfo orderInfo =  orderService.getOrderInfo(orderId);
        // 存储总金额
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        // 存储订单Id
        request.setAttribute("orderId",orderId);
        return "index";
    }

    @RequestMapping("alipay/submit")
    @ResponseBody
    public String alipaySubmit(HttpServletRequest request, HttpServletResponse response){
        // 数据保存！ 将交易信息保存到paymentInfo 中
        PaymentInfo paymentInfo = new PaymentInfo();
        // paymentInfo 数据来源应该在orderInfo
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo =  orderService.getOrderInfo(orderId);

        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject("买袜子--绿色的！");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setCreateTime(new Date());
        paymentService.savePaymentInfo(paymentInfo);
        // 显示二维码
        // AlipayClient 注入到spring 容器中！
        //  AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE); //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        // 同步回调路径
        // https://www.domain.com/CallBack/return_url?out_trade_no=ATGUIGU1567231189728666&version=1.0&app_id=2018020102122556&charset=utf-8&sign_type=RSA2&trade_no=2019083122001495390503588861&auth_app_id=2018020102122556&timestamp=2019-08-31%2014:57:09&seller_id=2088921750292524&method=alipay.trade.page.pay.return&total_amount=0.01&sign=Q45/PJO2/qiiP5kEyGU29hTu2VDMYmCMG1kjFMynyuNtZDDHbITintsvFx9eUDeySBvFRjgD4jUZgyFxnhPy6IoEd4CeoyZbHvYVFwTBGoUPSwYSlMctfLZ/+MZ9cIXmnliyYJwEWDvIkBuZ60oAA4Rg6ptv3oDMlkUbjoER1bZH/7DW8q+BHoARHLIVY90mhIhPeeqgNSY6bBBVdx/GzuCcaHr81h0+PRRPdGkM3AlIC9XAZrfIpkLeT80OSXZmsYg6+EE4esKQ3uO67CjvWsJkqpwcFwII6S1eUP1v5k9Q5MYnQ0TKs40u90z6MpokLF5yt5O6VENB1sNVZ2mlUg==
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 异步回调路径
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址
        // 设置参数

        // 什么一个map集合
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",paymentInfo.getTotalAmount());
        map.put("subject",paymentInfo.getSubject());
        alipayRequest.setBizContent(JSON.toJSONString(map));
//            alipayRequest.setBizContent("{" +
//                    "    \"out_trade_no\":\"20150320010101001\"," +
//                    "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
//                    "    \"total_amount\":88.88," +
//                    "    \"subject\":\"Iphone6 16G\"," +
//                    "    \"body\":\"Iphone6 16G\"," +
//                    "    \"passback_params\":\"merchantBizType%3d3C%26merchantBizNo%3d2016010101111\"," +
//                    "    \"extend_params\":{" +
//                    "    \"sys_service_provider_id\":\"2088511833207846\"" +
//                    "    }"+
//                    "  }");//填充业务参数
        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
    response.setContentType("text/html;charset=" + CHARSET);
    /*response.getWriter().write(form);//直接将完整的表单html输出到页面
    response.getWriter().flush();
    response.getWriter().close();*/
    // 每隔15秒，发送一个队列，检查3次
    paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);
    return form;

    }

    // 同步回调：
    @RequestMapping("alipay/callback/return")
    public String callbackReturn(){
        // 返回订单页面！
        // 清空购物车 。。。 不清楚！jeids.del(key);
        return "redirect:"+AlipayConfig.return_order_url;
    }
    // 异步回调：通知商家是否支付成功！
    // springMVC
    @RequestMapping("alipay/callback/notify")
    public String callbackNotify(@RequestParam Map<String,String> paramMap, HttpServletRequest request){

        // Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        try {
            boolean flag = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, CHARSET, AlipayConfig.sign_type); //调用SDK验证签名
            // 交易状态
            String trade_status = paramMap.get("trade_status");
            // 获取交易编号
            String out_trade_no = paramMap.get("out_trade_no");
            if(flag){
                // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure

                if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                    // 进一步判断：记录交易日志表的交易状态
                    // select * from paymentInfo where out_trade_no = ?
                    // 调用服务层
                    PaymentInfo paymentInfoQuery = new PaymentInfo();
                    paymentInfoQuery.setOutTradeNo(out_trade_no);
                    PaymentInfo paymentInfo = paymentService.getPaymentInfo(paymentInfoQuery);

                    if (paymentInfo.getPaymentStatus()==PaymentStatus.PAID || paymentInfo.getPaymentStatus()==PaymentStatus.ClOSED){
                        return "failure";
                    }
                    // 支付成功应该修改交易记录状态
                    // update paymentInfo set getPaymentStatus = aymentStatus.PAID where out_trade_no=out_trade_no
                    PaymentInfo paymentInfoUPD = new PaymentInfo();
                    paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);
                    paymentInfoUPD.setCreateTime(new Date());
                    // 更新交易记录
                    paymentService.updatePaymentInfo(out_trade_no,paymentInfoUPD);
                    // 支付成功！订单状态变成支付！ 发送消息队列！
                    paymentService.sendPaymentResult(paymentInfoQuery,"result");
                    return "success";
                }
            }else{
                // TODO 验签失败则记录异常日志，并在response中返回failure.
                return "failure";
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return "failure";
    }

    // payment.gmall.com/refund?orderId=100
    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId){
        // 调用服务层接口
        boolean result = paymentService.refund(orderId);
        return ""+result;
    }

    /*
        class Student{
            private int id; id=1
            private String name; name=admin
        }

        Map map = new HashMap();
        map.put(id,1);
        map.put(name,admin);

     */
    @RequestMapping("wx/submit")
    @ResponseBody
    public Map createNative(HttpServletRequest request){
        /*
        1.  获取交易记录状态：PAID
        2.  PAID :
         */
        /*
        IF(!PAID){
            // 微信支付 缓存中 orderId ，status！
        }else{
            //
        }

         */

//        String orderId = request.getParameter("orderId");
        IdWorker idWorker = new IdWorker();
        long id = idWorker.nextId();
        // 调用服务层 固定值：1 分
        Map map = paymentService.createNative(id+"","1");
        System.out.println(map.get("code_url"));
        return map;
    }

    @RequestMapping("bank")
    @ResponseBody
    public Map bank(){
        // 第三方接口：
        // 密钥，参数：总金额，商户Id，订单Id，。。。、

        return null;
    }

    // payment.gmall.com/sendPaymentResult?orderId=xxx&result=xxx
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,String result){

        paymentService.sendPaymentResult(paymentInfo,result);
        return "ok";
    }

    // 查询当前交易是否已经付款
    // payment.gmall.com/queryPaymentResult?orderId=xxx
    // 当用户支付时，不能确定是否支付成功的时候，可以联系客服！
    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(PaymentInfo paymentInfo){
        // 需要根据orderId 查询出整个paymentInfo 对象
        // select * from paymentInfo where orderId = ?
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);
        boolean result = false;
        if (paymentInfoQuery!=null){
            // 需要的outTradeNo ,orderId
            result = paymentService.checkPayment(paymentInfoQuery);
        }
        // 返回结果
        return ""+result;
    }



}
