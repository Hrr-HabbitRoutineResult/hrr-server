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

END $$

DELIMITER ;

CALL FixSchemaMismatch();

DROP PROCEDURE FixSchemaMismatch;