-- Follow 테이블에 status 컬럼 추가
ALTER TABLE follow
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' AFTER following_id;

-- 기존 데이터는 모두 APPROVED로 처리
UPDATE follow SET status = 'APPROVED';