DELIMITER $$

DROP PROCEDURE IF EXISTS AddUniqueConstraintToUserBlock $$

CREATE PROCEDURE AddUniqueConstraintToUserBlock()
BEGIN
    -- 1. 중복 데이터 먼저 제거
    -- 동일한 blocker_id, blocked_id 조합이 여러 개라면, 가장 오래된 것만 유지
    DELETE b1 FROM user_block b1
                       INNER JOIN user_block b2
    WHERE b1.id > b2.id
      AND b1.blocker_id = b2.blocker_id
      AND b1.blocked_id = b2.blocked_id;

    -- 2. user_block 테이블에서 blocker_id와 blocked_id 조합의 유니크 인덱스가 없는 경우에만 실행
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_block'
          AND INDEX_NAME = 'uk_blocker_blocked'
    ) THEN
        -- 3. 유니크 제약 조건 추가 (인덱스 명: uk_blocker_blocked)
        ALTER TABLE user_block
            ADD CONSTRAINT uk_blocker_blocked UNIQUE (blocker_id, blocked_id);
    END IF;
END $$

DELIMITER ;

-- 프로시저 실행 후 삭제
CALL AddUniqueConstraintToUserBlock();
DROP PROCEDURE IF EXISTS AddUniqueConstraintToUserBlock;
