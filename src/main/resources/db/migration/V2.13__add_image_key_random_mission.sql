-- random_mission 테이블에 image_key 컬럼 추가


DELIMITER //

CREATE PROCEDURE add_image_key_column_if_not_exists()
BEGIN
    -- INFORMATION_SCHEMA를 조회하여 'random_mission' 테이블에 'image_key' 컬럼이 있는지 확인
    IF NOT EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()  -- 현재 DB 스키마
          AND TABLE_NAME = 'random_mission'
          AND COLUMN_NAME = 'image_key'
    )
    THEN
        -- 컬럼이 존재하지 않을 때만 image_key 컬럼을 추가합니다.
        -- Java Entity에 따라 VARCHAR(255) NULL로 설정합니다.
        ALTER TABLE `random_mission`
            ADD COLUMN `image_key` VARCHAR(255) NULL;
    END IF;
END //

-- SQL 구문 구분자를 다시 기본값(;)으로 복원합니다.
DELIMITER ;

-- 생성한 임시 프로시저 실행
CALL add_image_key_column_if_not_exists();

-- 임시 프로시저 삭제 (환경 정리)
DROP PROCEDURE add_image_key_column_if_not_exists;
