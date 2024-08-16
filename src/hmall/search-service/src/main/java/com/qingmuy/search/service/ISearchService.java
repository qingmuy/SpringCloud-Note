package com.qingmuy.search.service;

import com.hmall.common.domain.PageDTO;
import com.qingmuy.api.domain.dto.ItemDTO;
import com.qingmuy.api.domain.query.ItemPageQuery;

import java.io.IOException;

public interface ISearchService{
    PageDTO<ItemDTO> searchItem(ItemPageQuery query) throws IOException;
}
