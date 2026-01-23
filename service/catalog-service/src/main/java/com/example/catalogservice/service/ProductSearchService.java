package com.example.catalogservice.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.catalogservice.controller.dto.ProductSearchRequest;
import com.example.catalogservice.domain.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final CategorySyncService categorySyncService;

    public Page<ProductDocument> searchProducts(ProductSearchRequest request) {
        Query query = buildQuery(request);

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withPageable(request.toPageable())
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);
        SearchPage<ProductDocument> searchPage = SearchHitSupport.searchPageFor(searchHits, request.toPageable());

        return (Page<ProductDocument>) SearchHitSupport.unwrapSearchHits(searchPage);
    }

    private Query buildQuery(ProductSearchRequest request) {
        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();

        // 상품명 검색 (must - 점수에 영향)
        if (hasText(request.getProductName())) {
            mustQueries.add(Query.of(q -> q
                    .match(m -> m
                            .field("productName")
                            .query(request.getProductName())
                    )
            ));
        }

        // 카테고리 필터 (filter - 점수에 영향 없음, 캐싱 가능)
        // 상위 카테고리 선택 시 하위 카테고리도 함께 검색
        if (request.getCategoryId() != null) {
            List<Long> categoryIds = categorySyncService.getCategoryIdWithDescendants(request.getCategoryId());
            filterQueries.add(Query.of(q -> q
                    .terms(t -> t
                            .field("categoryIds")
                            .terms(tv -> tv.value(categoryIds.stream()
                                    .map(id -> co.elastic.clients.elasticsearch._types.FieldValue.of(id))
                                    .toList()))
                    )
            ));
        }

        // 상태 필터
        if (hasText(request.getStatus())) {
            filterQueries.add(Query.of(q -> q
                    .term(t -> t
                            .field("status")
                            .value(request.getStatus())
                    )
            ));
        }

        // 가격 범위 필터
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            filterQueries.add(Query.of(q -> q
                    .range(r -> r
                            .number(n -> {
                                n.field("salePrice");
                                if (request.getMinPrice() != null) {
                                    n.gte((double) request.getMinPrice());
                                }
                                if (request.getMaxPrice() != null) {
                                    n.lte((double) request.getMaxPrice());
                                }
                                return n;
                            })
                    )
            ));
        }

        // 조건이 없으면 전체 조회
        if (mustQueries.isEmpty() && filterQueries.isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        // BoolQuery 구성
        return Query.of(q -> q
                .bool(BoolQuery.of(b -> {
                    if (!mustQueries.isEmpty()) {
                        b.must(mustQueries);
                    }
                    if (!filterQueries.isEmpty()) {
                        b.filter(filterQueries);
                    }
                    return b;
                }))
        );
    }

    private boolean hasText(String str) {
        return str != null && !str.isBlank();
    }
}
