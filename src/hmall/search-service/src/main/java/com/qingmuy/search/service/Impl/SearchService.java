package com.qingmuy.search.service.Impl;

import cn.hutool.json.JSONUtil;
import com.hmall.common.domain.PageDTO;
import com.qingmuy.api.domain.dto.ItemDTO;
import com.qingmuy.api.domain.query.ItemPageQuery;
import com.qingmuy.search.service.ISearchService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;


@Service
public class SearchService implements ISearchService {

    @Resource
    RestHighLevelClient client;

    @Override
    public PageDTO<ItemDTO> searchItem(ItemPageQuery query) throws IOException {
        // 1. 准备查询条件
        SearchRequest request = new SearchRequest("items");
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (query.getKey() != null && !query.getKey().isEmpty()) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        if (query.getCategory() != null) {
            boolQueryBuilder.filter(QueryBuilders.matchQuery("category", query.getCategory()));
        }
        if (query.getBrand() != null) {
            boolQueryBuilder.should(QueryBuilders.matchQuery("brand", query.getBrand()));
        }
        if (query.getMinPrice() != null) {
            boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()));
        }
        if (query.getMaxPrice() != null){
            boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()));
        }

        // 分页设置
        request.source().from((query.getPageNo() - 1) * query.getPageSize()).size(query.getPageSize());

        // 排序设置
        if (query.getSortBy() != null && !query.getSortBy().isEmpty()) {
            System.out.println("SortBy:" + query.getSortBy());
            if (query.getIsAsc()) {
                request.source().sort(query.getSortBy(), SortOrder.ASC);
            }else{
                request.source().sort(query.getSortBy(), SortOrder.DESC);
            }
        }

        /*// 高亮显示
        request.source().highlighter(
                SearchSourceBuilder.highlight()
                        .field("key")
                        .preTags("<em>")
                        .postTags("</em>")
        );*/


        request.source().query(boolQueryBuilder);

        // 2. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // 3. 解析响应结果
        PageDTO<ItemDTO> itemDTOPageDTO = new PageDTO<>();
        ArrayList<ItemDTO> itemDTOS = new ArrayList<>();
        // 3.1 解析结果
        SearchHits searchHits = response.getHits();
        // 3.2 查询总量
        itemDTOPageDTO.setTotal(searchHits.getTotalHits().value);
        // 3.3 查询的结果数组
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            ItemDTO itemDTO = JSONUtil.toBean(hit.getSourceAsString(), ItemDTO.class);
            itemDTOS.add(itemDTO);
        }
        itemDTOPageDTO.setList(itemDTOS);

        // 4. 返回结果
        return itemDTOPageDTO;
    }
}
