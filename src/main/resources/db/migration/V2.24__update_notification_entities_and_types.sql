DELIMITER $$

-- 1. 기존 프로시저 삭제 (구분자 $$ 사용)
DROP PROCEDURE IF EXISTS MigrateNotificationEntities $$

-- 2. 프로시저 정의
CREATE PROCEDURE MigrateNotificationEntities()
BEGIN
    -- 1. notification_event 테이블: image_key 컬럼 추가
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'notification_event'
        AND COLUMN_NAME = 'image_key'
    ) THEN
ALTER TABLE notification_event ADD COLUMN image_key VARCHAR(255) AFTER message;
END IF;

    -- 2. notification_event 테이블: Enum 타입 확장
ALTER TABLE notification_event
    MODIFY COLUMN context_type ENUM('BADGE', 'CHALLENGE', 'COMMENT', 'USER', 'VERIFICATION', 'ROUND');
ALTER TABLE notification_event
    MODIFY COLUMN target_type ENUM('BADGE', 'CHALLENGE', 'COMMENT', 'USER', 'VERIFICATION', 'ROUND') NOT NULL;

-- 3. notification_type 테이블: 데이터 보정 및 타입 변경
UPDATE notification_type SET type_name = 'CHALLENGE_EXTENSION' WHERE type_name IS NOT NULL;

ALTER TABLE notification_type
    MODIFY COLUMN type_name ENUM('CHALLENGE_EXTENSION') NOT NULL;

END $$

-- 3. 구분자 원복
DELIMITER ;

-- 4. 프로시저 실행 및 삭제
CALL MigrateNotificationEntities();
DROP PROCEDURE MigrateNotificationEntities;