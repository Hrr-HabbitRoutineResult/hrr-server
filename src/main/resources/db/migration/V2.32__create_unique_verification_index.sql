-- 1. 프로시저 생성 (인덱스 존재 여부 확인 후 생성 로직)
DELIMITER //

CREATE PROCEDURE CreateUniqueVerificationIndex()
BEGIN
    -- 'verification' 테이블에 'uk_user_challenge_date' 인덱스가 없는 경우에만 실행
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'verification'
          AND index_name = 'uk_user_challenge_date'
    ) THEN
        -- user_challenge_id와 생성 날짜(DATE)의 조합으로 유니크 인덱스 추가
        -- 이 제약 조건이 있어야 동시 요청 시 DB 레벨에서 차단이 가능합니다.
ALTER TABLE verification
    ADD UNIQUE INDEX uk_user_challenge_date (user_challenge_id, (CAST(created_at AS DATE)));
END IF;
END //

DELIMITER ;

-- 2. 프로시저 실행
CALL CreateUniqueVerificationIndex();

-- 3. 프로시저 삭제 (정리)
DROP PROCEDURE IF EXISTS CreateUniqueVerificationIndex;