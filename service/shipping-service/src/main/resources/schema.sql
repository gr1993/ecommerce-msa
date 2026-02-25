-- 배송 DB 생성
CREATE DATABASE IF NOT EXISTS shipping_service;
USE shipping_service;


-- 배송 테이블
CREATE TABLE order_shipping (
    shipping_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '배송 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    order_number VARCHAR(50) NOT NULL UNIQUE COMMENT '주문 번호',
    user_id BIGINT COMMENT '사용자 ID (order.created 이벤트 소비 시 저장)',
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
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시'
) COMMENT='주문 배송 정보';


-- 배송 상태 변경 이력 테이블
CREATE TABLE order_shipping_history (
    shipping_history_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '배송 상태 이력 ID',
    shipping_id BIGINT NOT NULL COMMENT '배송 ID',
    previous_status VARCHAR(30) COMMENT '이전 배송 상태',
    new_status VARCHAR(30) COMMENT '변경 후 배송 상태',
    location VARCHAR(100) COMMENT '배송 위치 (외부 API where 필드)',
    remark VARCHAR(200) COMMENT '배송 상세 설명 (외부 API remark 필드)',
    tracking_kind VARCHAR(30) COMMENT '외부 배송사 상태 코드 (ACCEPTED, PICKED_UP 등)',
    tracking_number VARCHAR(100) COMMENT '이력 기록 시점의 운송장 번호 (배송/반품 구분용)',
    changed_by VARCHAR(50) COMMENT '변경자 (관리자 계정 등)',
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '변경 일시',
    CONSTRAINT fk_shipping_history_shipping
        FOREIGN KEY (shipping_id)
        REFERENCES order_shipping(shipping_id)
        ON DELETE CASCADE
) COMMENT='배송 상태 변경 이력 테이블';


-- 주문 반품 정보 테이블
CREATE TABLE order_return (
    return_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '반품 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    return_status VARCHAR(30) NOT NULL COMMENT '반품 상태 (RETURN_REQUESTED, RETURN_APPROVED, RETURN_REJECTED, RETURNED)',
    reason VARCHAR(500) COMMENT '반품 사유',
    reject_reason VARCHAR(500) COMMENT '반품 거절 사유',
    tracking_number VARCHAR(50) COMMENT '회수용 운송장 번호',
    courier VARCHAR(50) COMMENT '택배사',
    receiver_name VARCHAR(100) COMMENT '반품 수령인 이름',
    receiver_phone VARCHAR(20) COMMENT '반품 수령인 연락처',
    return_address VARCHAR(500) COMMENT '반품 회수 주소',
    postal_code VARCHAR(20) COMMENT '반품 우편번호',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '반품 신청 일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '반품 정보 수정 일시'
) COMMENT='주문 상품 반품 정보 테이블';


-- 주문 반품 상태 변경 이력 테이블
CREATE TABLE order_return_history (
    return_history_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '반품 상태 이력 ID',
    return_id BIGINT NOT NULL COMMENT '반품 ID',
    previous_status VARCHAR(30) COMMENT '이전 반품 상태 (예: RETURN_APPROVED)',
    new_status VARCHAR(30) COMMENT '변경 후 반품 상태 (예: RETURN_IN_TRANSIT)',
    location VARCHAR(100) COMMENT '반품 회수 위치 (외부 API where 필드)',
    remark VARCHAR(200) COMMENT '반품 회수 상세 설명 (외부 API remark 필드)',
    tracking_kind VARCHAR(30) COMMENT '외부 배송사 상태 코드 (ACCEPTED, PICKED_UP 등)',
    tracking_number VARCHAR(100) COMMENT '이력 기록 시점의 반품 회수 운송장 번호',
    changed_by VARCHAR(50) COMMENT '변경자 (시스템 스케줄러, 관리자 계정, 사용자 등)',
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '변경 일시',
    CONSTRAINT fk_return_history_return
        FOREIGN KEY (return_id)
        REFERENCES order_return(return_id)
        ON DELETE CASCADE
) COMMENT='반품 상태 변경 이력 테이블';


-- Kafka 이벤트 멱등성 처리 테이블
CREATE TABLE processed_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '처리 이벤트 ID',
    event_id VARCHAR(100) NOT NULL UNIQUE COMMENT '이벤트 고유 식별자 (예: order.created:{orderId})',
    event_type VARCHAR(50) NOT NULL COMMENT '이벤트 타입 (예: order.created)',
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '이벤트 처리 일시'
) COMMENT='Kafka 이벤트 중복 처리 방지용 멱등성 테이블';


-- 주문 교환 정보 테이블
CREATE TABLE order_exchange (
    exchange_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '교환 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    exchange_status VARCHAR(30) NOT NULL COMMENT '교환 상태 (EXCHANGE_REQUESTED, EXCHANGE_APPROVED, EXCHANGE_REJECTED, EXCHANGED)',
    reason VARCHAR(500) COMMENT '교환 사유',
    reject_reason VARCHAR(500) COMMENT '교환 거절 사유',
    tracking_number VARCHAR(50) COMMENT '회수/배송용 운송장 번호',
    courier VARCHAR(50) COMMENT '택배사',
    receiver_name VARCHAR(100) COMMENT '교환 수령인 이름',
    receiver_phone VARCHAR(20) COMMENT '교환 수령인 연락처',
    exchange_address VARCHAR(500) COMMENT '교환 회수/배송 주소',
    postal_code VARCHAR(20) COMMENT '교환 우편번호',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '교환 신청 일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '교환 정보 수정 일시'
) COMMENT='주문 상품 교환 정보 테이블';


-- 주문 교환 상태 변경 이력 테이블
CREATE TABLE order_exchange_history (
    exchange_history_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '교환 상태 이력 ID',
    exchange_id BIGINT NOT NULL COMMENT '교환 ID',
    previous_status VARCHAR(30) COMMENT '이전 교환 상태 (예: EXCHANGE_APPROVED)',
    new_status VARCHAR(30) COMMENT '변경 후 교환 상태 (예: EXCHANGE_IN_TRANSIT)',
    location VARCHAR(100) COMMENT '교환 회수 위치 (외부 API where 필드)',
    remark VARCHAR(200) COMMENT '교환 회수 상세 설명 (외부 API remark 필드)',
    tracking_kind VARCHAR(30) COMMENT '외부 배송사 상태 코드 (ACCEPTED, PICKED_UP 등)',
    tracking_number VARCHAR(100) COMMENT '이력 기록 시점의 교환 회수 운송장 번호',
    changed_by VARCHAR(50) COMMENT '변경자 (시스템 스케줄러, 관리자 계정, 사용자 등)',
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '변경 일시',
    CONSTRAINT fk_exchange_history_exchange
        FOREIGN KEY (exchange_id)
        REFERENCES order_exchange(exchange_id)
        ON DELETE CASCADE
) COMMENT='교환 상태 변경 이력 테이블';


-- 이벤트 Outbox 테이블 (Transaction Outbox Pattern)
CREATE TABLE outbox (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '이벤트 ID',
    aggregate_type VARCHAR(255) NOT NULL COMMENT '이벤트 소스 타입 (Return, Exchange 등)',
    aggregate_id VARCHAR(255) NOT NULL COMMENT '이벤트 소스 ID',
    event_type VARCHAR(255) NOT NULL COMMENT '이벤트 타입 (Kafka 토픽명)',
    payload TEXT NOT NULL COMMENT '이벤트 페이로드 (JSON)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '이벤트 상태 (PENDING, PUBLISHED, FAILED)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    published_at TIMESTAMP NULL COMMENT '발행 일시',
    INDEX idx_event_type_status (event_type, status) COMMENT '이벤트 타입과 상태 인덱스'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='이벤트 메시지 Outbox 테이블';