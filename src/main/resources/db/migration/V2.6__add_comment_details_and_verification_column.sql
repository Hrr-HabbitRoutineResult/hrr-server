ALTER TABLE comment
    ADD COLUMN verification_id BIGINT NOT NULL AFTER id,
    ADD COLUMN user_id BIGINT NOT NULL AFTER verification_id,
    ADD COLUMN content TEXT NULL AFTER user_id,
    ADD COLUMN parent_id BIGINT NULL AFTER content,
    ADD COLUMN depth INT NOT NULL DEFAULT 0 AFTER parent_id,
    ADD COLUMN is_anonymous BOOLEAN NOT NULL DEFAULT FALSE AFTER depth,
    ADD COLUMN likes_count INT NOT NULL DEFAULT 0 AFTER is_anonymous,
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE AFTER likes_count;

-- 부모 댓글 조회 및 인증글 조회 성능 개선용 인덱스
CREATE INDEX idx_comment_verification ON comment (verification_id);
CREATE INDEX idx_comment_parent ON comment (parent_id);

-- FK 설정
ALTER TABLE comment
    ADD CONSTRAINT fk_comment_verification
        FOREIGN KEY (verification_id) REFERENCES verification(id)
            ON DELETE CASCADE;

ALTER TABLE comment
    ADD CONSTRAINT fk_comment_user
        FOREIGN KEY (user_id) REFERENCES user(id)
            ON DELETE CASCADE;

ALTER TABLE comment
    ADD CONSTRAINT fk_comment_parent
        FOREIGN KEY (parent_id) REFERENCES comment(id)
            ON DELETE CASCADE;

ALTER TABLE verification ADD COLUMN round_id BIGINT;
ALTER TABLE verification ADD COLUMN user_challenge_id BIGINT;