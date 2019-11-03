package com.atguigu.gmall0311.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0311.bean.OrderDetail;
import com.atguigu.gmall0311.bean.OrderInfo;
import com.atguigu.gmall0311.bean.enums.OrderStatus;
import com.atguigu.gmall0311.bean.enums.ProcessStatus;
import com.atguigu.gmall0311.config.ActiveMQUtil;
import com.atguigu.gmall0311.config.RedisUtil;
import com.atguigu.gmall0311.order.mapper.OrderDetailMapper;
import com.atguigu.gmall0311.order.mapper.OrderInfoMapper;
import com.atguigu.gmall0311.service.OrderService;
import com.atguigu.gmall0311.service.PaymentService;
import com.atguigu.gmall0311.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;
    
    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Reference
    private PaymentService paymentService;

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {

        // 数据库表结构：orderInfo orderDetail
        // 总金额，订单状态，userId ,第三方交易编号，创建时间，过期时间，进程状态
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        // 第三方交易编号
        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setCreateTime(new Date());
        // 过期时间为下订单之后的1天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        // orderInfo
        orderInfoMapper.insertSelective(orderInfo);

        // orderDetail
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (orderDetailList!=null && orderDetailList.size()>0){
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setId(null);
                // 设置订单Id
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insertSelective(orderDetail);
            }
        }
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();

        // 定义key
        String tradeNoKey = "user:"+userId+":tradeCode";

        // 定义流水号
        String tradeNo = UUID.randomUUID().toString();

        jedis.set(tradeNoKey,tradeNo);

        jedis.close();

        return tradeNo;
    }

    @Override
    public boolean checkTradeNo(String tradeNo, String userId) {
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();

        // 定义key
        String tradeNoKey = "user:"+userId+":tradeCode";

        // 获取缓存的流水号
        String tradeNoCode = jedis.get(tradeNoKey);
        // 关闭jedis
        jedis.close();

        return tradeNo.equals(tradeNoCode);
    }

    @Override
    public void delTradeNo(String userId) {
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();

        // 定义key
        String tradeNoKey = "user:"+userId+":tradeCode";

        jedis.del(tradeNoKey);

        jedis.close();

    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        // http://www.gware.com/hasStock?skuId=10221&num=2
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        // 通过orderId 获取数据
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        // 获取orderDetail集合放入orderInfo
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        // 创建链接并打开
        Connection connection = activeMQUtil.getConnection();
        // 获取json字符串
        String orderJson = initWareOrder(orderId);

        try {
            connection.start();
            // 创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 创建对象
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");

            // 创建消息提供者
            MessageProducer producer = session.createProducer(order_result_queue);
            // 创建消息对象
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();

            activeMQTextMessage.setText(orderJson);
            // 发送消息
            producer.send(activeMQTextMessage);

            // 提交
            session.commit();

            //关闭
            producer.close();
            session.close();
            connection.close();


        } catch (JMSException e) {
            e.printStackTrace();
        }
        // 消息队列名称
    }

    @Override
    public List<OrderInfo> getExpiredOrderList() {
        // 过期时间<当前时间 AND 未支付
        Example example = new Example(OrderInfo.class);
        // 构建查询条件
        example.createCriteria().andLessThan("expireTime",new Date()).andEqualTo("processStatus",ProcessStatus.UNPAID);
        List<OrderInfo> orderInfoList = orderInfoMapper.selectByExample(example);
        return orderInfoList;
    }

    @Override
    @Async
    public void execExpiredOrder(OrderInfo orderInfo) {
        // 更新状态
        updateOrderStatus(orderInfo.getId(),ProcessStatus.CLOSED);
        // 关闭交易记录信息
        paymentService.closePayment(orderInfo.getId());
    }

    // 发送的数据
    private String initWareOrder(String orderId) {
        // 根据orderId 查询orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        // 将orderInfo 转换为map
        Map map = initWareOrder(orderInfo);

        return JSON.toJSONString(map);

    }

    /**
     * 将orderInfo 转换为map
     * @param orderInfo
     * @return
     */
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody","test--测试");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId());
        // 获取orderDetail集合数据
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        // 声明一个集合来存储orderDetailMap
        ArrayList<Map> mapArrayList = new ArrayList<>();
        if (orderDetailList!=null && orderDetailList.size()>0){
            for (OrderDetail orderDetail : orderDetailList) {
                // {skuId:101,skuNum:1,skuName:’小米手64G’} {skuId:201,skuNum:1,skuName:’索尼耳机’}
                HashMap<String, Object> orderDetailMap = new HashMap<>();
                orderDetailMap.put("skuId",orderDetail.getSkuId());
                orderDetailMap.put("skuNum",orderDetail.getSkuNum());
                orderDetailMap.put("skuName",orderDetail.getSkuName());
                mapArrayList.add(orderDetailMap);

            }
        }
        map.put("details",mapArrayList);
        return map;

    }

    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {

        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        /*
        1.  获取原始订单
        2.  需要将wareSkuMap[{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}] 中的数据判断是否需要拆单并写拆单规则
            wareSkuMap 转换为我们能操作的对象
        3.  创建新的子订单
        4.  给新的子订单赋值
        5.  保存子订单
        6.  将子订单添加到集合中List<OrderInfo>
        7.  更新原始订单的状态！
         */
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        //  wareSkuMap 转换为我们能操作的对象
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        if (maps!=null && maps.size()>0){
            for (Map map : maps) {
                // {"wareId":"1","skuIds":["2","10"]}
                // 仓库Id
                String wareId = (String) map.get("wareId");
                // skuIds {"wareId":"1","skuIds":["2","10"]
                List<String> skuIds = (List<String>) map.get("skuIds");
                // 创建新的子订单
                OrderInfo subOrderInfo  = new OrderInfo();
                // 属性赋值
                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
                // 防止主键冲突
                subOrderInfo.setId(null);
                // 声明一个集合来存储子订单明细
                ArrayList<OrderDetail> orderDetailsList = new ArrayList<>();
                // 价格：必须有订单明细
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
                if (orderDetailList!=null && orderDetailList.size()>0){
                    // 根据skuId 进行判断
                    for (OrderDetail orderDetail : orderDetailList) {
                        for (String skuId : skuIds) {
                            if (orderDetail.getSkuId().equals(skuId)){
                                // 新的子订单明细
                                orderDetailsList.add(orderDetail);
                            }
                        }
                    }
                }
                subOrderInfo.setOrderDetailList(orderDetailsList);
                // 计算总价格
                subOrderInfo.sumTotalAmount();
                // 赋值仓库Id
                subOrderInfo.setWareId(wareId);
                // 赋值父订单Id
                subOrderInfo.setParentOrderId(orderId);

                // 保存子订单
                saveOrder(subOrderInfo);
                // 添加子订单
                subOrderInfoList.add(subOrderInfo);

            }
        }
        // 修改原始订单的状态！
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        return subOrderInfoList;
    }


}
