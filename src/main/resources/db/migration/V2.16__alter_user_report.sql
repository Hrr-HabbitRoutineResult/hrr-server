-- 기존 테이블이 있다면 삭제 후 재생성
DROP TABLE IF EXISTS user_report;

-- 새로운 구조의 user_report 테이블 생성
CREATE TABLE user_report (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             reporter_id BIGINT NOT NULL,
                             reason VARCHAR(50) NOT NULL,
                             description TEXT,
                             status VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
                             target_user_id BIGINT NOT NULL,
                             created_at DATETIME(6) NOT NULL,
                             updated_at DATETIME(6) NOT NULL,

                             CONSTRAINT fk_user_report_reporter_id FOREIGN KEY (reporter_id) REFERENCES user (id),
                             CONSTRAINT fk_user_report_target_user_id FOREIGN KEY (target_user_id) REFERENCES user (id)
) ENGINE=InnoDB;

-- 인덱스 명칭도 일관성 있게 변경
CREATE INDEX idx_user_report_reporter_id ON user_report(reporter_id);
CREATE INDEX idx_user_report_target_user_id ON user_report(target_user_id);
