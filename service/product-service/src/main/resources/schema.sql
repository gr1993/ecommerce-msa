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
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    option_value_id BIGINT NOT NULL COMMENT '옵션 값 ID',
    PRIMARY KEY (sku_id, option_value_id),
    CONSTRAINT fk_sku_option_sku
        FOREIGN KEY (sku_id)
        REFERENCES product_sku(sku_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_sku_option_value
        FOREIGN KEY (option_value_id)
        REFERENCES product_option_value(option_value_id)
        ON DELETE CASCADE
) COMMENT='SKU와 옵션 값을 매핑하는 테이블';

-- 상품 이미지 테이블
CREATE TABLE product_image (
    image_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '이미지 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    image_url TEXT NOT NULL COMMENT '이미지 URL',
    is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '대표 이미지 여부',
    display_order INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    CONSTRAINT fk_image_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE CASCADE
) COMMENT='상품 이미지 정보를 저장하는 테이블';