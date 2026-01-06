-- 인증 DB 생성
CREATE DATABASE IF NOT EXISTS auth_service;
USE auth_service;

-- 인증 테이블
CREATE TABLE IF NOT EXISTS auth_users (
    user_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
    email VARCHAR(255) NOT NULL COMMENT '사용자 이메일',
    password VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '계정 상태 (ACTIVE, INACTIVE, SUSPENDED)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

    PRIMARY KEY (user_id),
    UNIQUE KEY uk_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='인증 사용자 테이블';

-- 이벤트 처리 이력 테이블
CREATE TABLE processed_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '이벤트 처리 이력 ID',
    event_type VARCHAR(100) NOT NULL COMMENT '이벤트 타입',
    event_key VARCHAR(255) NOT NULL COMMENT '이벤트 고유 키 (중복 처리 식별용)',
    payload TEXT COMMENT '원본 이벤트 페이로드',
    status VARCHAR(20) NOT NULL COMMENT '처리 상태 (SUCCESS, FAILED, DUPLICATE 등)',
    processed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '처리 완료 일시',
    result_message VARCHAR(1000) COMMENT '처리 결과 메시지 또는 오류 상세',
    INDEX idx_event_type_event_key (event_type, event_key) COMMENT '이벤트 타입과 키 인덱스',
    INDEX idx_processed_at (processed_at) COMMENT '처리 일시 인덱스',
    UNIQUE KEY uk_event_type_key (event_type, event_key) COMMENT '이벤트 타입 + 키 유니크 제약 (중복 방지)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='이벤트 처리 이력 테이블';