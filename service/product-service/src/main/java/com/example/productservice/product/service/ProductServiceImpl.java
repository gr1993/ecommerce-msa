package com.example.productservice.product.service;

import com.example.productservice.category.domain.Category;
import com.example.productservice.category.repository.CategoryRepository;
import com.example.productservice.file.service.FileStorageService;
import com.example.productservice.global.common.dto.PageResponse;
import com.example.productservice.product.domain.*;
import com.example.productservice.product.dto.*;
import com.example.productservice.product.repository.ProductRepository;
import com.example.productservice.product.repository.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;

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

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        log.info("Creating product: {}", request.getProductName());

        // 1. Product 엔티티 생성
        Product product = Product.builder()
                .productName(request.getProductName())
                .productCode(request.getProductCode())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .salePrice(request.getSalePrice())
                .status(request.getStatus())
                .isDisplayed(request.getIsDisplayed())
                .build();

        // 2. 옵션 그룹 및 옵션 값 생성 (임시 ID -> 실제 DB ID 매핑)
        Map<String, ProductOptionValue> optionValueMap = new HashMap<>();

        for (OptionGroupRequest groupRequest : request.getOptionGroups()) {
            ProductOptionGroup optionGroup = ProductOptionGroup.builder()
                    .product(product)
                    .optionGroupName(groupRequest.getOptionGroupName())
                    .displayOrder(groupRequest.getDisplayOrder())
                    .build();
            product.getOptionGroups().add(optionGroup);

            for (OptionValueRequest valueRequest : groupRequest.getOptionValues()) {
                ProductOptionValue optionValue = ProductOptionValue.builder()
                        .optionGroup(optionGroup)
                        .optionValueName(valueRequest.getOptionValueName())
                        .displayOrder(valueRequest.getDisplayOrder())
                        .build();
                optionGroup.getOptionValues().add(optionValue);

                // 프론트 임시 ID를 실제 엔티티에 매핑
                if (valueRequest.getId() != null) {
                    optionValueMap.put(valueRequest.getId(), optionValue);
                }
            }
        }

        // 3. SKU 생성 (optionValueIds를 실제 엔티티로 변환)
        for (SkuRequest skuRequest : request.getSkus()) {
            ProductSku sku = ProductSku.builder()
                    .product(product)
                    .skuCode(skuRequest.getSkuCode())
                    .price(skuRequest.getPrice())
                    .stockQty(skuRequest.getStockQty())
                    .status(skuRequest.getStatus())
                    .build();
            product.getSkus().add(sku);

            // SKU와 옵션 값 연결
            for (String optionValueId : skuRequest.getOptionValueIds()) {
                ProductOptionValue optionValue = optionValueMap.get(optionValueId);
                if (optionValue != null) {
                    ProductSkuOption skuOption = ProductSkuOption.builder()
                            .sku(sku)
                            .optionValue(optionValue)
                            .build();
                    sku.getSkuOptions().add(skuOption);
                    optionValue.getSkuOptions().add(skuOption);
                }
            }
        }

        // 4. 파일 확정 (임시 -> 실제)
        List<Long> fileIds = request.getImages().stream()
                .map(ProductImageRequest::getFileId)
                .filter(fileId -> fileId != null)
                .collect(Collectors.toList());

        Map<Long, String> idUrlMap = new HashMap<>();
        if (!fileIds.isEmpty()) {
            idUrlMap = fileStorageService.confirmFiles(fileIds);
            log.info("Confirmed {} files", fileIds.size());
        }

        // 5. 이미지 생성
        for (ProductImageRequest imageRequest : request.getImages()) {
            String newImageUrl = idUrlMap.get(imageRequest.getFileId());

            ProductImage image = ProductImage.builder()
                    .product(product)
                    .fileId(imageRequest.getFileId())
                    .imageUrl(newImageUrl)
                    .isPrimary(imageRequest.getIsPrimary())
                    .displayOrder(imageRequest.getDisplayOrder())
                    .build();
            product.getImages().add(image);
        }

        // 6. 카테고리 설정
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            product.setCategories(categories);
            log.info("Set {} categories for product", categories.size());
        }

        // 7. 저장
        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully with ID: {}", savedProduct.getProductId());

        return ProductResponse.from(savedProduct);
    }

    @Override
    public ProductDetailResponse getProductDetail(Long productId) {
        log.info("Getting product detail for productId: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. productId: " + productId));

        return ProductDetailResponse.from(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductCreateRequest request) {
        log.info("Updating product: productId={}, productName={}", productId, request.getProductName());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. productId: " + productId));

        // 1. 기본 정보 업데이트
        product.setProductName(request.getProductName());
        product.setProductCode(request.getProductCode());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setSalePrice(request.getSalePrice());
        product.setStatus(request.getStatus());
        product.setIsDisplayed(request.getIsDisplayed());

        // 2. 기존 옵션 그룹, SKU, 이미지 삭제
        product.getOptionGroups().clear();
        product.getSkus().clear();

        List<Long> fidList = request.getImages()
                .stream()
                .map(ProductImageRequest::getFileId)
                .toList();
        if (fidList.isEmpty()) {
            product.getImages().clear();
        } else {
            product.getImages().removeIf(image -> !fidList.contains(image.getFileId()));
        }

        // flush를 호출하여 삭제를 먼저 실행 (SKU 코드 unique constraint 충돌 방지)
        productRepository.flush();

        // 3. 새로운 옵션 그룹 및 옵션 값 생성
        Map<String, ProductOptionValue> optionValueMap = new HashMap<>();

        for (OptionGroupRequest groupRequest : request.getOptionGroups()) {
            ProductOptionGroup optionGroup = ProductOptionGroup.builder()
                    .product(product)
                    .optionGroupName(groupRequest.getOptionGroupName())
                    .displayOrder(groupRequest.getDisplayOrder())
                    .build();
            product.getOptionGroups().add(optionGroup);

            for (OptionValueRequest valueRequest : groupRequest.getOptionValues()) {
                ProductOptionValue optionValue = ProductOptionValue.builder()
                        .optionGroup(optionGroup)
                        .optionValueName(valueRequest.getOptionValueName())
                        .displayOrder(valueRequest.getDisplayOrder())
                        .build();
                optionGroup.getOptionValues().add(optionValue);

                if (valueRequest.getId() != null) {
                    optionValueMap.put(valueRequest.getId(), optionValue);
                }
            }
        }

        // 4. 새로운 SKU 생성
        for (SkuRequest skuRequest : request.getSkus()) {
            ProductSku sku = ProductSku.builder()
                    .product(product)
                    .skuCode(skuRequest.getSkuCode())
                    .price(skuRequest.getPrice())
                    .stockQty(skuRequest.getStockQty())
                    .status(skuRequest.getStatus())
                    .build();
            product.getSkus().add(sku);

            for (String optionValueId : skuRequest.getOptionValueIds()) {
                ProductOptionValue optionValue = optionValueMap.get(optionValueId);
                if (optionValue != null) {
                    ProductSkuOption skuOption = ProductSkuOption.builder()
                            .sku(sku)
                            .optionValue(optionValue)
                            .build();
                    sku.getSkuOptions().add(skuOption);
                    optionValue.getSkuOptions().add(skuOption);
                }
            }
        }

        // 5. 파일 확정
        List<Long> fileIds = request.getImages().stream()
                .map(ProductImageRequest::getFileId)
                .filter(fileId -> fileId != null)
                .collect(Collectors.toList());

        Map<Long, String> idUrlMap = new HashMap<>();
        if (!fileIds.isEmpty()) {
            idUrlMap = fileStorageService.confirmFiles(fileIds);
            log.info("Confirmed {} files", fileIds.size());
        }

        // 6. 새로운 이미지 생성
        for (ProductImageRequest imageRequest : request.getImages()) {
            String newImageUrl = idUrlMap.get(imageRequest.getFileId());
            if (newImageUrl == null) continue;

            ProductImage image = ProductImage.builder()
                    .product(product)
                    .fileId(imageRequest.getFileId())
                    .imageUrl(newImageUrl)
                    .isPrimary(imageRequest.getIsPrimary())
                    .displayOrder(imageRequest.getDisplayOrder())
                    .build();
            product.getImages().add(image);
        }

        // 7. 카테고리 업데이트
        product.getCategories().clear();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            product.setCategories(categories);
            log.info("Updated {} categories for product", categories.size());
        }

        // 8. 저장
        Product savedProduct = productRepository.save(product);

        log.info("Product updated successfully: productId={}", savedProduct.getProductId());

        return ProductResponse.from(savedProduct);
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
