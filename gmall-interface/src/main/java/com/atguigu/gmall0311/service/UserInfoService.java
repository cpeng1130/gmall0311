package com.atguigu.gmall0311.service;


import com.atguigu.gmall0311.bean.UserAddress;
import com.atguigu.gmall0311.bean.UserInfo;

import java.util.List;

// 业务逻辑层
public interface UserInfoService {

    /**
     * 查询所有用户数据
     * @return
     */
    List<UserInfo> findAll();

    /**
     *
     * @param name
     * @return
     */
    UserInfo getUserInfoByName(String name);

    /**
     *
     * @param userInfo
     * @return
     */
    List<UserInfo> getUserInfoListByName(UserInfo userInfo);
    /**
     *
     * @param userInfo
     * @return
     */
    List<UserInfo> getUserInfoListByNickName(UserInfo userInfo);
    // int ,boolean, void

    /**
     * 添加用户信息
     * @param userInfo
     */
    void addUser(UserInfo userInfo);

    /**
     *
     * @param userInfo
     */
    void updUser(UserInfo userInfo);

    /**
     *
     * @param userInfo
     */
    void delUser(UserInfo userInfo);

    // 根据用户Id 查询用户的收货地址列表！

    /**
     *
     * @param userId
     * @return
     */
    List<UserAddress> getUserAddressByUserId(String userId);

    /**
     *
     * @param userAddress
     * @return
     */
    List<UserAddress> getUserAddressByUserId(UserAddress userAddress);

    /**
     * 登录方法
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     *
     * @param userId
     * @return
     */
    UserInfo verify(String userId);
}
