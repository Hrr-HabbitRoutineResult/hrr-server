-- 기존 테이블 삭제 (상속 구조 및 연관 관계 변경 반영을 위해 재생성)
DROP TABLE IF EXISTS verification_post_report;
DROP TABLE IF EXISTS user_report;

-- 인증 게시글 신고 테이블 생성
CREATE TABLE verification_post_report (
          id BIGINT AUTO_INCREMENT PRIMARY KEY,
          reporter_id BIGINT NOT NULL,              -- BaseReport: 신고자 (User 참조)
          verification_id BIGINT NOT NULL,          -- VerificationPostReport: 대상 게시글
          reason VARCHAR(50) NOT NULL,              -- BaseReport: 사유 Enum
          description TEXT,                         -- BaseReport: 상세 내용
          status VARCHAR(50) DEFAULT 'PENDING' NOT NULL, -- BaseReport: 상태 Enum
          created_at DATETIME(6) NOT NULL,
          updated_at DATETIME(6) NOT NULL,

-- 외래 키 설정 (User 테이블 및 Verification 테이블 참조)
          CONSTRAINT fk_v_report_reporter_id FOREIGN KEY (reporter_id) REFERENCES user (id),
          CONSTRAINT fk_v_report_verification_id FOREIGN KEY (verification_id) REFERENCES verification (id)
) ENGINE=InnoDB;

-- 유저 신고 테이블 생성
CREATE TABLE user_report (
         id BIGINT AUTO_INCREMENT PRIMARY KEY,
         reporter_id BIGINT NOT NULL,              -- BaseReport: 신고자 (User 참조)
         target_user_id BIGINT NOT NULL,           -- UserReport: 대상 유저 (targetUser)
         reason VARCHAR(50) NOT NULL,
         description TEXT,
         status VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
         created_at DATETIME(6) NOT NULL,
         updated_at DATETIME(6) NOT NULL,

-- 외래 키 설정 (User 테이블 참조 - 신고자와 피신고자 모두)
         CONSTRAINT fk_u_report_reporter_id FOREIGN KEY (reporter_id) REFERENCES user (id),
         CONSTRAINT fk_u_report_target_user_id FOREIGN KEY (target_user_id) REFERENCES user (id)
) ENGINE=InnoDB;

-- 인덱스 설정 (조회 성능 최적화)
CREATE INDEX idx_v_report_reporter_id ON verification_post_report(reporter_id);
CREATE INDEX idx_v_report_verification_id ON verification_post_report(verification_id);
CREATE INDEX idx_u_report_reporter_id ON user_report(reporter_id);
CREATE INDEX idx_u_report_target_user_id ON user_report(target_user_id);
