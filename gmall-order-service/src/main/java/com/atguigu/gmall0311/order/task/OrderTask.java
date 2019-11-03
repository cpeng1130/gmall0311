package com.atguigu.gmall0311.order.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0311.bean.OrderInfo;
import com.atguigu.gmall0311.service.OrderService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// 定时任务
@Component
@EnableScheduling
public class OrderTask {

    @Reference
    private OrderService orderService;

    // 分时日月周
    // .bat shutdown -h
    // 每分钟的第五秒执行
//    @Scheduled(cron="5 * * * * ?")
//    public void test01(){
//        System.out.println("终于完成了！可以找工作挣钱了！");
//    }
//    // 每隔五秒执行
//    @Scheduled(cron="0/5 * * * * ?")
//    public void test02(){
//        System.out.println("哈哈-呵呵-嘿嘿-嘻嘻-嘎嘎！");
//    }
    @Scheduled(cron="0/20 * * * * ?")
    public void checkOrder(){
        /*
        1.  查询一下过期订单
        2.  循环处理过期订单
         */
       List<OrderInfo> orderInfoList = orderService.getExpiredOrderList();

       if (orderInfoList!=null && orderInfoList.size()>0){
           for (OrderInfo orderInfo : orderInfoList) {
               // 调用处理过期订单方法
               orderService.execExpiredOrder(orderInfo);
           }
       }
    }


}
