DELIMITER $$

DROP PROCEDURE IF EXISTS MigrateUserStatusAndConstraints $$

CREATE PROCEDURE MigrateUserStatusAndConstraints()
BEGIN
    -- 1. 기존 데이터 이관 (순서: DELETED -> INACTIVE)
    UPDATE user SET status = 'INACTIVE' WHERE status = 'DELETED';

    -- 2. 기존 데이터 이관 (순서: WITHDRAWN_COMPLETED -> DELETED)
    UPDATE user SET status = 'DELETED' WHERE status = 'WITHDRAWN_COMPLETED';

    -- 3. NULL 값 기본값으로 채우기
    UPDATE user SET status = 'ACTIVE' WHERE status IS NULL;

    -- 4. ENUM 값 정의 변경 및 NOT NULL/DEFAULT 설정
    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user'
          AND COLUMN_NAME = 'status'
    ) THEN
        -- DB 컬럼 타입을 ENUM으로 지정하여 허용되지 않은 값이 들어오는 것을 원천 차단
        ALTER TABLE user
            MODIFY COLUMN status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED')
                NOT NULL DEFAULT 'ACTIVE';
    END IF;

END $$

DELIMITER ;

-- 실행 및 정리
CALL MigrateUserStatusAndConstraints();
DROP PROCEDURE IF EXISTS MigrateUserStatusAndConstraints;
