-- 쿠폰 정책 테이블
CREATE TABLE coupon (
    coupon_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '쿠폰 ID',
    coupon_code VARCHAR(50) NOT NULL UNIQUE COMMENT '쿠폰 코드',
    coupon_name VARCHAR(100) NOT NULL COMMENT '쿠폰명',

    discount_type VARCHAR(20) NOT NULL COMMENT '할인 유형 (FIXED, RATE)',
    discount_value DECIMAL(12,2) NOT NULL COMMENT '할인 금액 또는 할인율',

    min_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '최소 구매 금액',
    max_discount_amount DECIMAL(12,2) COMMENT '최대 할인 금액 (정률 할인 시)',

    valid_from TIMESTAMP NOT NULL COMMENT '쿠폰 시작 일시',
    valid_to TIMESTAMP NOT NULL COMMENT '쿠폰 종료 일시',

    status VARCHAR(20) NOT NULL COMMENT '상태 (ACTIVE, INACTIVE, EXPIRED)',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시'
) COMMENT='쿠폰 정책(마스터) 테이블';


-- 사용자 쿠폰 테이블
CREATE TABLE user_coupon (
    user_coupon_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 쿠폰 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    coupon_id BIGINT NOT NULL COMMENT '쿠폰 ID',

    coupon_status VARCHAR(20) NOT NULL COMMENT '쿠폰 상태 (ISSUED, USED, EXPIRED)',
    used_at TIMESTAMP COMMENT '쿠폰 사용 일시',

    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '쿠폰 발급 일시',

    CONSTRAINT fk_user_coupon_coupon
        FOREIGN KEY (coupon_id)
        REFERENCES coupon(coupon_id)
        ON DELETE CASCADE
) COMMENT='사용자 보유 쿠폰 테이블';


-- 상품/주문 단위 할인 정책 테이블
CREATE TABLE discount_policy (
    discount_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '할인 정책 ID',
    discount_name VARCHAR(100) NOT NULL COMMENT '할인 정책명',
    discount_type VARCHAR(20) NOT NULL COMMENT '할인 유형 (FIXED, RATE)',
    discount_value DECIMAL(12,2) NOT NULL COMMENT '할인 금액 또는 할인율',
    
    target_type VARCHAR(20) NOT NULL COMMENT '할인 적용 대상 (PRODUCT, CATEGORY, ORDER)',
    target_id BIGINT COMMENT '적용 대상 ID (상품 ID, 카테고리 ID 등)',
    
    min_order_amount DECIMAL(12,2) DEFAULT 0 COMMENT '적용 최소 구매 금액',
    max_discount_amount DECIMAL(12,2) COMMENT '최대 할인 금액 (정률 할인 시)',
    
    valid_from TIMESTAMP NOT NULL COMMENT '할인 시작 일시',
    valid_to TIMESTAMP NOT NULL COMMENT '할인 종료 일시',
    
    status VARCHAR(20) NOT NULL COMMENT '할인 상태 (ACTIVE, INACTIVE, EXPIRED)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시'
) COMMENT='상품/주문 단위 할인 정책 테이블';


-- 공지사항 테이블
CREATE TABLE notice (
    notice_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '공지사항 ID',
    title VARCHAR(200) NOT NULL COMMENT '공지사항 제목',
    content TEXT NOT NULL COMMENT '공지사항 내용',

    is_displayed TINYINT(1) NOT NULL DEFAULT 1 COMMENT '진열 여부 (0: 비노출, 1: 노출)',
    display_start TIMESTAMP COMMENT '노출 시작 일시',
    display_end TIMESTAMP COMMENT '노출 종료 일시',

    view_count BIGINT NOT NULL DEFAULT 0 COMMENT '조회수',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    created_by BIGINT COMMENT '생성자 ID',
    updated_by BIGINT COMMENT '수정자 ID'
) COMMENT='관리자 공지사항 정보 테이블';