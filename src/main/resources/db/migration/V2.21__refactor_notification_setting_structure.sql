-- 외래 키 제약 조건 체크를 일시적으로 해제 (안전한 테이블 삭제를 위해)
SET FOREIGN_KEY_CHECKS = 0;

-- 2. 기존 설정 테이블 삭제 (구조가 행 단위에서 컬럼 단위로 바뀌므로 재생성)
DROP TABLE IF EXISTS `notification_setting`;

-- 외래 키 체크 다시 활성화
SET FOREIGN_KEY_CHECKS = 1;

-- 새로운 1:1 구조의 알림 설정 테이블 생성
CREATE TABLE `notification_setting` (
                                        `id` BIGINT NOT NULL AUTO_INCREMENT,
                                        `user_id` BIGINT NOT NULL,
                                        `is_challenge_enabled` TINYINT(1) NOT NULL DEFAULT 1,
                                        `is_verification_enabled` TINYINT(1) NOT NULL DEFAULT 1,
                                        `is_follow_enabled` TINYINT(1) NOT NULL DEFAULT 1,
                                        `is_badge_enabled` TINYINT(1) NOT NULL DEFAULT 1,
                                        `created_at` DATETIME(6) NOT NULL,
                                        `updated_at` DATETIME(6) NOT NULL,
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uk_notification_setting_user_id` (`user_id`), -- 1:1 관계 보장
                                        CONSTRAINT `fk_notification_setting_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기존 유저들에게 기본 설정값(전체 활성화) 일괄 삽입
INSERT INTO `notification_setting` (user_id, is_challenge_enabled, is_verification_enabled, is_follow_enabled, is_badge_enabled, created_at, updated_at)
SELECT u.id, 1, 1, 1, 1, NOW(6), NOW(6)
FROM `user` u
         LEFT JOIN `notification_setting` ns ON u.id = ns.user_id
WHERE ns.user_id IS NULL AND u.deleted_at IS NULL;