-- 챌린지 신고 이력 저장 테이블 생성
CREATE TABLE challenge_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    challenge_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL,
    description VARCHAR(200) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_challenge_report_reporter_challenge UNIQUE (reporter_id, challenge_id),
    CONSTRAINT fk_challenge_report_reporter FOREIGN KEY (reporter_id) REFERENCES user (id),
    CONSTRAINT fk_challenge_report_challenge FOREIGN KEY (challenge_id) REFERENCES challenge (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_challenge_report_challenge_id ON challenge_report (challenge_id);
