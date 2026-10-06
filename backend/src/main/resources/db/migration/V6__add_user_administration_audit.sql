ALTER TABLE users
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE user_admin_audit (
    id UUID NOT NULL,
    actor_username VARCHAR(255) NOT NULL,
    target_username VARCHAR(255) NOT NULL,
    action VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_user_admin_audit PRIMARY KEY (id)
);

CREATE INDEX idx_user_admin_audit_target_created_at
    ON user_admin_audit (target_username, created_at);
