-- 컬럼 추가 (user_id, challenge_id)
ALTER TABLE challenge_wait
    ADD COLUMN user_id BIGINT NOT NULL,
ADD COLUMN challenge_id BIGINT NOT NULL;

-- 외래키(Foreign Key) 제약조건 추가
ALTER TABLE challenge_wait
    ADD CONSTRAINT FK_challenge_wait_user
        FOREIGN KEY (user_id) REFERENCES user (id);

ALTER TABLE challenge_wait
    ADD CONSTRAINT FK_challenge_wait_challenge
        FOREIGN KEY (challenge_id) REFERENCES challenge (id);

-- 유니크(Unique) 제약조건 추가
ALTER TABLE challenge_wait
    ADD CONSTRAINT uk_challenge_wait_user_challenge
        UNIQUE (user_id, challenge_id);