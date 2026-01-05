-- 1. 새로운 타입을 추가
ALTER TABLE notification_type
    MODIFY COLUMN type_name ENUM(
    'CHALLENGE_EXTENSION',
    'CHALLENGE_EXTENSION_SUCCESS',
    'CHALLENGE_EXTENSION_CANCEL'
    ) NOT NULL;

-- 2. 데이터 삽입 (이미 존재하는 경우 무시하거나 업데이트)
INSERT INTO notification_type (type_name, default_enabled, is_mandatory, created_at)
VALUES
    ('CHALLENGE_EXTENSION_SUCCESS', 1, 0, NOW()),
    ('CHALLENGE_EXTENSION_CANCEL', 1, 0, NOW())
    ON DUPLICATE KEY UPDATE
                         updated_at = NOW();