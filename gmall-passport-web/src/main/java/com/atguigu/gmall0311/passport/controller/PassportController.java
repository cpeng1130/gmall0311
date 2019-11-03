package com.atguigu.gmall0311.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0311.bean.UserInfo;
import com.atguigu.gmall0311.passport.config.JwtUtil;
import com.atguigu.gmall0311.service.UserInfoService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    private UserInfoService userInfoService;

    @Value("${token.key}")
    public String key;

    //http://localhost:8087/index?originUrl=https%3A%2F%2Fwww.jd.com%2F
    @RequestMapping("index")
    public String index(HttpServletRequest request){
        /*
            用户点击登录的时候，必须从页面的某个链接访问登录模块：则登录url 后面必须有你点击的哪个链接
         */

        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        // 应该存储originUrl
        return "index";
    }

    // 使用对象传值
    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo,HttpServletRequest request){
        // 调用服务层获取方法
        UserInfo info =  userInfoService.login(userInfo);
        if (info!=null){
            // 创建key，map，salt
            // 服务器Ip 地址 在服务器中设置 X-forwarded-for 对应的值
            String salt = request.getHeader("X-forwarded-for");
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId",info.getId());
            map.put("nickName",info.getNickName());
            String token = JwtUtil.encode(key, map, salt);
            // 实际应该返回token
            return token;
        }else {
            return "fail";
        }
    }

    // http://passport.atguigu.com/verify?token=xxx&salt=xxx
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");

        // 解密token
        Map<String, Object> map = JwtUtil.decode(token, key, salt);

        if (map!=null && map.size()>0){
            // 获取用户Id
            String userId = (String) map.get("userId");
            // 调用认证方法
            UserInfo userInfo =  userInfoService.verify(userId);
            if (userInfo!=null){
                return "success";
            }
        }

        return "fail";
    }


}
