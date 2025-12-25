-- 알림 조회 성능 최적화를 위한 복합 인덱스 추가
CREATE INDEX idx_notification_delivery_receiver_created
    ON notification_delivery (receiver_id, created_at DESC);