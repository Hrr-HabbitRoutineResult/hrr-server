DELIMITER $$

DROP PROCEDURE IF EXISTS EnsurePointHistoryVerificationColumn$$

CREATE PROCEDURE EnsurePointHistoryVerificationColumn()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'point_history'
          AND COLUMN_NAME = 'verification_id'
    ) THEN
ALTER TABLE `point_history`
    ADD COLUMN `verification_id` BIGINT NULL AFTER `random_mission_id`,
            ADD CONSTRAINT `fk_point_history_verification` FOREIGN KEY (`verification_id`) REFERENCES `verification` (`id`);
END IF;
END$$

CALL EnsurePointHistoryVerificationColumn()$$

DROP PROCEDURE EnsurePointHistoryVerificationColumn$$

    DELIMITER ;