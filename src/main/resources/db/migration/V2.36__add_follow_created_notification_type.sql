-- TODO: 인증 알림(NotificationType) PR 머지 후
-- COMMENT_CREATED, QUESTION_VERIFICATION, WEAK_VERIFICATION_WARNING 추가 및 ENUM 갱신

ALTER TABLE notification_type
    MODIFY COLUMN type_name ENUM(
    'CHALLENGE_EXTENSION',
    'CHALLENGE_EXTENSION_SUCCESS',
    'CHALLENGE_EXTENSION_CANCEL',
    'VERIFICATION_DEADLINE_3H',
    'VERIFICATION_DEADLINE_1H',
    'VERIFICATION_DEADLINE_NOW',
    'FOLLOW_CREATED'
    ) NOT NULL;

INSERT INTO notification_type (type_name, default_enabled, is_mandatory, created_at)
VALUES
    ('FOLLOW_CREATED', 1, 0, NOW())
    ON DUPLICATE KEY UPDATE
                         updated_at = NOW();
