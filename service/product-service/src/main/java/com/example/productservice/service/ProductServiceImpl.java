package com.example.productservice.service;

import com.example.productservice.domain.entity.Product;
import com.example.productservice.domain.repository.ProductRepository;
import com.example.productservice.domain.repository.ProductSpecification;
import com.example.productservice.dto.PageResponse;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.dto.ProductSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public PageResponse<ProductResponse> searchProducts(ProductSearchRequest request) {
        log.info("Searching products with request: {}", request);

        // 페이지네이션 설정
        Pageable pageable = createPageable(request);

        // 검색 조건으로 상품 조회
        Page<Product> productPage = productRepository.findAll(
                ProductSpecification.searchWith(request),
                pageable
        );

        // Product -> ProductResponse 변환
        Page<ProductResponse> responsePage = productPage.map(ProductResponse::from);

        log.info("Found {} products", responsePage.getTotalElements());

        return PageResponse.from(responsePage);
    }

    private Pageable createPageable(ProductSearchRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 10;

        // 정렬 처리
        if (request.getSort() != null && !request.getSort().trim().isEmpty()) {
            String[] sortParams = request.getSort().split(",");
            if (sortParams.length == 2) {
                String property = sortParams[0].trim();
                String direction = sortParams[1].trim();
                Sort sort = direction.equalsIgnoreCase("desc")
                        ? Sort.by(property).descending()
                        : Sort.by(property).ascending();
                return PageRequest.of(page, size, sort);
            }
        }

        // 기본 정렬: 생성일 내림차순
        return PageRequest.of(page, size, Sort.by("createdAt").descending());
    }
}
