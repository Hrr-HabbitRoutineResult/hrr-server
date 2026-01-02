DELIMITER $$

DROP PROCEDURE IF EXISTS AddWithdrawnCompletedStatus $$

CREATE PROCEDURE AddWithdrawnCompletedStatus()
BEGIN
    -- user_status 컬럼이 ENUM 타입이고, WITHDRAWN_COMPLETED가 포함되어 있지 않은 경우에만 실행
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user'
          AND COLUMN_NAME = 'status'
          AND COLUMN_TYPE LIKE '%WITHDRAWN_COMPLETED%'
    ) THEN
        -- 기존 ENUM 정의에 WITHDRAWN_COMPLETED 추가
        ALTER TABLE user
            MODIFY COLUMN status ENUM('ACTIVE', 'DELETED', 'INACTIVE', 'SUSPENDED', 'WITHDRAWN_COMPLETED') DEFAULT NULL;
    END IF;
END $$

DELIMITER ;

-- 프로시저 실행 후 삭제
CALL AddWithdrawnCompletedStatus();
DROP PROCEDURE IF EXISTS AddWithdrawnCompletedStatus;
