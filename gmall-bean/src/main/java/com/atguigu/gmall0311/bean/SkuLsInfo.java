package com.atguigu.gmall0311.bean;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuLsInfo implements Serializable {

    String id;

    BigDecimal price;

    String skuName;

    String catalog3Id;

    String skuDefaultImg;
    // 热度排名
    Long hotScore=0L;

    // 时间

    // 销量

    List<SkuLsAttrValue> skuAttrValueList;
}
