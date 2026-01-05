-- 1. 프로시저가 이미 존재할 경우 삭제
DROP PROCEDURE IF EXISTS AddColumnIfNotExist;

-- 2. 컬럼 추가 프로시저 생성
DELIMITER $$

CREATE PROCEDURE AddColumnIfNotExist()
BEGIN
    -- comment 테이블에 anonymous_number 컬럼이 없는 경우에만 실행
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'comment'
          AND COLUMN_NAME = 'anonymous_number'
    ) THEN
        -- 익명 번호 컬럼 추가 (Integer 타입, Null 허용)
        ALTER TABLE comment ADD COLUMN anonymous_number INT;

        -- 인덱스가 필요한 경우 추가 (선택 사항)
        -- CREATE INDEX idx_comment_anonymous_number ON comment (anonymous_number);
    END IF;
END $$

DELIMITER ;

-- 3. 프로시저 실행
CALL AddColumnIfNotExist();

-- 4. 사용한 프로시저 삭제 (DB를 깨끗하게 유지하기 위함)
DROP PROCEDURE IF EXISTS AddColumnIfNotExist;
