ALTER TABLE tickets ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tickets ADD COLUMN category VARCHAR(64);

CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_created_by ON tickets (created_by);
CREATE INDEX idx_tickets_assignee ON tickets (assignee);
CREATE INDEX idx_tickets_created_at ON tickets (created_at DESC);
CREATE INDEX idx_tickets_updated_at ON tickets (updated_at DESC);
