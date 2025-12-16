-- 카테고리 테이블: 상품 전시용 카테고리 정보 관리
CREATE TABLE catalog_category (
    category_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '카테고리 ID',
    parent_id        BIGINT DEFAULT NULL COMMENT '상위 카테고리 ID (최상위인 경우 NULL)',
    category_name    VARCHAR(100) NOT NULL COMMENT '카테고리명',
    display_order    INT NOT NULL DEFAULT 0 COMMENT '전시 순서',
    is_displayed     BOOLEAN NOT NULL DEFAULT TRUE COMMENT '전시 여부',
    
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시'
) COMMENT='카테고리 정보를 저장하는 테이블';

-- 카테고리 계층 구조를 위한 자기 참조 외래키
ALTER TABLE catalog_category
ADD CONSTRAINT fk_catalog_category_parent
FOREIGN KEY (parent_id) REFERENCES catalog_category(category_id)
ON DELETE SET NULL;


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