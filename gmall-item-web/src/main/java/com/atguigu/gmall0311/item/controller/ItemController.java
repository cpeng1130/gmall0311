package com.atguigu.gmall0311.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0311.bean.SkuInfo;
import com.atguigu.gmall0311.bean.SkuSaleAttrValue;
import com.atguigu.gmall0311.bean.SpuSaleAttr;
import com.atguigu.gmall0311.config.LoginRequire;
import com.atguigu.gmall0311.service.ListService;
import com.atguigu.gmall0311.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    // https://item.jd.com/100000177770.html
    @RequestMapping("{skuId}.html")
//    @LoginRequire(autoRedirect = true) // 表示访问商品详情的时候，必须要登录！
    public String getItem(@PathVariable String skuId , HttpServletRequest request){
        System.out.println(skuId);

        // 调用服务层
        // select * from skuInfo where id = skuId;
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        //        skuImageList 直接放入到skuInfo 中
        //        List<SkuImage> skuImageList = manageService.getSkuImageBySkuId(skuId);
        //
        //        request.setAttribute("skuImageList",skuImageList);
        // 获取销售属性结果集：
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);


        // 获取销售属性值组成的skuId 集合
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        // 118|120 = 33 119|121=34 118|122=35 组成json 字符串！
        // map.put("118|120",33 ) 然后将转换为json字符串即可！
        String key = "";
        HashMap<String, String> map = new HashMap<>();
        // 拼接规则：skuId 与 下一个skuId 不相等的时候，不拼接！ 当拼接到集合末尾则不拼接
        for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);

            // 第一次拼接： key=118
            // 第二次拼接： key=118|
            // 第三次拼接： key=118|120
            // 第四次拼接： key=""
            // 什么时候拼接|
            if (key.length()>0){
                key+="|";
            }
            key+=skuSaleAttrValue.getSaleAttrValueId();

            if ( (i+1)==skuSaleAttrValueList.size()||!skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i+1).getSkuId())){
                // 放入map 中！
                    map.put(key,skuSaleAttrValue.getSkuId());
                // 清空key
                    key="";
            }
        }

        // 将map 转换为json 字符串
        String valuesSkuJson = JSON.toJSONString(map);
        request.setAttribute("valuesSkuJson",valuesSkuJson );


        request.setAttribute("spuSaleAttrList",spuSaleAttrList);

        // 保存skuInfo 对象数据
        request.setAttribute("skuInfo",skuInfo);

        // 调用热度排名
        listService.updHotScore(skuId);
        return "item";
    }
}
