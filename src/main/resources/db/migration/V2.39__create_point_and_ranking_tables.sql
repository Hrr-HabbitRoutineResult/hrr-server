-- 포인트 적립 내역 테이블
CREATE TABLE IF NOT EXISTS `point_history` (
                                               `id` BIGINT NOT NULL AUTO_INCREMENT,
                                               `user_id` BIGINT NOT NULL,
                                               `point_type` VARCHAR(30) NOT NULL,
    `points` INT NOT NULL,
    `challenge_id` BIGINT NULL,
    `round_id` BIGINT NULL,
    `random_mission_id` BIGINT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_point_history_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_point_history_challenge` FOREIGN KEY (`challenge_id`) REFERENCES `challenge` (`id`),
    CONSTRAINT `fk_point_history_round` FOREIGN KEY (`round_id`) REFERENCES `round` (`id`),
    CONSTRAINT `fk_point_history_random_mission` FOREIGN KEY (`random_mission_id`) REFERENCES `random_mission` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 유저 랭킹 스냅샷 테이블
CREATE TABLE IF NOT EXISTS `user_rank_snapshot` (
                                                    `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                    `user_id` BIGINT NOT NULL,
                                                    `ranking` INT NOT NULL,
                                                    `points` BIGINT NOT NULL,
                                                    `total_user_count` INT NOT NULL,
                                                    `snapshot_date` DATE NOT NULL,
                                                    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_user_snapshot_date` UNIQUE (`user_id`, `snapshot_date`),
    CONSTRAINT `fk_user_rank_snapshot_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 이 마이그레이션이 이전에 중간까지 실행되다 실패한 이력이 있어도, 재실행 시 "이미 존재함" 에러 없이 안전하게 통과하도록 처리
DELIMITER $$

CREATE PROCEDURE EnsurePointRankingIndexExists()
BEGIN
    -- 포인트 내역 조회(최신순, 기간 필터) 최적화 인덱스
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'point_history'
          AND INDEX_NAME = 'idx_point_history_user_created'
    ) THEN
CREATE INDEX idx_point_history_user_created ON point_history (user_id, created_at);
END IF;

    -- 특정 스냅샷일의 등수순 조회(상위 N명) 최적화 인덱스
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_rank_snapshot'
          AND INDEX_NAME = 'idx_snapshot_date_ranking'
    ) THEN
CREATE INDEX idx_snapshot_date_ranking ON user_rank_snapshot (snapshot_date, ranking);
END IF;
END$$

CALL EnsurePointRankingIndexExists()$$

DROP PROCEDURE EnsurePointRankingIndexExists$$

    DELIMITER ;