package com.atguigu.gmall0311.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;

@Data
public class SpuSaleAttrValue  implements Serializable{

    @Id
    @Column
    String id ;

    @Column
    String spuId;

    @Column
    String saleAttrId;

    @Column
    String saleAttrValueName;
    // isChecked 锁定当前商品的具体销售属性值是谁！
    @Transient
    String isChecked;

}
