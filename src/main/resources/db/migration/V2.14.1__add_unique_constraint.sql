-- user_id 에 대한 일대일 관계 유니크 제약 조건 추가 (멱등성 보장)
DROP PROCEDURE IF EXISTS add_unique_constraint_if_not_exists;

DELIMITER $$

CREATE PROCEDURE add_unique_constraint_if_not_exists()
BEGIN
    -- 제약 조건이 존재하는지 확인
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'social_auth'
          AND CONSTRAINT_NAME = 'uk_social_auth_user_id'
    ) THEN
        -- 존재하지 않을 때만 제약 조건을 추가
        ALTER TABLE social_auth ADD CONSTRAINT uk_social_auth_user_id UNIQUE (user_id);
    END IF;
END $$

DELIMITER ;

-- 프로시저 실행 후 삭제
CALL add_unique_constraint_if_not_exists();
DROP PROCEDURE IF EXISTS add_unique_constraint_if_not_exists;
