package com.example.productservice.product.service;

import com.example.productservice.file.domain.FileUpload;
import com.example.productservice.file.repository.FileUploadRepository;
import com.example.productservice.product.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class SampleDataService {

    private final ProductService productService;
    private final FileUploadRepository fileUploadRepository;
    private final SampleDataService self;

    public SampleDataService(ProductService productService,
                             FileUploadRepository fileUploadRepository,
                             @Lazy SampleDataService self) {
        this.productService = productService;
        this.fileUploadRepository = fileUploadRepository;
        this.self = self;
    }

    @Value("${file.upload.temp-dir:uploads/temp}")
    private String tempDir;

    @Value("${file.upload.base-url:/files}")
    private String baseUrl;

    public List<ProductResponse> createSampleProducts() {
        log.info("Starting sample product creation...");

        // 샘플 상품 15개 생성 (각각 별도 트랜잭션에서 실행됨)
        List<ProductResponse> createdProducts = new ArrayList<>();
        List<SampleProductData> sampleProducts = getSampleProductDataList();

        for (SampleProductData data : sampleProducts) {
            try {
                // 각 상품마다 새로운 이미지 파일 준비 (별도 트랜잭션 - self를 통해 프록시 호출)
                Map<String, Long> productImageFileIdMap = self.prepareImageFilesForProduct(data.imageFiles);
                ProductCreateRequest request = buildProductRequest(data, productImageFileIdMap);
                ProductResponse response = productService.createProduct(request);
                createdProducts.add(response);
                log.info("Created sample product: {} (ID: {})", data.productName, response.getProductId());
            } catch (Exception e) {
                log.error("Failed to create sample product: {}", data.productName, e);
            }
        }

        log.info("Sample product creation completed. Created {} products.", createdProducts.size());
        return createdProducts;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Long> prepareImageFilesForProduct(List<String> imageFileNames) {
        Map<String, Long> imageFileIdMap = new HashMap<>();
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path tempPath = Paths.get(tempDir, dateDir);

        try {
            Files.createDirectories(tempPath);
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            for (String imageFileName : imageFileNames) {
                try {
                    Resource resource = resolver.getResource("classpath:image/" + imageFileName);
                    if (!resource.exists()) {
                        log.warn("Image file not found: {}", imageFileName);
                        continue;
                    }

                    String storedFilename = UUID.randomUUID() + ".jpg";
                    Path filePath = tempPath.resolve(storedFilename);
                    Files.copy(resource.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    String url = baseUrl + "/temp/" + dateDir + "/" + storedFilename;

                    FileUpload fileUpload = FileUpload.builder()
                            .originalFilename(imageFileName)
                            .storedFilename(storedFilename)
                            .filePath(filePath.toString())
                            .fileSize(resource.contentLength())
                            .contentType("image/jpeg")
                            .status("TEMP")
                            .url(url)
                            .build();

                    FileUpload savedFile = fileUploadRepository.save(fileUpload);
                    imageFileIdMap.put(imageFileName, savedFile.getFileId());

                } catch (IOException e) {
                    log.error("Failed to prepare image file: {}", imageFileName, e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to create temp directory", e);
        }

        return imageFileIdMap;
    }

    private List<SampleProductData> getSampleProductDataList() {
        List<SampleProductData> products = new ArrayList<>();

        // 자동완성 테스트를 위해 비슷한 이름의 상품들 포함

        // 프리미엄 시리즈 (티셔츠)
        products.add(new SampleProductData(
                "프리미엄 코튼 티셔츠", "PREMIUM-TOP-001",
                "최고급 면 소재의 편안한 티셔츠입니다. 사계절 내내 입기 좋습니다.",
                new BigDecimal("45000"), new BigDecimal("39000"),
                List.of("top-white.jpg", "top-blue.jpg"),
                List.of("화이트", "블루"), "색상"
        ));

        products.add(new SampleProductData(
                "프리미엄 린넨 티셔츠", "PREMIUM-TOP-002",
                "시원한 린넨 소재의 여름용 티셔츠입니다.",
                new BigDecimal("55000"), new BigDecimal("49000"),
                List.of("top-blue.jpg", "top-pink.jpg"),
                List.of("블루", "핑크"), "색상"
        ));

        products.add(new SampleProductData(
                "프리미엄 울 니트", "PREMIUM-TOP-003",
                "부드러운 울 소재의 따뜻한 니트입니다.",
                new BigDecimal("89000"), new BigDecimal("79000"),
                List.of("top-pink.jpg", "top-white.jpg"),
                List.of("핑크", "화이트"), "색상"
        ));

        // 슬림핏 시리즈 (바지)
        products.add(new SampleProductData(
                "슬림핏 청바지 블루", "SLIM-BOTTOM-001",
                "날씬해 보이는 슬림핏 청바지입니다.",
                new BigDecimal("69000"), new BigDecimal("59000"),
                List.of("bottom-black.jpg", "bottom-brown.jpg"),
                List.of("블랙", "브라운"), "색상"
        ));

        products.add(new SampleProductData(
                "슬림핏 청바지 블랙", "SLIM-BOTTOM-002",
                "세련된 블랙 컬러의 슬림핏 청바지입니다.",
                new BigDecimal("69000"), new BigDecimal("59000"),
                List.of("bottom-black.jpg"),
                List.of("블랙"), "색상"
        ));

        products.add(new SampleProductData(
                "슬림핏 면바지", "SLIM-BOTTOM-003",
                "편안한 착용감의 면 소재 슬림핏 바지입니다.",
                new BigDecimal("59000"), new BigDecimal("49000"),
                List.of("bottom-white.jpg", "bottom-brown.jpg"),
                List.of("화이트", "브라운"), "색상"
        ));

        // 클래식 시리즈 (신발)
        products.add(new SampleProductData(
                "클래식 러닝화", "CLASSIC-SHOES-001",
                "오래도록 사랑받는 클래식 디자인의 러닝화입니다.",
                new BigDecimal("129000"), new BigDecimal("99000"),
                List.of("shoes-black.jpg", "Shoes-red.jpg"),
                List.of("블랙", "레드"), "색상"
        ));

        products.add(new SampleProductData(
                "클래식 워킹화", "CLASSIC-SHOES-002",
                "편안한 워킹을 위한 클래식 디자인 신발입니다.",
                new BigDecimal("119000"), new BigDecimal("89000"),
                List.of("Shoes-brown.jpg", "Shoes-purple.jpg"),
                List.of("브라운", "퍼플"), "색상"
        ));

        // 스포츠 시리즈 (신발)
        products.add(new SampleProductData(
                "스포츠 러닝화", "SPORT-SHOES-001",
                "뛰어난 쿠셔닝의 스포츠 러닝화입니다.",
                new BigDecimal("159000"), new BigDecimal("129000"),
                List.of("Shoes-red.jpg", "shoes-black.jpg", "Shoes-purple.jpg"),
                List.of("레드", "블랙", "퍼플"), "색상"
        ));

        products.add(new SampleProductData(
                "캐주얼 스니커즈", "CASUAL-SHOES-001",
                "일상에서 편하게 신을 수 있는 캐주얼 스니커즈입니다.",
                new BigDecimal("89000"), new BigDecimal("69000"),
                List.of("Shoes-purple.jpg", "Shoes-brown.jpg"),
                List.of("퍼플", "브라운"), "색상"
        ));

        // 데일리 시리즈 (티셔츠)
        products.add(new SampleProductData(
                "데일리 티셔츠 화이트", "DAILY-TOP-001",
                "매일 입기 좋은 기본 화이트 티셔츠입니다.",
                new BigDecimal("29000"), new BigDecimal("25000"),
                List.of("top-white.jpg"),
                List.of("화이트"), "색상"
        ));

        products.add(new SampleProductData(
                "데일리 티셔츠 블루", "DAILY-TOP-002",
                "매일 입기 좋은 시원한 블루 티셔츠입니다.",
                new BigDecimal("29000"), new BigDecimal("25000"),
                List.of("top-blue.jpg"),
                List.of("블루"), "색상"
        ));

        // 베이직 시리즈
        products.add(new SampleProductData(
                "베이직 팬츠", "BASIC-BOTTOM-001",
                "어디에나 잘 어울리는 베이직 팬츠입니다.",
                new BigDecimal("49000"), new BigDecimal("39000"),
                List.of("bottom-white.jpg", "bottom-black.jpg", "bottom-brown.jpg"),
                List.of("화이트", "블랙", "브라운"), "색상"
        ));

        products.add(new SampleProductData(
                "베이직 운동화", "BASIC-SHOES-001",
                "기본에 충실한 편안한 운동화입니다.",
                new BigDecimal("79000"), new BigDecimal("59000"),
                List.of("shoes-black.jpg", "Shoes-red.jpg", "Shoes-brown.jpg"),
                List.of("블랙", "레드", "브라운"), "색상"
        ));

        // 와이드 팬츠
        products.add(new SampleProductData(
                "와이드 데님 팬츠", "WIDE-BOTTOM-001",
                "트렌디한 와이드 핏의 데님 팬츠입니다.",
                new BigDecimal("79000"), new BigDecimal("69000"),
                List.of("bottom-brown.jpg", "bottom-white.jpg"),
                List.of("브라운", "화이트"), "색상"
        ));

        return products;
    }

    private ProductCreateRequest buildProductRequest(SampleProductData data, Map<String, Long> imageFileIdMap) {
        // 옵션 값 생성
        List<OptionValueRequest> optionValues = new ArrayList<>();
        for (int i = 0; i < data.optionValues.size(); i++) {
            optionValues.add(OptionValueRequest.builder()
                    .id("opt_" + i)
                    .optionValueName(data.optionValues.get(i))
                    .displayOrder(i)
                    .build());
        }

        // 옵션 그룹 생성
        List<OptionGroupRequest> optionGroups = List.of(
                OptionGroupRequest.builder()
                        .optionGroupName(data.optionGroupName)
                        .displayOrder(0)
                        .optionValues(optionValues)
                        .build()
        );

        // SKU 생성 (각 옵션 값별로 하나씩)
        List<SkuRequest> skus = new ArrayList<>();
        for (int i = 0; i < data.optionValues.size(); i++) {
            skus.add(SkuRequest.builder()
                    .skuCode(data.productCode + "-" + (i + 1))
                    .price(data.salePrice)
                    .stockQty(100 + (i * 10))
                    .status("ACTIVE")
                    .optionValueIds(List.of("opt_" + i))
                    .build());
        }

        // 이미지 생성
        List<ProductImageRequest> images = new ArrayList<>();
        for (int i = 0; i < data.imageFiles.size(); i++) {
            Long fileId = imageFileIdMap.get(data.imageFiles.get(i));
            if (fileId != null) {
                images.add(ProductImageRequest.builder()
                        .fileId(fileId)
                        .isPrimary(i == 0)
                        .displayOrder(i)
                        .build());
            }
        }

        return ProductCreateRequest.builder()
                .productName(data.productName)
                .productCode(data.productCode)
                .description(data.description)
                .basePrice(data.basePrice)
                .salePrice(data.salePrice)
                .status("ACTIVE")
                .isDisplayed(true)
                .categoryIds(new HashSet<>())
                .optionGroups(optionGroups)
                .skus(skus)
                .images(images)
                .build();
    }

    private static class SampleProductData {
        String productName;
        String productCode;
        String description;
        BigDecimal basePrice;
        BigDecimal salePrice;
        List<String> imageFiles;
        List<String> optionValues;
        String optionGroupName;

        SampleProductData(String productName, String productCode, String description,
                          BigDecimal basePrice, BigDecimal salePrice,
                          List<String> imageFiles, List<String> optionValues, String optionGroupName) {
            this.productName = productName;
            this.productCode = productCode;
            this.description = description;
            this.basePrice = basePrice;
            this.salePrice = salePrice;
            this.imageFiles = imageFiles;
            this.optionValues = optionValues;
            this.optionGroupName = optionGroupName;
        }
    }
}
