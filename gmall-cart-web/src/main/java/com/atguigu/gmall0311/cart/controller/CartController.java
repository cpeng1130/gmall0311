package com.atguigu.gmall0311.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0311.bean.CartInfo;
import com.atguigu.gmall0311.bean.SkuInfo;
import com.atguigu.gmall0311.config.CookieUtil;
import com.atguigu.gmall0311.config.LoginRequire;
import com.atguigu.gmall0311.service.CartService;
import com.atguigu.gmall0311.service.ManageService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @Reference
    private ManageService manageService;

    // 记录未登录的userId 给UUID
    private String userKey;

    // 如何判断当前是否登录！获取userId
    // 控制器是谁？ item.html 找！
    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){

        // 应该将对应的商品信息做一个保存
        // 调用服务层将商品数据添加到redis ，mysql
        // 获取userId
        String userId = (String) request.getAttribute("userId");
        // 获取购买的数量，商品Id
        String skuNum = request.getParameter("skuNum");
        String skuId = request.getParameter("skuId");

        if (userId!=null){
            // 登录状态添加购物车
            cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        }else {
            // 未登录添加购物车！放入cookie 中！
             cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));
//            userKey= getUUID(request,response);
//            // 添加
//            // 将其放入cookie
//            Cookie cookie = new Cookie("user-key",userKey);
//            // 将cookie 写给客户端
//            response.addCookie(cookie);
//            cartService.addToCartRedis(skuId,userKey,Integer.parseInt(skuNum));
        }

        // 通过skuId查询skuInfo
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        request.setAttribute("skuNum",skuNum);

        request.setAttribute("skuInfo",skuInfo);

        return "success";
    }
    private String getUUID(HttpServletRequest request,HttpServletResponse response) {
        // 获取cookie中的UUID
        Cookie[] cookies = request.getCookies();
        boolean isMatch = false;
        for (Cookie cks : cookies) {
            if (cks.getName().equals("user-key")){
                userKey = cks.getValue();
                isMatch = true;
            }
        }
        if (!isMatch){
            // 记录未登录的userId 给UUID
            userKey = UUID.randomUUID().toString().replace("-","");

        }
        return userKey;
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response){

        List<CartInfo> cartInfoList = new ArrayList<>();
        // 获取userId
        String userId = (String) request.getAttribute("userId");
        if (userId!=null){
            // 先看未登录购物车中是否有数据
            List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
            if (cartListCK!=null && cartListCK.size()>0){
                // 合并购物车
                cartInfoList = cartService.mergeToCartList(cartListCK,userId);
                // 删除未登录数据
                cartCookieHandler.deleteCartCookie(request,response);
            }else {
                // redis-mysql
                cartInfoList = cartService.getCartList(userId);
            }
        }else {
            // cookie
            cartInfoList = cartCookieHandler.getCartList(request);
        }
        // 保存购物车集合
        request.setAttribute("cartInfoList",cartInfoList);
        return "cartList";
    }

    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        // 从页面获取传递过来的数据
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");
        String userId = (String) request.getAttribute("userId");

        if (userId!=null){
            // 登录时选中 redis
            cartService.checkCart(skuId,isChecked,userId);
        }else {
            // 未登录时选中 cookie
            cartCookieHandler.checkCart(request,response,skuId,isChecked);

        }
    }

    // 点击去结算重定向到订单页面
    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        // 获取userId
        String userId = (String) request.getAttribute("userId");
        // 合并购物车中勾选的商品 cookie -- redis 合并
        // 获取未登录的数据
        List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
        if (cartListCK!=null && cartListCK.size()>0){
            // 合并勾选的商品
            cartService.mergeToCartList(cartListCK,userId);
            // 删除未登录的数据
            cartCookieHandler.deleteCartCookie(request,response);
        }
        return "redirect://trade.gmall.com/trade";
    }
}
