package com.atguigu.gmall0311.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;

@Data
public class BaseAttrValue implements Serializable {

    @Id
    @Column
    private String id;
    @Column
    private String valueName;
    @Column
    private String attrId;

    //    http://list.gmall.com/list.html?keyword=小米&valueId=83&valueId=82 没点之前
    //    http://list.gmall.com/list.html?keyword=小米&valueId=83 点击之后 urlParam
    // urlParam 用来记录当用户点击面包屑以后的url参数
    @Transient
    private String urlParam;


}
