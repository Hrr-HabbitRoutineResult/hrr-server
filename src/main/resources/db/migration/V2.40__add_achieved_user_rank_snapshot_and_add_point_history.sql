-- 랭킹 동점자 처리 + point_history 중복 지급 방지용 UNIQUE 제약 추가
--
-- (주의) 아래 4개 DDL 변경사항은 모두 "이미 존재하면 건너뛰기" 방식으로 처리합니다.
--       MySQL은 DDL문을 즉시 커밋하기 때문에, 이전 시도가 중간에 실패해서 일부만 반영된 상태로
--       남아있어도 이 스크립트를 재실행하면 안전하게 이어서 완료됩니다.
--
-- 1) user_rank_snapshot.achieved_at : 랭킹 동점자 노출 순서(먼저 달성한 사람 우선) 정렬 기준
-- 2) point_history.challenge_scope_key : 챌린지 단위 타입(FIRST_VERIFICATION, CHALLENGE_MASTER)의
--    중복 지급 방지용 생성 컬럼. round_id가 채워진(라운드 단위) 행은 이 값이 NULL이 되어
--    같은 챌린지의 2라운드/3라운드 적립과 충돌하지 않는다.
-- 3) uk_point_history_challenge_scope : 챌린지 단위 중복 지급 방지 유니크 제약
-- 4) uk_point_history_round_scope : 라운드 단위(FLAWLESS_ROUND, WEEK1~3_PERFECT) 중복 지급 방지 유니크 제약

DELIMITER $$

DROP PROCEDURE IF EXISTS EnsureRankingAndPointHistorySchema$$

CREATE PROCEDURE EnsureRankingAndPointHistorySchema()
BEGIN
    -- 1. user_rank_snapshot.achieved_at 컬럼 추가
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_rank_snapshot'
          AND COLUMN_NAME = 'achieved_at'
    ) THEN
ALTER TABLE `user_rank_snapshot`
    ADD COLUMN `achieved_at` DATETIME(6) NULL AFTER `snapshot_date`;
END IF;

    -- 2. point_history.challenge_scope_key 생성 컬럼 추가
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'point_history'
          AND COLUMN_NAME = 'challenge_scope_key'
    ) THEN
ALTER TABLE `point_history`
    ADD COLUMN `challenge_scope_key` BIGINT
        GENERATED ALWAYS AS (CASE WHEN `round_id` IS NULL THEN `challenge_id` ELSE NULL END) STORED;
END IF;

    -- 3. 챌린지 단위 중복 지급 방지 유니크 제약
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'point_history'
          AND INDEX_NAME = 'uk_point_history_challenge_scope'
    ) THEN
ALTER TABLE `point_history`
    ADD CONSTRAINT `uk_point_history_challenge_scope`
        UNIQUE (`user_id`, `point_type`, `challenge_scope_key`);
END IF;

    -- 4. 라운드 단위 중복 지급 방지 유니크 제약
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'point_history'
          AND INDEX_NAME = 'uk_point_history_round_scope'
    ) THEN
ALTER TABLE `point_history`
    ADD CONSTRAINT `uk_point_history_round_scope`
        UNIQUE (`user_id`, `point_type`, `round_id`);

END IF;
END$$

CALL EnsureRankingAndPointHistorySchema()$$

DROP PROCEDURE EnsureRankingAndPointHistorySchema$$

    DELIMITER ;