package com.qingmuy.search.controller;

import com.hmall.common.domain.PageDTO;
import com.qingmuy.api.domain.dto.ItemDTO;
import com.qingmuy.api.domain.query.ItemPageQuery;
import com.qingmuy.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    @Resource
    ISearchService searchService;

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> itemSearch(ItemPageQuery query) throws IOException {
        System.out.println("query = " + query);
        return searchService.searchItem(query);
    }
}
