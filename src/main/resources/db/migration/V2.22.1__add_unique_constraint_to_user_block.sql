DELIMITER $$

DROP PROCEDURE IF EXISTS AddUniqueConstraintToUserBlock $$

CREATE PROCEDURE AddUniqueConstraintToUserBlock()
BEGIN
    -- 1. user_block 테이블에서 blocker_id와 blocked_id 조합의 유니크 인덱스가 없는 경우에만 실행
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_block'
          AND INDEX_NAME = 'uk_blocker_blocked'
    ) THEN
        -- 2. 유니크 제약 조건 추가 (인덱스 명: uk_blocker_blocked)
        ALTER TABLE user_block
            ADD CONSTRAINT uk_blocker_blocked UNIQUE (blocker_id, blocked_id);
    END IF;
END $$

DELIMITER ;

-- 프로시저 실행 후 삭제
CALL AddUniqueConstraintToUserBlock();
DROP PROCEDURE IF EXISTS AddUniqueConstraintToUserBlock;
