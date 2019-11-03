package com.atguigu.gmall0311.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class ProducerTest {

    // 主函数
    public static void main(String[] args) throws JMSException {
        /*
        1.  创建消息队列工厂
        2.  获取连接
        3.  打开连接
        4.  创建session
        5.  创建队列
        6.  创建提供者
        7.  创建消息对象
        8.  发送消息
        9.  关闭
         */

        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.67.220:61616");
        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();
        // 第一个参数表示是否开启事务
        // 第二个参数表示开启事务所对应的处理方式
//        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

        Queue atguigu = session.createQueue("test-gmall");
        MessageProducer producer = session.createProducer(atguigu);
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("困了,累了,乐乐");

        producer.send(activeMQTextMessage);

        // 必须提交
         session.commit();

        producer.close();
        session.close();
        connection.close();


    }

}
