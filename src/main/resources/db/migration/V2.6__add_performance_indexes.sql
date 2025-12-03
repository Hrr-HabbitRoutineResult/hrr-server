-- findWeeklyVerifications 쿼리 성능 최적화를 위한 인덱스 추가

-- Verification 테이블 검색 최적화
-- 날짜 범위 검색(BETWEEN) 속도 향상
CREATE INDEX idx_verification_created_at ON verification (created_at);

-- 상태값(COMPLETED 등) 필터링 속도 향상
CREATE INDEX idx_verification_status ON verification (status);

-- UserChallenge 테이블 조인(Join) 및 검색 최적화
-- 챌린지 ID로 검색할 때 속도 향상 (기존 복합키는 user_id가 앞이라 challenge_id 검색엔 비효율적일 수 있음)
CREATE INDEX idx_user_challenge_challenge_id ON user_challenge (challenge_id);

-- 유저 ID로 검색할 때 속도 향상
CREATE INDEX idx_user_challenge_user_id ON user_challenge (user_id);