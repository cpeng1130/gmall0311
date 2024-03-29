package com.atguigu.gmall0311.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.jboss.netty.util.internal.ReusableIterator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

import javax.jms.Session;

@Configuration
public class ActiveMQConfig {

    @Value("${spring.activemq.broker-url:disabled}")
    String brokerURL ;

    @Value("${activemq.listener.enable:disabled}")
    String listenerEnable;

    // 获取ActiveMQUtil
    @Bean
    public ActiveMQUtil getActiveMQUtil(){
        if ("disabled".equals(brokerURL)){
            return null;
        }
        ActiveMQUtil activeMQUtil = new ActiveMQUtil();
        activeMQUtil.init(brokerURL);
        return  activeMQUtil;
    }

    // 创建一个消息监听器对象
    /*
     <bean id="activeMQConnectionFactory" class="org.apache.activemq.ActiveMQConnectionFactory">
        <property name="brokerURL" value="tcp://192.168.67.220:61616">
     </bean>
    <bean  name="jmsQueueListener" class="org.springframework.jms.config.DefaultJmsListenerContainerFactory">
        <property name="activeMQConnectionFactory" ref="activeMQConnectionFactory">
    </bean>
     */
    @Bean(name = "jmsQueueListener")
    public DefaultJmsListenerContainerFactory jmsQueueListenerContainerFactory(ActiveMQConnectionFactory activeMQConnectionFactory) {
        if("disabled".equals(listenerEnable)){
            return null;
        }
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(activeMQConnectionFactory);
        // 设置事务
        factory.setSessionTransacted(false);
        // 自动签收
        factory.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
        // 设置并发数
        factory.setConcurrency("5");
        // 重连间隔时间
        factory.setRecoveryInterval(5000L);

        return factory;
    }
    // 接收消息
    /*
      <bean id="activeMQConnectionFactory" class="org.apache.activemq.ActiveMQConnectionFactory">
        <property name="brokerURL" value="tcp://192.168.67.220:61616">
     </bean>
     */
    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory ( ){
        ActiveMQConnectionFactory activeMQConnectionFactory =
                new ActiveMQConnectionFactory(brokerURL);
        return activeMQConnectionFactory;
    }
}
