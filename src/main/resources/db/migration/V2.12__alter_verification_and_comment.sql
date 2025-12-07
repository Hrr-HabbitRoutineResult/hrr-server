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

    -- 2. Comment 테이블: is_adopted 컬럼 추가 (존재하지 않을 경우에만)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'comment'
        AND COLUMN_NAME = 'is_adopted'
    ) THEN
ALTER TABLE comment ADD COLUMN is_adopted BIT(1) DEFAULT 0 NOT NULL;
END IF;

    -- 3. Comment 테이블: content 컬럼 NOT NULL로 변경
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'comment'
        AND COLUMN_NAME = 'content'
        AND IS_NULLABLE = 'YES'
    ) THEN
        -- 안전장치: NULL 데이터가 있다면 에러 방지를 위해 빈 문자열로 업데이트
UPDATE comment SET content = '' WHERE content IS NULL;

-- 컬럼 속성 변경 (NULL -> NOT NULL)
ALTER TABLE comment MODIFY COLUMN content TEXT NOT NULL;
END IF;

END $$

DELIMITER ;

CALL FixSchemaMismatch();

DROP PROCEDURE FixSchemaMismatch;