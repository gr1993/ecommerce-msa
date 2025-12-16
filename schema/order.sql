-- 주문 테이블
CREATE TABLE orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '주문 ID',
    order_number VARCHAR(50) NOT NULL UNIQUE COMMENT '주문 번호',
    user_id BIGINT NOT NULL COMMENT '주문자 ID',

    order_status VARCHAR(30) NOT NULL COMMENT '주문 상태
    (CREATED, PAID, SHIPPING, DELIVERED, CANCELED)',

    total_product_amount DECIMAL(12,2) NOT NULL COMMENT '상품 총 금액',
    total_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '할인 총 금액',
    total_payment_amount DECIMAL(12,2) NOT NULL COMMENT '최종 결제 금액',

    ordered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '주문 일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시'
) COMMENT='주문 정보 테이블';


-- 주문 상품 테이블
CREATE TABLE order_item (
    order_item_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '주문 상품 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',

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
) COMMENT='주문에 포함된 상품 정보';


-- 주문 배송 정보 테이블
CREATE TABLE order_shipping (
    shipping_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '배송 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',

    receiver_name VARCHAR(100) NOT NULL COMMENT '수령인 이름',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '수령인 연락처',
    address VARCHAR(500) NOT NULL COMMENT '배송 주소',
    postal_code VARCHAR(20) COMMENT '우편번호',

    shipping_status VARCHAR(30) NOT NULL COMMENT '배송 상태
    (READY, SHIPPING, DELIVERED)',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

    CONSTRAINT fk_shipping_order
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE
) COMMENT='주문 배송 정보';


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
