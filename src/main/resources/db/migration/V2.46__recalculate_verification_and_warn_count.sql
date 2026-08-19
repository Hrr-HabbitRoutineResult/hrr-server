UPDATE round_record rr
SET rr.verification_count = (
    SELECT COUNT(*)
    FROM verification v
    WHERE v.round_record_id = rr.id
      -- soft delete된 인증글은 집계에서 제외
      AND v.status <> 'DELETED'
);

UPDATE round_record rr
SET rr.warn_count = (
    SELECT FLOOR(COUNT(*) / 3)
    FROM weak_verification_report w
    WHERE w.round_record_id = rr.id
);