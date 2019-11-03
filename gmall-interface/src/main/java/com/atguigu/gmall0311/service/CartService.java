package com.atguigu.gmall0311.service;

import com.atguigu.gmall0311.bean.CartInfo;

import java.util.List;

public interface CartService {

    // 返回值，参数列表

    /**
     *
     * @param skuId 商品Id
     * @param userId 用户Id
     * @param skuNum 商品 数量
     */
    void  addToCart(String skuId,String userId,Integer skuNum);

    /**
     *
     * 根据userId 查询购物车数据
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     * @param cartListCK
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);

    /**
     *
     * @param skuId
     * @param userKey
     * @param i
     */
    void addToCartRedis(String skuId, String userKey, int i);

    /**
     * 选中商品
     * @param skuId
     * @param isChecked
     * @param userId
     */
    void checkCart(String skuId, String isChecked, String userId);

    /**
     *
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);
}
