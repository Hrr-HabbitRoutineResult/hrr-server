-- Follow 테이블에 status 컬럼 추가 (없을 경우에만)
DELIMITER //

CREATE PROCEDURE add_follow_status_if_not_exists()
BEGIN
    -- INFORMATION_SCHEMA를 조회하여 'follow' 테이블에 'status' 컬럼이 있는지 확인
    IF NOT EXISTS(
        SELECT *
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()  -- 현재 DB 스키마
          AND TABLE_NAME = 'follow'
          AND COLUMN_NAME = 'status'
    )
    THEN
        -- 컬럼이 존재하지 않을 때만 status 컬럼을 추가 (기존 데이터는 APPROVED로)
        ALTER TABLE `follow`
            ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
    END IF;
END//

DELIMITER ;

-- 생성한 임시 프로시저 실행
CALL add_follow_status_if_not_exists();

-- 임시 프로시저 삭제 (환경 정리)
DROP PROCEDURE add_follow_status_if_not_exists;