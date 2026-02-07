DELIMITER //

CREATE PROCEDURE migrate_hrr_penalty_system()
BEGIN
    -- 1 . round_record 테이블에 version 컬럼 추가 (없을 경우에만)
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_NAME = 'round_record' AND COLUMN_NAME = 'version'
    ) THEN
        ALTER TABLE `round_record` ADD COLUMN `version` BIGINT DEFAULT 0;
    END IF;

    -- 2 . weak_verification_report (부실 인증 신고) 테이블 생성
    CREATE TABLE IF NOT EXISTS `weak_verification_report` (
                                                              `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                              `reporter_id` BIGINT NOT NULL,
                                                              `verification_id` BIGINT NOT NULL,
                                                              `round_record_id` BIGINT NOT NULL,
                                                              `created_at` TIMESTAMP(6) NOT NULL,
                                                              `updated_at` TIMESTAMP(6) NULL,
                                                              CONSTRAINT `uk_reporter_verification` UNIQUE (`reporter_id`, `verification_id`),
                                                              CONSTRAINT `fk_wvr_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`),
                                                              CONSTRAINT `fk_wvr_verification` FOREIGN KEY (`verification_id`) REFERENCES `verification` (`id`),
                                                              CONSTRAINT `fk_wvr_round_record` FOREIGN KEY (`round_record_id`) REFERENCES `round_record` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- 3 . verification_absence_log (미인증 로그) 테이블 생성
    CREATE TABLE IF NOT EXISTS `verification_absence_log` (
                                                              `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                              `round_record_id` BIGINT NOT NULL,
                                                              `absence_date` DATE NOT NULL,
                                                              `created_at` TIMESTAMP(6) NOT NULL,
                                                              `updated_at` TIMESTAMP(6) NULL,
                                                              CONSTRAINT `uk_round_record_date` UNIQUE (`round_record_id`, `absence_date`),
                                                              CONSTRAINT `fk_val_round_record` FOREIGN KEY (`round_record_id`) REFERENCES `round_record` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

END //

DELIMITER ;

-- 실행 및 삭제
CALL migrate_hrr_penalty_system();
DROP PROCEDURE IF EXISTS migrate_hrr_penalty_system;
