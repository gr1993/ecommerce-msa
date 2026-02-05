-- 주문 DB 생성
CREATE DATABASE IF NOT EXISTS order_service;
USE order_service;

-- 주문 테이블
CREATE TABLE orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '주문 ID',
    order_number VARCHAR(50) NOT NULL UNIQUE COMMENT '주문 번호',
    user_id BIGINT NOT NULL COMMENT '주문자 ID',
    order_status VARCHAR(30) NOT NULL COMMENT '주문 상태
    (CREATED, PAID, FILED, SHIPPING, DELIVERED, CANCELED)',
    total_product_amount DECIMAL(12,2) NOT NULL COMMENT '상품 총 금액',
    total_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '할인 총 금액',
    total_payment_amount DECIMAL(12,2) NOT NULL COMMENT '최종 결제 금액',
    order_memo TEXT COMMENT '관리자 메모 / CS 메모',
    ordered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '주문 일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시'
) COMMENT='주문 정보 테이블';

-- 주문 상품 테이블
CREATE TABLE order_item (
    order_item_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '주문 상품 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    sku_id BIGINT NOT NULL COMMENT '상품 옵션 SKU ID',
    product_name VARCHAR(200) NOT NULL COMMENT '상품명 스냅샷',
    product_code VARCHAR(50) COMMENT '상품 코드 스냅샷',
    quantity INT NOT NULL COMMENT '수량',
    unit_price DECIMAL(12,2) NOT NULL COMMENT '상품 단가',
    total_price DECIMAL(12,2) NOT NULL COMMENT '상품 총 금액',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE
) COMMENT='주문에 포함된 상품 정보 (SKU 단위 포함)';

-- 주문 배송지 정보 테이블
CREATE TABLE order_delivery (
    delivery_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '배송지 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    receiver_name VARCHAR(50) NOT NULL COMMENT '받는 분 이름',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '받는 분 연락처',
    zipcode VARCHAR(10) NOT NULL COMMENT '우편번호',
    address VARCHAR(255) NOT NULL COMMENT '기본 주소',
    address_detail VARCHAR(255) NOT NULL COMMENT '상세 주소',
    delivery_memo VARCHAR(255) COMMENT '배송 메모 (요청사항)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    CONSTRAINT fk_order_delivery_order
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE
) COMMENT='주문 배송지 정보 테이블';

-- 주문 결제 테이블
CREATE TABLE order_payment (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '결제 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    payment_method VARCHAR(30) NOT NULL COMMENT '결제 수단
    (CARD, BANK_TRANSFER, KAKAO_PAY 등)',
    payment_amount DECIMAL(12,2) NOT NULL COMMENT '결제 금액',
    payment_status VARCHAR(30) NOT NULL COMMENT '결제 상태
    (READY, PAID, FAILED, CANCELED)',
    paid_at TIMESTAMP COMMENT '결제 완료 일시',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE
) COMMENT='주문 결제 정보';


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