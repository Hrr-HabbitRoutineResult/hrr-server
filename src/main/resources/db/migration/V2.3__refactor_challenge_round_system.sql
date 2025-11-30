-- Round 테이블 구조 변경
ALTER TABLE `round`
    ADD COLUMN challenge_id BIGINT NOT NULL,
    ADD COLUMN round_number INT NOT NULL,
    ADD COLUMN start_date DATE NOT NULL,
    ADD COLUMN end_date DATE NOT NULL;

-- 외래키
ALTER TABLE `round`
    ADD CONSTRAINT FK_round_challenge
        FOREIGN KEY (challenge_id) REFERENCES challenge (id);

-- 유니크: @UniqueConstraint(name = "uk_challenge_round_number", ...)
ALTER TABLE `round`
    ADD CONSTRAINT uk_challenge_round_number
        UNIQUE (challenge_id, round_number);

-- 인덱스: @Index(name = "idx_round_end_date", ...)
CREATE INDEX idx_round_end_date ON `round` (end_date);


-- Challenge 테이블 수정
ALTER TABLE challenge
    ADD COLUMN current_round_id BIGINT;

ALTER TABLE challenge
    ADD CONSTRAINT FK_challenge_current_round
        FOREIGN KEY (current_round_id) REFERENCES `round` (id);


-- UserChallenge 테이블 수정
-- 기존 점수 관련 컬럼 삭제
ALTER TABLE user_challenge
DROP COLUMN verification_count,
    DROP COLUMN verification_uncount,
    DROP COLUMN warn_count,
    DROP COLUMN verification_status;

-- 신규 컬럼 추가
ALTER TABLE user_challenge
    ADD COLUMN status VARCHAR(255) DEFAULT 'JOINED' NOT NULL,
    ADD COLUMN kick_warnings INT DEFAULT 0 NOT NULL;

-- 유니크: @UniqueConstraint(columnNames = {"user_id", "challenge_id"})
ALTER TABLE user_challenge
    ADD CONSTRAINT uk_user_challenge_user_challenge
        UNIQUE (user_id, challenge_id);


-- RoundRecord 테이블 신규 생성
CREATE TABLE round_record (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              created_at DATETIME(6) NOT NULL,
                              updated_at DATETIME(6),

                              round_id BIGINT NOT NULL,
                              user_challenge_id BIGINT NOT NULL,

                              verification_count INT DEFAULT 0 NOT NULL,
                              warn_count INT DEFAULT 0 NOT NULL,
                              final_rank INT,
                              next_round_intent VARCHAR(255) DEFAULT 'UNDECIDED' NOT NULL,

                              CONSTRAINT FK_round_record_round
                                  FOREIGN KEY (round_id) REFERENCES `round` (id),
                              CONSTRAINT FK_round_record_user_challenge
                                  FOREIGN KEY (user_challenge_id) REFERENCES user_challenge (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 유니크: @UniqueConstraint(name = "uk_round_user", ...)
ALTER TABLE round_record
    ADD CONSTRAINT uk_round_user
        UNIQUE (round_id, user_challenge_id);

-- 인덱스: @Index(name = "idx_round_id", ...)
CREATE INDEX idx_round_id ON round_record (round_id);

-- 인덱스: @Index(name = "idx_user_history", ...)
CREATE INDEX idx_user_history ON round_record (user_challenge_id);


-- Verification 테이블 구조 변경
ALTER TABLE verification
    ADD COLUMN round_record_id BIGINT NOT NULL;

ALTER TABLE verification
    ADD CONSTRAINT FK_verification_round_record
        FOREIGN KEY (round_record_id) REFERENCES round_record (id);