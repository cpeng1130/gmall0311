package com.atguigu.gmall0311.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0311.bean.*;
import com.atguigu.gmall0311.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class ManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping("index")
    public String index(){
        // 返回一个页面index.html
        return "index";
    }

    @RequestMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
        return manageService.getCatalog1();
    }

    @RequestMapping("getCatalog2")
    // @ResponseBody // 返回json 数据底层需要调用一个jar 包？ jackson.jar ,gson.jar
    public List<BaseCatalog2> getCatalog2(String catalog1Id){
        return manageService.getCatalog2(catalog1Id);
    }

    @RequestMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        return manageService.getCatalog3(catalog2Id);
    }

    @RequestMapping("attrInfoList")
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){
        return manageService.getAttrList(catalog3Id);
    }


    // http://localhost:8082/getAttrValueList?attrId=23
    @RequestMapping("getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId){
        // 调用服务层
        // select * from baseAttrValue where attrId=?
        // return manageService.getAttrValueList(attrId);

        // select * from baseAttrInfo where id = attrId;
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        // baseAttrInfo.getAttrValueList();
        return baseAttrInfo.getAttrValueList();
    }


    // @RequestBody 将json 格式数据转换为java 对象！
    @RequestMapping("saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        // 调用服务层将数据保存到数据库！
        manageService.saveAttrInfo(baseAttrInfo);

    }
    // http://localhost:8082/spuList?catalog3Id=61 属性对象赋值
    @RequestMapping("spuList")
    public List<SpuInfo> spuList(SpuInfo spuInfo){
        return manageService.getSpuList(spuInfo);
    }

}
