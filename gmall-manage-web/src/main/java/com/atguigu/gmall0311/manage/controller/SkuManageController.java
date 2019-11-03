package com.atguigu.gmall0311.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0311.bean.SkuInfo;
import com.atguigu.gmall0311.bean.SkuLsInfo;
import com.atguigu.gmall0311.service.ListService;
import com.atguigu.gmall0311.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;

@RestController
@CrossOrigin
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;
    // 前台数据以Json 对象形式传递到后台，后台应该将json 字符串封装成java对象

    @RequestMapping("saveSkuInfo")
    public String saveSkuInfo(@RequestBody SkuInfo skuInfo){
        // 如何防止空指针！
        if (skuInfo!=null){
            manageService.saveSkuInfo(skuInfo);
        }
        return "ok";
    }


    // http://localhost:8082/onSale?skuId=33
    @RequestMapping("onSale")
    public String onSale(String skuId){
        // 创建要保存的数据对象
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        // 通过skuId获取skuInfo 对象
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        // 属性拷贝：
        BeanUtils.copyProperties(skuInfo,skuLsInfo);

//        try {
//            org.apache.commons.beanutils.BeanUtils.copyProperties(skuLsInfo,skuInfo);
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }

        listService.saveSkuLsInfo(skuLsInfo);
        return "ok";
    }
}
