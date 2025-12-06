DROP PROCEDURE IF EXISTS FixSchemaMismatch;

DELIMITER $$

CREATE PROCEDURE FixSchemaMismatch()
BEGIN
    -- 1. Verification 테이블: is_resolved 컬럼 추가 (존재하지 않을 경우에만)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'verification'
        AND COLUMN_NAME = 'is_resolved'
    ) THEN
ALTER TABLE verification ADD COLUMN is_resolved BIT(1) DEFAULT 0 NOT NULL;
END IF;

    -- 2. Comment 테이블: content 컬럼 추가
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'comment'
        AND COLUMN_NAME = 'content'
    ) THEN
ALTER TABLE comment ADD COLUMN content VARCHAR(255) NOT NULL;
END IF;

    -- 3. Comment 테이블: is_adopted 컬럼 추가
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'comment'
        AND COLUMN_NAME = 'is_adopted'
    ) THEN
ALTER TABLE comment ADD COLUMN is_adopted BIT(1) DEFAULT 0 NOT NULL;
END IF;

    -- 4. Comment 테이블: verification_id 컬럼 추가
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'comment'
        AND COLUMN_NAME = 'verification_id'
    ) THEN
ALTER TABLE comment ADD COLUMN verification_id BIGINT NOT NULL;
END IF;

    -- 5. Comment 테이블: 외래키(FK) 연결
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'comment'
        AND CONSTRAINT_NAME = 'FK_comment_verification'
    ) THEN
ALTER TABLE comment
    ADD CONSTRAINT FK_comment_verification
        FOREIGN KEY (verification_id) REFERENCES verification (id);
END IF;

END $$

DELIMITER ;

-- 프로시저 실행
CALL FixSchemaMismatch();

-- 프로시저 삭제 (뒷정리)
DROP PROCEDURE FixSchemaMismatch;