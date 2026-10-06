CREATE TABLE teams (
    id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_teams PRIMARY KEY (id),
    CONSTRAINT uk_teams_name UNIQUE (name)
);

CREATE TABLE team_memberships (
    id UUID NOT NULL,
    team_id UUID NOT NULL,
    username VARCHAR(255) NOT NULL,
    role VARCHAR(64) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_team_memberships PRIMARY KEY (id),
    CONSTRAINT fk_team_memberships_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT uk_team_member UNIQUE (team_id, username)
);

CREATE INDEX idx_team_memberships_username ON team_memberships (username);

ALTER TABLE tickets ADD COLUMN team_id UUID;
ALTER TABLE tickets ADD CONSTRAINT fk_tickets_team FOREIGN KEY (team_id) REFERENCES teams (id);
CREATE INDEX idx_tickets_team_id ON tickets (team_id);
