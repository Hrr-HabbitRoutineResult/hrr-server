ALTER TABLE notification_type
    MODIFY COLUMN type_name ENUM(
    'CHALLENGE_EXTENSION',
    'CHALLENGE_EXTENSION_SUCCESS',
    'CHALLENGE_EXTENSION_CANCEL',
    'VERIFICATION_DEADLINE_3H',
    'VERIFICATION_DEADLINE_1H',
    'VERIFICATION_DEADLINE_NOW'
    ) NOT NULL;

INSERT INTO notification_type (type_name, default_enabled, is_mandatory, created_at)
VALUES
    ('VERIFICATION_DEADLINE_3H',  1, 0, NOW()),
    ('VERIFICATION_DEADLINE_1H',  1, 0, NOW()),
    ('VERIFICATION_DEADLINE_NOW', 1, 0, NOW())
    ON DUPLICATE KEY UPDATE
                         updated_at = NOW();