-- Null 허용 상태로 컬럼 추가
ALTER TABLE notification_event ADD COLUMN created_date DATE;

-- 기존 데이터의 created_at에서 날짜만 추출하여 새 컬럼을 채우기
UPDATE notification_event SET created_date = DATE(created_at);

-- NotNull 제약 조건 추가
ALTER TABLE notification_event MODIFY COLUMN created_date DATE NOT NULL;

-- 복합 유니크 제약 조건 추가
ALTER TABLE notification_event ADD CONSTRAINT uk_notification_event_idempotency
    UNIQUE (context_type, context_id, type_id, created_date);