CREATE TABLE order_shipping (
    shipping_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '배송 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',

    receiver_name VARCHAR(100) NOT NULL COMMENT '수령인 이름',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '수령인 연락처',
    address VARCHAR(500) NOT NULL COMMENT '배송 주소',
    postal_code VARCHAR(20) COMMENT '우편번호',

    shipping_status VARCHAR(30) NOT NULL COMMENT '배송 상태
    (READY, SHIPPING, DELIVERED, RETURNED)',

    shipping_company VARCHAR(100) COMMENT '배송사 이름',
    tracking_number VARCHAR(100) COMMENT '운송장 번호',
    delivery_service_status VARCHAR(30) COMMENT '배송사 연동 상태
    (NOT_SENT, SENT, IN_TRANSIT, DELIVERED)',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

    CONSTRAINT fk_shipping_order
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE
) COMMENT='주문 배송 정보';


-- 배송 상태 변경 이력 테이블
CREATE TABLE order_shipping_history (
    shipping_history_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '배송 상태 이력 ID',
    shipping_id BIGINT NOT NULL COMMENT '배송 ID',
    previous_status VARCHAR(30) COMMENT '이전 배송 상태',
    new_status VARCHAR(30) COMMENT '변경 후 배송 상태',
    changed_by VARCHAR(50) COMMENT '변경자 (관리자 계정 등)',
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '변경 일시',

    CONSTRAINT fk_shipping_history_shipping
        FOREIGN KEY (shipping_id)
        REFERENCES order_shipping(shipping_id)
        ON DELETE CASCADE
) COMMENT='배송 상태 변경 이력 테이블';
