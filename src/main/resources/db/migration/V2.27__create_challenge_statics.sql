-- 1. challenge_statics 테이블 생성
CREATE TABLE IF NOT EXISTS challenge_statics (
                                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 challenge_id BIGINT NOT NULL,
                                                 favor_type VARCHAR(50) NOT NULL,
                                                 favor_value VARCHAR(100) NOT NULL,
                                                 stat_number INT DEFAULT 0,
                                                 created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                                                 updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    -- 외래 키 설정 (Challenge 테이블 참조)
                                                 CONSTRAINT fk_challenge_statics_challenge
                                                     FOREIGN KEY (challenge_id) REFERENCES challenge (id)
                                                         ON DELETE CASCADE
);

-- 2. 멱등성을 위한 유니크 인덱스 추가 (동일 챌린지에 같은 타입/값이 중복 저장되지 않도록 방지)
ALTER TABLE challenge_statics
    ADD CONSTRAINT uk_challenge_favor UNIQUE (challenge_id, favor_type, favor_value);

-- 3. 조회 성능을 위한 인덱스 추가
CREATE INDEX idx_statics_challenge_id ON challenge_statics (challenge_id);
