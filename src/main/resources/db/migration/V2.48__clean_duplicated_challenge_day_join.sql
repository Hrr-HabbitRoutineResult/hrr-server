-- 챌린지 인증 요일(challenge_day_join) 중복 데이터 정리
--
-- [배경]
-- 챌린지 수정(updateChallenge) 시 기존 요일을 Repository로 직접 삭제한 뒤 새 요일을 저장했으나,
-- Challenge.challengeDays에 cascade = ALL(PERSIST 포함)이 걸려 있고
-- 영속성 컨텍스트의 컬렉션에 기존 엔티티가 그대로 남아 있어 flush 시점에 예약된 DELETE가 취소됨.
-- 그 결과 기존 요일이 남은 채 새 요일이 INSERT 되어 요일이 중복 누적됨. (예: 월,월,수,수,금,금)
--
-- [정리 기준]
-- 1단계 : 챌린지별로 created_at이 가장 늦은 배치(= 마지막으로 저장된 요일 세트)만 남기고 이전 배치 삭제
--         한 번의 저장 배치 내에서 created_at이 마이크로초 단위로 미세하게 다를 수 있어 1초 여유값 적용
-- 2단계 : 남은 행에서 (challenge_id, day_of_week) 중복 제거 (가장 작은 id만 유지)
--
-- [주의]
-- 각 챌린지는 최소 1개 이상의 요일 행이 반드시 남도록 설계되어 있음
-- (1단계는 최신 배치를 항상 보존하고, 2단계는 그룹당 MIN(id)를 보존)

-- 1단계) 챌린지별 최신 배치보다 이전에 저장된 요일 행 삭제
DELETE cdj
FROM challenge_day_join cdj
         JOIN (
    SELECT challenge_id, MAX(created_at) AS last_created_at
    FROM challenge_day_join
    GROUP BY challenge_id
) latest ON latest.challenge_id = cdj.challenge_id
WHERE cdj.created_at < latest.last_created_at - INTERVAL 1 SECOND;

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