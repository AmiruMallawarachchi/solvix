CREATE TABLE tickets (
    id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    assignee VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_tickets PRIMARY KEY (id)
);

CREATE TABLE ticket_comments (
    ticket_id UUID NOT NULL,
    comment VARCHAR(255) NOT NULL,
    CONSTRAINT fk_ticket_comments_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id)
);

CREATE INDEX idx_ticket_comments_ticket_id ON ticket_comments (ticket_id);
