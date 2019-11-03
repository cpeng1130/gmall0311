package com.atguigu.gmall0311.user.controller;

import com.atguigu.gmall0311.bean.UserInfo;
import com.atguigu.gmall0311.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class UserController {

    @Autowired
    private UserInfoService userInfoService;

    @RequestMapping("findAll")
    @ResponseBody
    public List<UserInfo> findAll(){
        return userInfoService.findAll();
    }
}
