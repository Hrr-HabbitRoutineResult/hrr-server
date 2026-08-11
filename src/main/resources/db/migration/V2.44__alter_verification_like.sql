ALTER TABLE verification_like
    ADD COLUMN user_id BIGINT NOT NULL,
    ADD COLUMN verification_id BIGINT NOT NULL;

ALTER TABLE verification_like
    ADD CONSTRAINT uk_verification_like_user_verification
        UNIQUE (user_id, verification_id);

ALTER TABLE verification_like
    ADD CONSTRAINT fk_verification_like_user_id
        FOREIGN KEY (user_id)
            REFERENCES user (id);

ALTER TABLE verification_like
    ADD CONSTRAINT fk_verification_like_verification_id
        FOREIGN KEY (verification_id)
            REFERENCES verification (id)
            ON DELETE CASCADE;
