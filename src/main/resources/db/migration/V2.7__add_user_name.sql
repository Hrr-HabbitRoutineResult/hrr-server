--    : user 테이블에 name 컬럼이 없는지 확인하고 추가
DELIMITER //

CREATE PROCEDURE add_name_column_if_not_exists()
BEGIN
    -- INFORMATION_SCHEMA를 조회하여 'user' 테이블에 'name' 컬럼이 있는지 확인
    IF NOT EXISTS(
        SELECT *
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()  -- 현재 DB 스키마
          AND TABLE_NAME = 'user'
          AND COLUMN_NAME = 'name'
    )
    THEN
        -- 컬럼이 존재하지 않을 때만 name 컬럼을 추가 (VARCHAR(20) NULL)
        ALTER TABLE `user`
            ADD COLUMN `name` VARCHAR(20) NULL;
    END IF;
END//

DELIMITER ;

-- 2. 생성한 임시 프로시저 실행
CALL add_name_column_if_not_exists();

-- 3. 임시 프로시저 삭제 (환경 정리)
DROP PROCEDURE add_name_column_if_not_exists;

-- nickname 컬럼의 속성을 NULL로 변경합니다.
ALTER TABLE `user`
    MODIFY COLUMN `nickname` VARCHAR(20) NULL;
