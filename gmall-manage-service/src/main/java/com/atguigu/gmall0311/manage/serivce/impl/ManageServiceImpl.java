package com.atguigu.gmall0311.manage.serivce.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0311.bean.*;
import com.atguigu.gmall0311.config.RedisUtil;
import com.atguigu.gmall0311.manage.constant.ManageConst;
import com.atguigu.gmall0311.manage.mapper.*;
import com.atguigu.gmall0311.service.ManageService;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;


import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private  SkuImageMapper skuImageMapper;

    @Autowired
    private  SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    // @Autowired 从容器中获取对象！
    @Autowired
    private RedisUtil redisUtil;


    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        // select * from basecatalog2 wehre catalog1Id=?
//        Example example = new Example(BaseCatalog2.class);
//        example.createCriteria().andEqualTo("catalog1Id",catalog1Id);
//        return baseCatalog2Mapper.selectByExample(example);
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);

    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return    baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
//        select * from baseAttrInfo where catalog3Id = ?
//        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
//        baseAttrInfo.setCatalog3Id(catalog3Id);
//        return baseAttrInfoMapper.select(baseAttrInfo);
//        必须使用xxxMapper.xml 形式来解决！
//        select * from base_attr_info bai INNER JOIN base_attr_value bav
//        ON bai.id = bav.attr_id WHERE bai.catalog3_id = 61

        return baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);
    }

    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        // 保存，还要做修改功能！ baseAttrInfo
        if (baseAttrInfo.getId()!=null && baseAttrInfo.getId().length()>0){
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        }else {
            //        baseAtrrInfo;
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }
//        baseAttrValue;
//        update baseAttrValue set xx=xxx where attrId = ? 不使用该方式进行修改！
//        先将baseAttrValue; 中的数据进行删除！删除条件的！attrId=baseAttrInfo.getId();
//         删除数据：
//        delete from baseAttrValue where attrId = ? baseAttrInfo.getId()
        BaseAttrValue baseAttrValueDel = new BaseAttrValue();
        baseAttrValueDel.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueDel);

        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList!=null && attrValueList.size()>0){
            for (BaseAttrValue baseAttrValue : attrValueList) {
                // id 主键自增
                // valueName 页面传递的数据
                // attrId  baseAttrInfo.getId();
                //                int i,j ;
                //                i=0;
                //                j=2;
                //                i=j/i;
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(baseAttrValue);
            }
        }
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        // select * from baseAttrValue where attrId=?
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        return    baseAttrValueMapper.select(baseAttrValue);
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        // select * from baseAttrInfo where id = attrId;
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        // select * from baseAttrValue where attrId=?baseAttrInfo.getId();
        // 赋值平台属性值集合！
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    @Override
    public List<SpuInfo> getSpuList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<SpuInfo> getSpuList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo) ;
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {

//        spuInfo : 商品表

         spuInfoMapper.insertSelective(spuInfo);
//        spuSaleAttr: 销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        // 循环遍历
        if (spuSaleAttrList!=null && spuSaleAttrList.size()>0){
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                // 设置spuId
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);

                //        spuSaleAttrValue: 销售属性值
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList!=null && spuSaleAttrValueList.size()>0){
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        // 设置spuId
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }

            }
        }
//        spuImage: 商品图片
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList!=null && spuImageList.size()>0){
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }


    }

    @Override
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        List<SpuImage> spuImageList = spuImageMapper.select(spuImage);
        return spuImageList;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        // 调用mapper
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
//        skuInfo: 库存单元表：
        skuInfoMapper.insertSelective(skuInfo);
//        skuImage: sku图片：
        // 一个sku 对应一组图片
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList!=null && skuImageList.size()>0){
            for (SkuImage skuImage : skuImageList) {
//                String id;  主键
//                String skuId;  skuInfo.getId();
//                String imgName;
//                String imgUrl;
//                String spuImgId;
//                String isDefault;
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }
//        skuSaleAttrValue : 销售属性值表 {能够确定当前sku 具体有哪些销售属性值}
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList!=null && skuSaleAttrValueList.size()>0){
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }

//        skuAttrValue : 平台属性值关联表 {能够通过平台属性值对sku进行筛选}
//        集合长度：size();  数组长度：length  字符串长度：length(); 文件长度：length();

        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList!=null && skuAttrValueList.size()>0){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }
    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {

        return getSkuInfoRedisson(skuId);

        // 使用redis--set 命令做分布式锁！
        //return getSkuInfoRedist(skuId);
    }
    private SkuInfo getSkuInfoRedisson(String skuId) {
        // 业务代码
        SkuInfo skuInfo =null;
        RLock lock =null;
        Jedis jedis =null;
        try {
            // 测试redis String
            jedis = redisUtil.getJedis();

            // 定义key
            String userKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            if (jedis.exists(userKey)){
                // 获取缓存中的数据
                String userJson = jedis.get(userKey);
                if (!StringUtils.isEmpty(userJson)){
                    skuInfo = JSON.parseObject(userJson, SkuInfo.class);
                    return skuInfo;
                }
            }else {
                // 创建config
                Config config = new Config();
                // redis://192.168.67.220:6379 配置文件中！
                config.useSingleServer().setAddress("redis://192.168.67.220:6379");

                RedissonClient redisson = Redisson.create(config);

                lock = redisson.getLock("my-lock");

                lock.lock(10, TimeUnit.SECONDS);

                // 从数据库查询数据
                skuInfo = getSkuInfoDB(skuId);
                // 将数据放入缓存
                // jedis.set(userKey,JSON.toJSONString(skuInfo));
                jedis.setex(userKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis!=null){
                jedis.close();
            }
            if (lock!=null){
                lock.unlock();
            }

        }
        // 从db走！
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoRedist(String skuId) {
        SkuInfo skuInfo =null;
        Jedis jedis =null;
        try {
           /*
           1.   获取jedis
           2.   判断缓存中是否有数据
           3.   如果有：则从缓存获取
           4.   没有：走db 要加锁，放入redis
            */

            jedis =   redisUtil.getJedis();
            //  定义key 获取数据
            String userKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            String skuJson = jedis.get(userKey);

            if (skuJson==null){
                System.out.println("缓存没有数据数据");
                // set k1 v1 px 10000 nx
                // 定义锁的Key sku:skuId:lock
                String skuLockKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKULOCK_SUFFIX;
                // 执行命令
                String lockKey  = jedis.set(skuLockKey, "ATGUIGU", "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);
                if ("OK".equals(lockKey)){
                    System.out.println("获取到分布式锁！");
                    skuInfo = getSkuInfoDB(skuId);
                    // 将是数据放入缓存
                    // 将对象转换成字符串
                    String skuRedisStr = JSON.toJSONString(skuInfo);
                    jedis.setex(userKey,ManageConst.SKUKEY_TIMEOUT,skuRedisStr);

                    // 删除掉锁
                    jedis.del(skuLockKey);

                    return skuInfo;
                }else {
                    // 等待
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            }else {
                // 走的缓存！
                 skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
                 return skuInfo;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis!=null){
                jedis.close();
            }
        }
        // 从db走！
        return getSkuInfoDB(skuId);
    }

    // ctrl+alt+m
    private SkuInfo getSkuInfoDB(String skuId) {
        // select * from skuInfo where id = skuId;
        // 通过skuId 将skuImageList 查询出来直接放入skuInfo 对象中！
        SkuInfo skuInfo;
        skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        skuInfo.setSkuImageList(getSkuImageBySkuId(skuId));

        // 通过skuId 获取skuAttrValue 数据
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(skuAttrValueList);

        return   skuInfo;
    }

    @Override
    public List<SkuImage> getSkuImageBySkuId(String skuId) {
        SkuImage skuImage = new SkuImage();
        //sql select * from skuImage where skuId = ?
        skuImage.setSkuId(skuId);
        return   skuImageMapper.select(skuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(),skuInfo.getSpuId());
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        /*
            SELECT * FROM base_attr_info bai INNER JOIN base_attr_value bav
					ON bai.id = bav.attr_id WHERE bav.id in (13,14,80,81,82,83)

            第一种：
            mybatis:
			<select id="selectPostIn" resultType="domain.blog.Post">
             SELECT * FROM base_attr_info bai INNER JOIN base_attr_value bav
					ON bai.id = bav.attr_id WHERE bav.id in
              <foreach item="valueId" index="index" collection="list"
                  open="(" separator="," close=")">
                    #{valueId}
              </foreach>
            </select>
			 bav.id  in #{id}

			 第二种：13,14,80,81,82,83 看作是一个字符串直接传入sql 语句中！
         */
        // 使用工具类
        String valueIds = org.apache.commons.lang3.StringUtils.join(attrValueIdList.toArray(), ",");
        System.out.println("valueIds:"+valueIds); //13,14,80,81,82,83
        return baseAttrInfoMapper.selectAttrInfoListByIds(valueIds);

    }

}
