package com.atguigu.gmall0311.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

@Data
public class BaseAttrInfo implements Serializable{
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 表示获取主键自增！
    private String id;
    @Column
    private String attrName;
    @Column
    private String catalog3Id;

    // 有一个平台属性值的集合！
    @Transient  // @Transient 表示非数据库中的字段，但是业务逻辑需要使用的字段 !
    private List<BaseAttrValue> attrValueList;


}
