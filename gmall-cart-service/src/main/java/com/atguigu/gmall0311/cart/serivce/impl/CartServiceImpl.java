package com.atguigu.gmall0311.cart.serivce.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0311.bean.CartInfo;
import com.atguigu.gmall0311.bean.SkuInfo;
import com.atguigu.gmall0311.cart.constant.CartConst;
import com.atguigu.gmall0311.cart.mapper.CartInfoMapper;
import com.atguigu.gmall0311.config.RedisUtil;
import com.atguigu.gmall0311.service.CartService;
import com.atguigu.gmall0311.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;

    // 表示登录时添加购物车
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {

        /*
        1.  判断购物车中是否有该商品
            select * from cartInfo where userId = ? and skuId = ?
        2.  有： 数量相加
        3.  没有：直接添加到数据库
        4.  放入redis中
        mysql 与 redis 如何进行同步？
            在添加购物车的时候，直接添加到数据库并添加到redis！
         */
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        cartInfo.setSkuId(skuId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo); // 2

        // 获取jedis 
        Jedis jedis = redisUtil.getJedis();
        // 定义key user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX+userId+ CartConst.USER_CART_KEY_SUFFIX;
        /*
            用那种数据类型 hash jedis.hset(key,field,value)
            key=user:userId:cart
            field=skuId
            value=cartInfoValue
          */
        if (cartInfoExist!=null){
            // 数量相加 skuNum = 1
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum); // 3
            // 给实时价格初始化值
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            // cartInfoExist更新到数据库
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
            // 放入redis
            // jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));
            
        }else {
            // 直接添加到数据库 , 获取skuInfo 信息。添加到cartInfo 中！
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo1 = new CartInfo();

            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);
            // 添加到数据库
            cartInfoMapper.insertSelective(cartInfo1);
            cartInfoExist = cartInfo1;
            // 放入redis
            // jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfo1));
        }

        // 放入redis
        jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));

        // 购物车是否有过期时间？

        // 设置过期时间？跟用户的过期时间一致
        // 获取用户的key
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        // 剩余过期时间
        Long ttl = jedis.ttl(userKey);
        // 赋值给购物车
        jedis.expire(cartKey,ttl.intValue());

        jedis.close();


    }

    @Override
    public List<CartInfo> getCartList(String userId) {

        List<CartInfo> cartInfoList = new ArrayList<>();
        /*
            1.  获取jedis
            2.  从redis 中获取数据
            3.  如果有：将redis 数据返回
            4.  如果没有：从数据库查询{查询购物车中的实时价格}，并放入redis
         */
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义key user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX+userId+ CartConst.USER_CART_KEY_SUFFIX;
        // 获取数据
        //        jedis.hgetAll()
        //        jedis.hvals()
        List<String> cartList = jedis.hvals(cartKey);
        if (cartList!=null && cartList.size()>0){
            for (String cartJson : cartList) {
                //cartJson 转换为对象
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                // 添加购物车数据
                cartInfoList.add(cartInfo);
            }
            // 查询的时候，按照更新的时间倒序！
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    // compareTo str1 = abc str2 =abcd
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        }else {
            // 从数据库中获取数据
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
        // 获取数据库中的数据
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);
        // 合并条件 skuId 相同的时候合并
        for (CartInfo cartInfoCK : cartListCK) {
            // 声明一个boolean 类型遍历
            boolean isMatch = false;
            // 有相同的数据直接更新到数据
            for (CartInfo cartInfoDB : cartInfoListDB) {
                if (cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId())){
                    // 数量相加
                    cartInfoDB.setSkuNum(cartInfoCK.getSkuNum()+cartInfoDB.getSkuNum());
                    // 更新
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    isMatch  = true;
                }
            }
            // 未登录的数据在数据库中没有，则直接插入数据库
            if (!isMatch){
                // 未登录的时候的userId 为null
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }
        // 最后再查询一次更新之后，新添加的所有数据
        List<CartInfo> cartInfoList = loadCartCache(userId);
        // 合并勾选的商品：
        for (CartInfo cartInfoDB : cartInfoList) {
            for (CartInfo cartInfoCK : cartListCK) {
                if (cartInfoDB.getSkuId().equals(cartInfoCK.getSkuId())){
                    // 判断未登录状态为勾选状态！
                    if ("1".equals(cartInfoCK.getIsChecked())){
                        // 将数据库中的商品勾选为1
                        cartInfoDB.setIsChecked("1");
                        // 数量进行覆盖！
                        // cartInfoDB.setSkuNum(cartInfoCK.getSkuNum());
                        // 更新
                        // cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                        // redis 发送消息队列

                        // 自动勾选
                        checkCart(cartInfoDB.getSkuId(),cartInfoCK.getIsChecked(),userId);
                    }
                }
            }
        }

        return cartInfoList;
    }

    @Override
    public void addToCartRedis(String skuId, String userKey, int skuNum) {

        /*
        1.  先获取所有的数据
        2.  判断是否有相同的数据 skuId
        3.  有：数量相加
        4.  无：直接添加redis

        hgetAll ();
        如何获取userKey
         */

        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String cartKey = CartConst.USER_KEY_PREFIX+userKey+ CartConst.USER_CART_KEY_SUFFIX;
        Map<String, String> map = jedis.hgetAll(cartKey);
        //        for (String s : map.keySet()) {
        //            if (s.equals(skuId)){
        //
        //            }
        //        }
        //   有：数量相加
        String cartInfoJson = map.get(skuId);
        if (StringUtils.isNotEmpty(cartInfoJson)){
            // 添加到redis value=cartInfo字符串
            // 从缓存中获取数据 ： value --- cartinfo  --- getSkuNum + skuNum
            CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
            cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
            jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfo));
        }else {
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo1 = new CartInfo();
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setSkuNum(skuNum);
            jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfo1));
        }
        jedis.close();


    }

    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        /*
        1.  将页面传递过来的商品Id ，与购物车中的商品Id 进行匹配
        2.  修改isChecked的数据
        3.  单独创建一个key 来存储已选中的商品
         */
        // 锁定购物车状态
        // 创建jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义 user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX+userId+ CartConst.USER_CART_KEY_SUFFIX;
        // 获取选中的商品
        String cartInfoJson = jedis.hget(cartKey, skuId);
        // cartInfoJson 把转换为对象
        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        // 将cartInfo 写回购物车
        jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfo));

        // 重新记录被勾选的商品 user:userId:checked
        String cartCheckedKey = CartConst.USER_KEY_PREFIX+userId+ CartConst.USER_CHECKED_KEY_SUFFIX;

        if ("1".equals(isChecked)){
            jedis.hset(cartCheckedKey,skuId,JSON.toJSONString(cartInfo));
        }else {
            jedis.hdel(cartCheckedKey,skuId);
        }
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String cartCheckedKey = CartConst.USER_KEY_PREFIX+userId+ CartConst.USER_CHECKED_KEY_SUFFIX;
        List<String> stringList = jedis.hvals(cartCheckedKey);

        for (String cartInfoJson : stringList) {
            cartInfoList.add(JSON.parseObject(cartInfoJson,CartInfo.class));
        }

        jedis.close();
        return cartInfoList;
    }

    /**
     * 根据userId查询数据并放入缓存
     * @param userId
     * @return
     */
    private List<CartInfo> loadCartCache(String userId) {
        /*
        1.  根据userId 查询一下当前商品的实时价格：
            cartInfo.skuPrice = skuInfo.price
        2.  将查询出来的数据集合放入缓存！
         */
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        /* 面试javaScript   =  ==  ===  */
        if (cartInfoList==null || cartInfoList.size()==0){
            return null;
        }
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义key user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX+userId+ CartConst.USER_CART_KEY_SUFFIX;

//        for (CartInfo cartInfo : cartInfoList) {
//            // 每次放一条数据
//            jedis.hset(cartKey,cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
//        }

        HashMap<String, String> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        // 将map 放入缓存
        jedis.hmset(cartKey,map);
        // hgetAll -- map
        return cartInfoList;
    }
}
