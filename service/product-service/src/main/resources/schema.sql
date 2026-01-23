-- 상품 DB 생성
CREATE DATABASE IF NOT EXISTS product_service;
USE product_service;

-- 상품 테이블
CREATE TABLE product (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '상품 ID',
    product_name VARCHAR(200) NOT NULL COMMENT '상품명',
    product_code VARCHAR(50) UNIQUE COMMENT '상품 코드',
    description TEXT COMMENT '상품 상세 설명',
    base_price DECIMAL(12,2) NOT NULL COMMENT '기본 가격',
    sale_price DECIMAL(12,2) COMMENT '할인 가격',
    status VARCHAR(20) NOT NULL COMMENT '상품 상태: ACTIVE, INACTIVE, SOLD_OUT',
    is_displayed TINYINT(1) NOT NULL DEFAULT 1 COMMENT '진열 여부',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시'
) COMMENT='상품 정보를 저장하는 테이블';

-- 상품 옵션 그룹 테이블
CREATE TABLE product_option_group (
    option_group_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '옵션 그룹 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    option_group_name VARCHAR(100) NOT NULL COMMENT '옵션 그룹명 (예: 색상, 사이즈)',
    display_order INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    CONSTRAINT fk_option_group_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE CASCADE
) COMMENT='상품 옵션 그룹 정보를 저장하는 테이블';

-- 상품 옵션 값 테이블
CREATE TABLE product_option_value (
    option_value_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '옵션 값 ID',
    option_group_id BIGINT NOT NULL COMMENT '옵션 그룹 ID',
    option_value_name VARCHAR(100) NOT NULL COMMENT '옵션 값명 (예: Red, Blue, M, L)',
    display_order INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    CONSTRAINT fk_option_value_group
        FOREIGN KEY (option_group_id)
        REFERENCES product_option_group(option_group_id)
        ON DELETE CASCADE
) COMMENT='상품 옵션 값을 저장하는 테이블';

-- 상품 SKU 테이블
CREATE TABLE product_sku (
    sku_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'SKU ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    sku_code VARCHAR(50) UNIQUE COMMENT 'SKU 코드',
    price DECIMAL(12,2) NOT NULL COMMENT '가격',
    stock_qty INT NOT NULL DEFAULT 0 COMMENT '재고 수량',
    status VARCHAR(20) NOT NULL COMMENT 'SKU 상태: ACTIVE, SOLD_OUT, INACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    CONSTRAINT fk_sku_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE CASCADE
) COMMENT='상품 SKU 정보를 저장하는 테이블';

-- SKU와 옵션 값 연결 테이블
CREATE TABLE product_sku_option (
    sku_option_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'SKU 옵션 ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    option_value_id BIGINT NOT NULL COMMENT '옵션 값 ID',
    PRIMARY KEY (sku_option_id),
    CONSTRAINT fk_sku_option_sku
        FOREIGN KEY (sku_id)
        REFERENCES product_sku(sku_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_sku_option_value
        FOREIGN KEY (option_value_id)
        REFERENCES product_option_value(option_value_id)
        ON DELETE CASCADE
) COMMENT='SKU와 옵션 값을 매핑하는 테이블';

-- 파일 업로드 기록 테이블
CREATE TABLE file_upload (
    file_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '파일 ID',
    original_filename VARCHAR(255) NOT NULL COMMENT '원본 파일명',
    stored_filename VARCHAR(255) NOT NULL COMMENT '저장된 파일명',
    file_path VARCHAR(500) NOT NULL COMMENT '파일 경로',
    file_size BIGINT NOT NULL COMMENT '파일 크기 (bytes)',
    content_type VARCHAR(100) COMMENT '파일 타입',
    status VARCHAR(20) NOT NULL COMMENT '파일 상태: TEMP, CONFIRMED, DELETED',
    url VARCHAR(500) NOT NULL COMMENT '접근 URL',
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '업로드일시',
    confirmed_at TIMESTAMP COMMENT '확정일시',
    INDEX idx_status_uploaded (status, uploaded_at)
) COMMENT='파일 업로드 기록 테이블';

-- 상품 이미지 테이블
CREATE TABLE product_image (
    image_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '이미지 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    file_id BIGINT NOT NULL COMMENT '파일 ID',
    image_url TEXT NOT NULL COMMENT '이미지 URL',
    is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '대표 이미지 여부',
    display_order INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    CONSTRAINT fk_image_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_image_file
            FOREIGN KEY (file_id)
            REFERENCES file_upload(file_id)
            ON DELETE CASCADE
) COMMENT='상품 이미지 정보를 저장하는 테이블';


-- 카테고리 테이블: 상품 전시용 카테고리 정보 관리
CREATE TABLE category (
    category_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '카테고리 ID',
    parent_id        BIGINT DEFAULT NULL COMMENT '상위 카테고리 ID (최상위인 경우 NULL)',
    category_name    VARCHAR(100) NOT NULL COMMENT '카테고리명',
    display_order    INT NOT NULL DEFAULT 0 COMMENT '전시 순서',
    is_displayed     BOOLEAN NOT NULL DEFAULT TRUE COMMENT '전시 여부',
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시'
) COMMENT='카테고리 정보를 저장하는 테이블';

-- 카테고리 계층 구조를 위한 자기 참조 외래키
ALTER TABLE category
ADD CONSTRAINT fk_category_parent
FOREIGN KEY (parent_id) REFERENCES category(category_id)
ON DELETE SET NULL;


-- 상품과 카테고리 매핑 테이블
CREATE TABLE product_category (
    product_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_pc_product FOREIGN KEY (product_id)
        REFERENCES product(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_pc_category FOREIGN KEY (category_id)
        REFERENCES category(category_id) ON DELETE CASCADE
) COMMENT='상품과 카테고리를 연결하는 매핑 테이블';


-- 상품 전시 영역 매핑 테이블
CREATE TABLE product_display_mapping (
    mapping_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '매핑 테이블 PK',
    product_id      BIGINT NOT NULL COMMENT '연결된 상품 ID',
    display_area    VARCHAR(50) NOT NULL COMMENT '전시 영역: MAIN, RECOMMEND, EVENT 등',
    display_order   INT NOT NULL DEFAULT 0 COMMENT '해당 영역 내 전시 순서',
    is_displayed    BOOLEAN NOT NULL DEFAULT TRUE COMMENT '전시 여부',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    CONSTRAINT fk_display_product FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE CASCADE
) COMMENT='상품 전시 영역 매핑 테이블';


-- 상품 검색 키워드 테이블
CREATE TABLE product_search_keyword (
    keyword_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '검색 키워드 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    keyword VARCHAR(100) NOT NULL COMMENT '검색 키워드',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    CONSTRAINT fk_psk_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_product_keyword
        UNIQUE (product_id, keyword)
) COMMENT='상품별 검색 키워드 매핑 테이블';


-- 이벤트 메시지 테이블
CREATE TABLE outbox (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '이벤트 ID',
    aggregate_type VARCHAR(255) NOT NULL COMMENT '이벤트 소스 타입',
    aggregate_id VARCHAR(255) NOT NULL COMMENT '이벤트 소스 ID',
    event_type VARCHAR(255) NOT NULL COMMENT '이벤트 타입',
    payload TEXT NOT NULL COMMENT '이벤트 페이로드',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '이벤트 상태 (PENDING, PUBLISHED, FAILED)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    published_at TIMESTAMP NULL COMMENT '발행 일시',
    INDEX idx_event_type_status (event_type, status) COMMENT '이벤트 타입과 상태 인덱스'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='이벤트 메시지 테이블';