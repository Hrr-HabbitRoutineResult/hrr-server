DROP PROCEDURE IF EXISTS `AddVersionColumnIfNotExist`;

DELIMITER $$

CREATE PROCEDURE `AddVersionColumnIfNotExist`()
BEGIN
    -- User 테이블에 version 컬럼 추가 (Optimistic Locking용)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'user'
        AND COLUMN_NAME = 'version'
    ) THEN
        ALTER TABLE user ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER id;
    END IF;

    -- 기존 데이터에 version 초기값 설정 (혹시 NULL인 경우 대비)
    UPDATE user SET version = 0 WHERE version IS NULL;

END $$

DELIMITER ;

-- 프로시저 실행
CALL `AddVersionColumnIfNotExist`();

-- 프로시저 삭제
DROP PROCEDURE `AddVersionColumnIfNotExist`;