DELIMITER $$

DROP PROCEDURE IF EXISTS MigrateNotificationEvent$$

CREATE PROCEDURE MigrateNotificationEvent()
BEGIN
    -- 1. created_date 컬럼 추가 및 기본 데이터 세팅
    -- 컬럼이 없을 때만 추가
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_NAME = 'notification_event'
        AND COLUMN_NAME = 'created_date'
        AND TABLE_SCHEMA = DATABASE()
    ) THEN
ALTER TABLE notification_event ADD COLUMN created_date DATE;
END IF;

-- 컬럼이 존재하더라도 데이터가 NULL인 행이 있다면 created_at에서 추출하여 채움
UPDATE notification_event
SET created_date = DATE(created_at)
WHERE created_date IS NULL;

-- NOT NULL 제약 조건 적용
ALTER TABLE notification_event MODIFY COLUMN created_date DATE NOT NULL;


-- 2. notification_delivery 참조 관계 재배치
-- 중복된 이벤트들 중 대표 ID를 참조하도록 업데이트
UPDATE notification_delivery nd
    INNER JOIN notification_event old_e ON nd.event_id = old_e.id
    INNER JOIN (
    -- 중복 그룹별로 남길 최소 ID 선별
    SELECT MIN(id) as min_id, context_type, context_id, type_id, created_date
    FROM notification_event
    GROUP BY context_type, context_id, type_id, created_date
    ) representative ON
    old_e.context_type = representative.context_type AND
    old_e.context_id = representative.context_id AND
    old_e.type_id = representative.type_id AND
    old_e.created_date = representative.created_date
    SET nd.event_id = representative.min_id
WHERE nd.event_id != representative.min_id;


-- 3. 중복된 원본 이벤트 삭제
DELETE FROM notification_event
WHERE id NOT IN (
    SELECT min_id FROM (
                           SELECT MIN(id) AS min_id
                           FROM notification_event
                           GROUP BY context_type, context_id, type_id, created_date
                       ) AS temp
);


-- 4. 복합 유니크 제약 조건 추가
-- 인덱스가 없을 때만 생성하여 중복 실행 에러 방지
IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_NAME = 'notification_event'
        AND INDEX_NAME = 'uk_notification_event_idempotency'
        AND TABLE_SCHEMA = DATABASE()
    ) THEN
ALTER TABLE notification_event ADD CONSTRAINT uk_notification_event_idempotency
    UNIQUE (context_type, context_id, type_id, created_date);
END IF;

END $$

DELIMITER ;

-- 프로시저 실행
CALL MigrateNotificationEvent();

-- 사용 완료한 프로시저 삭제
DROP PROCEDURE IF EXISTS MigrateNotificationEvent;