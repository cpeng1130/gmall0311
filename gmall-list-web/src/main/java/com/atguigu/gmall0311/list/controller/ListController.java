package com.atguigu.gmall0311.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0311.bean.*;
import com.atguigu.gmall0311.service.ListService;
import com.atguigu.gmall0311.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    // http://list.gmall.com/list.html?catalog3Id=61
    @RequestMapping("list.html")
    public String getSearch(SkuLsParams skuLsParams, HttpServletRequest request){


        // 每页显示两条数据：
        skuLsParams.setPageSize(2);

        SkuLsResult skuLsResult = listService.search(skuLsParams);

        System.out.println(JSON.toJSONString(skuLsResult));
        //return JSON.toJSONString(skuLsResult);
        // 获取skuLsInfo 集合，并将其显示到页面
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        List<BaseAttrInfo> baseAttrInfoList = null;
        // 获取平台属性，平台属性值数据
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        /*分开走：三级分类Id*/
        //        if (skuLsParams.getCatalog3Id()!=null){
        //            baseAttrInfoList=  manageService.getAttrList(skuLsParams.getCatalog3Id());
        //        }else {
        //            // 通过平台属性值Id 查询平台属性，平台属性值 13，14，80，81，82，83
        //            baseAttrInfoList = manageService.getAttrList(attrValueIdList);
        //        }
        /*都通过平台属性值Id检索*/
        baseAttrInfoList = manageService.getAttrList(attrValueIdList);

        // 编写一个方法记录当前的查询条件：
        String urlParam = makeUrlParam(skuLsParams);

        // 声明一个面包屑集合
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();

        // 集合在遍历的过程中要删除对应的数据！
        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            // 获取平台属性对象
            BaseAttrInfo baseAttrInfo = iterator.next();
            // 获取平台属性值集合
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

            // 获取到集合中平台属性值Id
            for (BaseAttrValue baseAttrValue : attrValueList) {
                // 获取url上的valueId 在skuLsParams 对象中
                if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
                    for (String valueId : skuLsParams.getValueId()) {
                        if (valueId.equals(baseAttrValue.getId())){
                            // 删除平台属性对象
                            iterator.remove();

                            // 构成面包屑：baseAttrValueed 的名称就是面包屑
                            BaseAttrValue baseAttrValueed = new BaseAttrValue();
                            baseAttrValueed.setValueName(baseAttrInfo.getAttrName() + ":" +baseAttrValue.getValueName());
                            // 重新制作urlParam 参数
                            // http://list.gmall.com/list.html?keyword=小米&valueId=83&valueId=82
                            // http://list.gmall.com/list.html?keyword=小米&valueId=83
                            String newUrlParam = makeUrlParam(skuLsParams,valueId);
                            // 保存点击面包屑之后的url参数
                            baseAttrValueed.setUrlParam(newUrlParam);
                            // 将面包屑添加到集合
                            baseAttrValueArrayList.add(baseAttrValueed);
                        }
                    }
                }
            }
        }


        // 保存检索关键字
        request.setAttribute("keyword",skuLsParams.getKeyword());

        // 保存面包屑
        request.setAttribute("baseAttrValueArrayList",baseAttrValueArrayList);
        // 保存原来的参数
        request.setAttribute("urlParam",urlParam);
        // 保存数据
        request.setAttribute("skuLsInfoList",skuLsInfoList);

        request.setAttribute("baseAttrInfoList",baseAttrInfoList);


        // 作用域中将分页信息保存：
        request.setAttribute("totalPage",skuLsResult.getTotalPages());
        request.setAttribute("pageNo",skuLsParams.getPageNo());

        return "list";

    }

    // 编写记录查询条件的方法
    /**
     *
     * @param skuLsParams 用户查询的参数实体类
     * @param excludeValueIds 点击的面包屑中隐藏的平台属性值Id
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {

        String urlParam = "";
        //  href="list.html?keyword=?&valueId=?"
        // 判断keyword  urlParam = keyword=skuLsParams.getKeyword()
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            urlParam+="keyword="+skuLsParams.getKeyword();
        }
        // href="list.html?keyword=?&valueId=?&catalog3Id=xx" 自己拼接
        // href="list.html?catalog3Id=xx" 页面选择
        // 三级分类Id  keyword=skuLsParams.getKeyword()&
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            if (urlParam.length()>0){
                urlParam+="&";
            }
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }

        // href="list.html?keyword=?&valueId=?"
        // 平台属性值Id
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            for (String valueId : skuLsParams.getValueId()) {

                if (excludeValueIds!=null && excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if (excludeValueId.equals(valueId)){
                        // return continue break;
                      continue;
                    }
                }
                if (urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
            }
        }
        System.out.println(urlParam);
        // 返回urlParam
        return urlParam;

    }
}
