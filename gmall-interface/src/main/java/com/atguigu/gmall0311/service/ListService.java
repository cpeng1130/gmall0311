package com.atguigu.gmall0311.service;

import com.atguigu.gmall0311.bean.SkuLsInfo;
import com.atguigu.gmall0311.bean.SkuLsParams;
import com.atguigu.gmall0311.bean.SkuLsResult;

public interface ListService {

    /**
     * 商品上架
     * @param skuLsInfo
     */
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    /**
     * 检索接口
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 更新商品的热度
     * @param skuId
     */
    void updHotScore(String skuId);


}
