package com.atguigu.gmall0311.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall0311.bean.SkuLsInfo;
import com.atguigu.gmall0311.bean.SkuLsParams;
import com.atguigu.gmall0311.bean.SkuLsResult;
import com.atguigu.gmall0311.config.RedisUtil;
import com.atguigu.gmall0311.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    // 调用es客户端
    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;


    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";

    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {
        // put /index/type/id {}
        /*
         1.  明确保存对象skuLsInfo
         2.  创建动作
         3.  执行动作
         */
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        /*
        1.  定义dsl 语句
        2.  定义执行的动作
        3.  执行动作
        4.  获取返回结果
         */
        // 动态的生成的
        String query= makeQueryStringForSearch(skuLsParams);

        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult =null;

        try {
            searchResult = jestClient.execute(search);

        } catch (IOException e) {
            e.printStackTrace();
        }
        // 第一个参数查询的结果集，第二个参数用户输入参数查询的实体类：可以设置每页显示的数据条数
        SkuLsResult skuLsResult = makeResultForSearch(searchResult,skuLsParams);

        return skuLsResult;
    }

    @Override
    public void updHotScore(String skuId) {
        /*
            1.  更新次数放入redis
            2.  当达到一定值，量时，更新es
         */
        Jedis jedis = redisUtil.getJedis();
        // 使用哪种数据类型以及key
        String hotKey="hotScore";
        Double hotScore = jedis.zincrby(hotKey, 1, "skuId:" + skuId);
        // 符合规则的时候，将数据放入es
        if (hotScore%10==0){
            //  updateHotScore(skuId,  Math.round(hotScore));
            // Math.round(11.5) = 12 Math.round(-11.5) = -11
            updateHotScore(skuId,  Math.round(hotScore));
        }


    }

    /**
     * 更新es
     * @param skuId
     * @param hotScore
     */
    private void updateHotScore(String skuId, long hotScore) {
        /*
        1.  定义dsl 语句
        2.  定义执行的动作
        3.  执行动作
         */
        String updEs="{\n" +
                "  \"doc\": {\n" +
                "\t\"hotScore\": "+hotScore+"\n" +
                "  }\n" +
                "}\n";

        Update update = new Update.Builder(updEs).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     *  获取返回的结果集！
     * @param searchResult
     * @param skuLsParams
     * @return
     */
    private SkuLsResult makeResultForSearch(SearchResult searchResult, SkuLsParams skuLsParams) {
        SkuLsResult skuLsResult = new SkuLsResult();

//        List<SkuLsInfo> skuLsInfoList;
        ArrayList<SkuLsInfo> skuLsInfoArrayList = new ArrayList<>();
        //      skuLsInfoArrayList 集合数据应该从es 中查询得到！
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            // 获取skuLsInfo
            SkuLsInfo skuLsInfo = hit.source;
            // skuLsInfo 中的skuName 并不是高亮，所以应该获取highlight 高亮部分！
            if (hit.highlight!=null && hit.highlight.size()>0){
                // 获取高亮字段
                List<String> list = hit.highlight.get("skuName");
                String skuNameHI = list.get(0);
                // 将原来的skuLsInfo 中的skuName 替换掉
                skuLsInfo.setSkuName(skuNameHI);
            }
            // skuLsInfo 添加到集合
            skuLsInfoArrayList.add(skuLsInfo);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoArrayList);
//        long total;
        skuLsResult.setTotal(searchResult.getTotal());
//        long totalPages;
//        10    3     4 | 9 3 3
//        long tp= searchResult.getTotal()%skuLsParams.getPageSize()==0?searchResult.getTotal()/skuLsParams.getPageSize():searchResult.getTotal()/skuLsParams.getPageSize()+1;

        long totalPages = (searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPages);
//        List<String> attrValueIdList;
        // 声明一个集合来存储平台属性值Id
        ArrayList<String> arrayList = new ArrayList<>();
        // 获取平台属性值Id 在聚合中获取！
        // TermsAggregation 获取 平台属性值Id
        TermsAggregation groupby_attr = searchResult.getAggregations().getTermsAggregation("groupby_attr");

        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        for (TermsAggregation.Entry bucket : buckets) {
            String valueId = bucket.getKey();
            // 将valueId 放入集合中
            arrayList.add(valueId);
        }

        skuLsResult.setAttrValueIdList(arrayList);
        return skuLsResult;
    }

    // 动态生成dsl 语句方法
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        // 创建查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // skuLsParams 用户输入的条件

        // 判断keyword = skuName
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",skuLsParams.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);

            // 设置高亮
            // 获取高亮对象
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();
            // 设置高亮属性
            highlighter.postTags("</span>");
            highlighter.preTags("<span style=color:red>");
            highlighter.field("skuName");

            // 将设置好的高亮对象放入查询器
            searchSourceBuilder.highlight(highlighter);
        }
        // 平台属性值Id
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            // 循环遍历
            for (String valueId : skuLsParams.getValueId()) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId);
                // filter -- term
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }


        // 判断三级分类Id
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            // term
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",skuLsParams.getCatalog3Id());
            // filter -- term
            boolQueryBuilder.filter(termQueryBuilder);
        }

        // query
        searchSourceBuilder.query(boolQueryBuilder);

        // 设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        // 设置分页
        // 从第几条数据开始显示
        // select * from skuInfo limit (pageNo-1)*pageSize ,pageSize
        // 3  , 2  |  0,2  | 2, 2

        int from = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        // 设置每页显示的条数
        searchSourceBuilder.size(skuLsParams.getPageSize());

        // 设置聚合
        //        TermsAggregation termsAggregation = new TermsAggregation("skuAttrValueList.valueId",);
        //        searchSourceBuilder.aggregation();

        // 将term 封装到agg ，按照skuAttrValueList.valueId 进行聚合

        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");

        searchSourceBuilder.aggregation(groupby_attr);

        String query = searchSourceBuilder.toString();
        // 动态生成的dsl 语句！
        System.out.println("query:"+query);

        return query;

    }
}
