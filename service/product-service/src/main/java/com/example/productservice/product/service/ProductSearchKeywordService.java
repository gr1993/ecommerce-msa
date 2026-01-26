package com.example.productservice.product.service;

import com.example.productservice.product.dto.SearchKeywordRequest;
import com.example.productservice.product.dto.SearchKeywordResponse;

import java.util.List;

public interface ProductSearchKeywordService {

    List<SearchKeywordResponse> getKeywordsByProductId(Long productId);

    SearchKeywordResponse addKeyword(Long productId, SearchKeywordRequest request);

    void deleteKeyword(Long productId, Long keywordId);
}
