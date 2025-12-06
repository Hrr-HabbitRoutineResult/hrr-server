-- 기존 유저의 login_status 값을 새로운 ENUM 값으로 업데이트
UPDATE `user`
SET `login_status` =
        CASE `login_status`
            WHEN 'EXISTING' THEN 'ACTIVE'

            -- 매핑되지 않은 값은 그대로 유지
            ELSE `login_status`
            END
-- 마이그레이션 대상인 NEW와 EXISTING만 처리하도록 명확히 지정
-- (만약 이미 새 값으로 마이그레이션된 유저가 있다면 제외)
WHERE `login_status` IN ('EXISTING', 'NEW');

-- User의 nickname unique 제약 추가(develop의 마이그레이션 파일 최신 버전이 2.5이므로 버전을 변경하는 과정에서 저장 프로시저를 활용해 멱등성 확보)
-- 구분자 변경 시작
DELIMITER $$

-- 제약조건 추가 로직을 담을 프로시저 생성
CREATE PROCEDURE AddUniqueConstraintIfNotExists_Nickname()
BEGIN
    -- 'user' 테이블에 'UQ_user_nickname' 제약조건이 존재하는지 확인
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()  -- 현재 DB 스키마
          AND TABLE_NAME = 'user'
          AND CONSTRAINT_TYPE = 'UNIQUE'
          AND CONSTRAINT_NAME = 'UQ_user_nickname'
    ) THEN
        -- 존재하지 않는 경우에만 제약조건 추가
        ALTER TABLE `user`
            ADD CONSTRAINT UQ_user_nickname
                UNIQUE (nickname);
    END IF;
END$$

-- 프로시저 실행
CALL AddUniqueConstraintIfNotExists_Nickname()$$

-- 마이그레이션 완료 후 프로시저 삭제 (깨끗하게 정리)
DROP PROCEDURE AddUniqueConstraintIfNotExists_Nickname$$

-- 구분자 복원
DELIMITER ;
