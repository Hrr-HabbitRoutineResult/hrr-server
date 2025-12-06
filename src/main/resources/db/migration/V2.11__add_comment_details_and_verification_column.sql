DROP PROCEDURE IF EXISTS `Migrate_V2_10`;

DELIMITER $$

CREATE PROCEDURE `Migrate_V2_10`()
BEGIN
    -- 1. comment 테이블 컬럼 추가 (verification_id가 없을 때만 전체 실행)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'comment'
        AND COLUMN_NAME = 'verification_id'
    ) THEN
ALTER TABLE comment
    ADD COLUMN verification_id BIGINT NOT NULL AFTER id,
            ADD COLUMN user_id BIGINT NOT NULL AFTER verification_id,
            ADD COLUMN content TEXT NULL AFTER user_id,
            ADD COLUMN parent_id BIGINT NULL AFTER content,
            ADD COLUMN depth INT NOT NULL DEFAULT 0 AFTER parent_id,
            ADD COLUMN is_anonymous BOOLEAN NOT NULL DEFAULT FALSE AFTER depth,
            ADD COLUMN likes_count INT NOT NULL DEFAULT 0 AFTER is_anonymous,
            ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE AFTER likes_count;
END IF;

    -- 2. 인덱스 추가: idx_comment_verification
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'comment'
        AND INDEX_NAME = 'idx_comment_verification'
    ) THEN
CREATE INDEX idx_comment_verification ON comment (verification_id);
END IF;

    -- 3. 인덱스 추가: idx_comment_parent
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'comment'
        AND INDEX_NAME = 'idx_comment_parent'
    ) THEN
CREATE INDEX idx_comment_parent ON comment (parent_id);
END IF;

    -- 4. FK 추가: fk_comment_verification
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'comment'
        AND CONSTRAINT_NAME = 'fk_comment_verification'
    ) THEN
ALTER TABLE comment
    ADD CONSTRAINT fk_comment_verification
        FOREIGN KEY (verification_id) REFERENCES verification(id)
            ON DELETE CASCADE;
END IF;

    -- 5. FK 추가: fk_comment_user
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'comment'
        AND CONSTRAINT_NAME = 'fk_comment_user'
    ) THEN
ALTER TABLE comment
    ADD CONSTRAINT fk_comment_user
        FOREIGN KEY (user_id) REFERENCES user(id)
            ON DELETE CASCADE;
END IF;

    -- 6. FK 추가: fk_comment_parent
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'comment'
        AND CONSTRAINT_NAME = 'fk_comment_parent'
    ) THEN
ALTER TABLE comment
    ADD CONSTRAINT fk_comment_parent
        FOREIGN KEY (parent_id) REFERENCES comment(id)
            ON DELETE CASCADE;
END IF;

    -- 7. verification 테이블 컬럼 추가: round_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'verification'
        AND COLUMN_NAME = 'round_id'
    ) THEN
ALTER TABLE verification ADD COLUMN round_id BIGINT;
END IF;

    -- 8. verification 테이블 컬럼 추가: user_challenge_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'verification'
        AND COLUMN_NAME = 'user_challenge_id'
    ) THEN
ALTER TABLE verification ADD COLUMN user_challenge_id BIGINT;
END IF;

END $$

DELIMITER ;

-- 프로시저 실행
CALL `Migrate_V2_10`();

-- 프로시저 삭제 (청소)
DROP PROCEDURE `Migrate_V2_10`;