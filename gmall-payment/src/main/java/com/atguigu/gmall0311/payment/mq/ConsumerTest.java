package com.atguigu.gmall0311.payment.mq;

import com.atguigu.gmall0311.config.ActiveMQUtil;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.*;

// 不在容器总！
@Component
public class ConsumerTest {
//
//    @Autowired
//    private ActiveMQUtil activeMQUtil;
    public static void main(String[] args) throws JMSException {
        /*
        1.  创建消息队列工厂
        2.  获取连接
        3.  打开连接
        4.  创建session
        5.  创建队列
        6.  创建消费者
        7.  接收消息
         */
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER,ActiveMQConnection.DEFAULT_PASSWORD,"tcp://192.168.67.220:61616");
         Connection connection = activeMQConnectionFactory.createConnection();
//        Connection connection =new ConsumerTest().activeMQUtil.getConnection();
        connection.start();
        // 第一个参数表示是否开启事务
        // 第二个参数表示开启事务所对应的处理方式
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//        Session Sessionn = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue atguigu = session.createQueue("test-gmall");
        MessageConsumer consumer = session.createConsumer(atguigu);

        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                // 判断消息的类型
                if (message instanceof TextMessage){
                    String text = null;
                    try {
                        text = ((TextMessage) message).getText();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                    System.out.println(text);
                }
            }
        });
    }
}
