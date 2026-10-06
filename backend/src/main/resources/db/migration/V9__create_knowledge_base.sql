CREATE TABLE kb_articles (
    id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    author VARCHAR(255) NOT NULL,
    category VARCHAR(64),
    view_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_kb_articles PRIMARY KEY (id)
);

CREATE TABLE kb_article_tags (
    article_id UUID NOT NULL,
    tag VARCHAR(64) NOT NULL,
    CONSTRAINT pk_kb_article_tags PRIMARY KEY (article_id, tag),
    CONSTRAINT fk_kb_article_tags_article FOREIGN KEY (article_id) REFERENCES kb_articles (id) ON DELETE CASCADE
);

CREATE TABLE ticket_kb_suggestions (
    ticket_id UUID NOT NULL,
    article_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_ticket_kb_suggestions PRIMARY KEY (ticket_id, article_id),
    CONSTRAINT fk_ticket_kb_suggestions_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_kb_suggestions_article FOREIGN KEY (article_id) REFERENCES kb_articles (id) ON DELETE CASCADE
);

CREATE INDEX idx_kb_articles_status ON kb_articles (status);
CREATE INDEX idx_kb_articles_category ON kb_articles (category);
CREATE INDEX idx_kb_articles_author ON kb_articles (author);
CREATE INDEX idx_kb_article_tags_tag ON kb_article_tags (tag);
