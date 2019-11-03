package com.atguigu.gmall0311.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0311.bean.BaseSaleAttr;
import com.atguigu.gmall0311.bean.SpuImage;
import com.atguigu.gmall0311.bean.SpuInfo;
import com.atguigu.gmall0311.bean.SpuSaleAttr;
import com.atguigu.gmall0311.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class SpuManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping("baseSaleAttrList")
    public List<BaseSaleAttr> baseSaleAttrList(){
       return manageService.getBaseSaleAttrList();
    }

    @RequestMapping("saveSpuInfo")
    public String saveSpuInfo(@RequestBody SpuInfo spuInfo){
        // 保存数据！
        manageService.saveSpuInfo(spuInfo);
        return "success";
    }
    // http://localhost:8082/spuImageList?spuId=60
    // springmvc 对象传值的方式接收数据
    @RequestMapping("spuImageList")
    public List<SpuImage> spuImageList(SpuImage spuImage){
        return manageService.getSpuImageList(spuImage);
    }

    // http://localhost:8082/spuSaleAttrList?spuId=60
    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> spuSaleAttrList(String spuId){

        return manageService.getSpuSaleAttrList(spuId);
    }


}
