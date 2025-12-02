-- 기존 유저의 login_status 값을 새로운 ENUM 값으로 업데이트

UPDATE `user`
SET `login_status` =
        CASE `login_status`
            WHEN 'EXISTING' THEN 'ACTIVE'

            -- 매핑되지 않은 값은 그대로 유지
            ELSE `login_status`
            END
-- 마이그레이션 대상인 NEW와 EXISTING만 처리하도록 명확히 지정
-- (만약 이미 새 값으로 마이그레이션된 유저가 있다면 제외)
WHERE `login_status` IN ('EXISTING', 'NEW');

-- User의 nickname unique 제약 추가
ALTER TABLE `user`
    ADD CONSTRAINT UQ_user_nickname
        UNIQUE (nickname);
