package com.atguigu.gmall0311.service;

import com.atguigu.gmall0311.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {

    // 保存交易记录
    void savePaymentInfo(PaymentInfo paymentInfo);

    // 查询交易记录
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);

    // 更新交易记录
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUPD);
    // 退款接口
    boolean refund(String orderId);
    // 支付
    Map createNative(String orderId, String totalAmount);

    /**
     * 发送消息给订单
     * @param paymentInfo orderId
     * @param result
     */
    void sendPaymentResult(PaymentInfo paymentInfo,String result);

    /**
     * out_trade_no 查询
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    /**
     * 发送消息查询是否支付成功
     * @param outTradeNo
     * @param delaySec
     * @param checkCount
     */
    void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);

    /**
     * 根据orderId 关闭交易记录
     * @param orderId
     */
    void closePayment(String orderId);
}
