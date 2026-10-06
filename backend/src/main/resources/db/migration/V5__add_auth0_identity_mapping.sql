ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN auth0_subject VARCHAR(255);

CREATE UNIQUE INDEX uq_users_auth0_subject
    ON users (auth0_subject);
