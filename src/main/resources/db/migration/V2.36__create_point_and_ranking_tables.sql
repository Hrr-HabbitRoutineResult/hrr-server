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

-- 포인트 내역 조회(최신순, 기간 필터) 최적화 인덱스
CREATE INDEX `idx_point_history_user_created` ON `point_history` (`user_id`, `created_at`);


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

-- 특정 스냅샷일의 등수순 조회(상위 N명) 최적화 인덱스
CREATE INDEX `idx_snapshot_date_ranking` ON `user_rank_snapshot` (`snapshot_date`, `ranking`);