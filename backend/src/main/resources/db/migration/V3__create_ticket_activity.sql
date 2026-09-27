CREATE TABLE ticket_activity (
    id UUID NOT NULL,
    ticket_id UUID NOT NULL,
    type VARCHAR(32) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_ticket_activity PRIMARY KEY (id),
    CONSTRAINT fk_ticket_activity_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id)
);

CREATE INDEX idx_ticket_activity_ticket_created
    ON ticket_activity (ticket_id, created_at, id);
