CREATE TABLE verification_scrap
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    verification_id BIGINT       NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,

    CONSTRAINT uk_verification_scrap_user_verification
        UNIQUE (user_id, verification_id),

    CONSTRAINT fk_verification_scrap_user_id
        FOREIGN KEY (user_id)
            REFERENCES user (id),

    CONSTRAINT fk_verification_scrap_verification_id
        FOREIGN KEY (verification_id)
            REFERENCES verification (id)
            ON DELETE CASCADE
);