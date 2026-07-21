ALTER TABLE notification_type
    MODIFY COLUMN type_name ENUM(
    'CHALLENGE_START',
    'CHALLENGE_UPDATED',
    'CHALLENGE_VACANCY',
    'CHALLENGE_EXTENSION',
    'CHALLENGE_EXTENSION_SUCCESS',
    'CHALLENGE_EXTENSION_CANCEL',
    'VERIFICATION_DEADLINE_3H',
    'VERIFICATION_DEADLINE_1H',
    'VERIFICATION_DEADLINE_NOW'
    ) NOT NULL;

INSERT INTO notification_type (type_name, default_enabled, is_mandatory, created_at)
VALUES
    ('CHALLENGE_START', 1, 0, NOW()),
    ('CHALLENGE_UPDATED', 1, 0, NOW()),
    ('CHALLENGE_VACANCY', 1, 0, NOW())
    ON DUPLICATE KEY UPDATE
                         updated_at = NOW();

-- TODO:
-- 기존 PR(NotificationType 추가) 머지 후 develop 최신으로 pull 받아 NotificationType을 최종 반영 예정
-- 운영 DB 호환성을 위해 기존 NotificationType은 제거하지 않고 유지