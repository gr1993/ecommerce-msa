package com.example.catalogservice.service;

import com.example.catalogservice.domain.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public Page<ProductDocument> searchProducts(Long categoryId, Pageable pageable) {
        NativeQuery query;

        if (categoryId != null) {
            query = NativeQuery.builder()
                    .withQuery(q -> q
                            .term(t -> t
                                    .field("categoryIds")
                                    .value(categoryId)
                            )
                    )
                    .withPageable(pageable)
                    .build();
        } else {
            query = NativeQuery.builder()
                    .withQuery(q -> q.matchAll(m -> m))
                    .withPageable(pageable)
                    .build();
        }

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);
        SearchPage<ProductDocument> searchPage = SearchHitSupport.searchPageFor(searchHits, pageable);

        return (Page<ProductDocument>) SearchHitSupport.unwrapSearchHits(searchPage);
    }
}
