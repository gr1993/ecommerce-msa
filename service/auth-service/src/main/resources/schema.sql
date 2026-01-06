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
