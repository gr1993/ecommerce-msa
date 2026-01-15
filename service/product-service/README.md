# product-service


### 프로젝트 패키지 구조
```
com.example.productservice/
├── ProductServiceApplication.java
├── global/                          # 전역 설정, 공통 모듈
│   ├── config/
│   └── common/
│       └── dto/
├── product/                         # 상품 도메인
│   ├── controller/
│   ├── service/
│   ├── domain/
│   ├── repository/
│   └── dto/
├── file/                            # 파일 도메인
│   ├── service/
│   ├── domain/
│   ├── repository/
│   ├── dto/
│   └── scheduler/
├── category/                        # 카테고리 도메인
│   ├── controller/
│   ├── service/
│   ├── domain/
│   ├── repository/
│   └── dto/
├── display/                         # 화면/노출 도메인
│   ├── controller/
│   ├── service/
│   ├── domain/
│   ├── repository/
│   └── dto/
└── search/                          # 검색 도메인
    ├── controller/
    ├── service/
    ├── domain/
    ├── repository/
    └── dto/
```