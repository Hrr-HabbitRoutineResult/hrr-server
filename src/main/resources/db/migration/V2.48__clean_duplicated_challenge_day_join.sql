-- 챌린지 인증 요일(challenge_day_join) 중복 데이터 정리
--
-- [배경]
-- 챌린지 수정(updateChallenge) 시 기존 요일을 Repository로 직접 삭제한 뒤 새 요일을 저장했으나,
-- Challenge.challengeDays에 cascade = ALL(PERSIST 포함)이 걸려 있고
-- 영속성 컨텍스트의 컬렉션에 기존 엔티티가 그대로 남아 있어 flush 시점에 예약된 DELETE가 취소됨.
-- 그 결과 기존 요일이 남은 채 새 요일이 INSERT 되어 요일이 중복 누적됨. (예: 월,월,수,수,금,금)
--
-- [정리 기준]
-- 단순 중복 제거만 하면 "월수금 -> 화목"처럼 요일 구성을 바꾼 수정이 반영되지 않는다.
-- 따라서 챌린지별로 "마지막으로 저장된 요일 세트"만 남겨야 한다.
--
-- 1단계 : 챌린지별로 마지막 저장 배치의 시작 지점을 찾아, 그 이전에 저장된 행을 삭제
--         한 번의 저장(saveAll)은 밀리초 단위로 끝나고 서로 다른 저장은 최소 수 분~수 일 차이가 나므로,
--         연속된 저장 시각 사이의 간격이 BATCH_GAP(5초)을 넘는 지점을 배치 경계로 판단한다.
--         (최신 시각에서 고정 시간을 빼는 방식은 저장이 오래 걸리면 정상 행을 지울 수 있어 사용하지 않음)
-- 2단계 : 남은 행에서 (challenge_id, day_of_week) 중복 제거 (가장 작은 id만 유지)
--         생성 시점에 프론트가 중복 요일을 보낸 경우를 대비한 방어 단계
--
-- [안전 장치]
-- - 저장 배치가 하나뿐인 챌린지는 배치 경계가 존재하지 않으므로 1단계 삭제 대상에서 완전히 제외된다.
-- - 각 챌린지는 최소 1개 이상의 요일 행이 반드시 남는다.
--   (1단계는 마지막 배치를 항상 보존하고, 2단계는 그룹당 MIN(id)를 보존)
--
-- [배포 전 사전 검증 쿼리]
--   삭제 예정 행 수 확인 (아래 1단계 DELETE를 SELECT로 바꿔서 실행)
--   SELECT COUNT(*) FROM challenge_day_join;   -- 정리 전 총 행 수 기록
--
-- [배포 후 사후 검증 쿼리]
--   -- (1) 중복이 남아 있으면 안 됨. 결과 0건이어야 정상
--   SELECT challenge_id, day_of_week, COUNT(*)
--   FROM challenge_day_join GROUP BY challenge_id, day_of_week HAVING COUNT(*) > 1;
--
--   -- (2) 요일이 0개가 된 챌린지가 있으면 안 됨. 결과 0건이어야 정상
--   SELECT c.id FROM challenge c
--   LEFT JOIN challenge_day_join cdj ON cdj.challenge_id = c.id
--   WHERE cdj.id IS NULL;

-- 1단계) 챌린지별 마지막 저장 배치의 시작 시각 이전에 저장된 요일 행 삭제
DELETE cdj
FROM challenge_day_join cdj
         JOIN (
    -- 간격이 5초를 초과하는 지점 중 가장 마지막 = 마지막 저장 배치의 시작 시각
    SELECT challenge_id, MAX(created_at) AS last_batch_started_at
    FROM (
             SELECT challenge_id,
                    created_at,
                    TIMESTAMPDIFF(
                            MICROSECOND,
                            LAG(created_at) OVER (PARTITION BY challenge_id ORDER BY created_at),
                            created_at
                    ) AS gap_microseconds
             FROM (SELECT DISTINCT challenge_id, created_at FROM challenge_day_join) distinct_saved_at
         ) gaps
    WHERE gap_microseconds > 5000000 -- BATCH_GAP: 5초
    GROUP BY challenge_id
) last_batch ON last_batch.challenge_id = cdj.challenge_id
WHERE cdj.created_at < last_batch.last_batch_started_at;

-- 2단계) 남은 행에서 (challenge_id, day_of_week) 중복 제거
DELETE cdj
FROM challenge_day_join cdj
         JOIN (
    SELECT challenge_id, day_of_week, MIN(id) AS keep_id
    FROM challenge_day_join
    GROUP BY challenge_id, day_of_week
) keeper ON keeper.challenge_id = cdj.challenge_id
    AND keeper.day_of_week = cdj.day_of_week
WHERE cdj.id > keeper.keep_id;