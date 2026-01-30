package com.example.catalogservice.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.catalogservice.controller.dto.ProductSearchRequest;
import com.example.catalogservice.domain.document.ProductDocument;
import com.example.catalogservice.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
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
    private final ProductSearchRepository productSearchRepository;

    public ProductDocument findProductById(String productId) {
        return productSearchRepository.findById(productId).orElse(null);
    }

    public List<String> autocompleteProductName(String keyword) {
        if (!hasText(keyword)) {
            return List.of();
        }

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q
                        .match(m -> m
                                .field("productName.autocomplete")
                                .query(keyword)
                        )
                )
                .withSourceFilter(new org.springframework.data.elasticsearch.core.query.FetchSourceFilter(
                        true, new String[]{"productName"}, null))
                .withMaxResults(5)
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        return searchHits.getSearchHits().stream()
                .map(hit -> hit.getContent().getProductName())
                .distinct()
                .limit(5)
                .toList();
    }

    public Page<ProductDocument> searchProducts(ProductSearchRequest request) {
        Query query = buildQuery(request);
        boolean hasKeyword = hasText(request.getProductName());

        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of(request.getPage(), request.getSize()));

        // 키워드 검색이 있으면 _score 우선 정렬, 없으면 요청된 정렬만 적용
        if (hasKeyword) {
            // _score 우선 정렬
            queryBuilder.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        }

        // 요청된 정렬 또는 기본 정렬 적용
        applySort(queryBuilder, request);

        NativeQuery nativeQuery = queryBuilder.build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);
        SearchPage<ProductDocument> searchPage = SearchHitSupport.searchPageFor(
                searchHits, PageRequest.of(request.getPage(), request.getSize()));

        return (Page<ProductDocument>) SearchHitSupport.unwrapSearchHits(searchPage);
    }

    private void applySort(NativeQueryBuilder queryBuilder, ProductSearchRequest request) {
        String sort = request.getSort();

        if (hasText(sort)) {
            String[] parts = sort.split(",");
            String field = parts[0];
            SortOrder order = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                    ? SortOrder.Desc
                    : SortOrder.Asc;
            queryBuilder.withSort(s -> s.field(f -> f.field(field).order(order)));
        } else {
            // 기본 정렬: createdAt DESC
            queryBuilder.withSort(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc)));
        }
    }

    private Query buildQuery(ProductSearchRequest request) {
        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();

        // 상품명 + 검색 키워드 검색 (must - 점수에 영향, productName boost 2배)
        if (hasText(request.getProductName())) {
            mustQueries.add(Query.of(q -> q
                    .multiMatch(mm -> mm
                            .query(request.getProductName())
                            .fields("productName^2", "searchKeywords")
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
