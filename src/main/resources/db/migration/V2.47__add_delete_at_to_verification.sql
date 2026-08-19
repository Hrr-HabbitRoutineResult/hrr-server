ALTER TABLE verification
    ADD COLUMN deleted_at DATETIME(6) NULL COMMENT '인증글 삭제 시각 (soft delete). NULL이면 삭제되지 않은 상태';

CREATE INDEX idx_verification_round_record_status
    ON verification (round_record_id, status);