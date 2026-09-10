CREATE TABLE auth_principals (
    id UUID PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    role VARCHAR(16) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    active BOOLEAN NOT NULL,
    token_hash VARCHAR(64) UNIQUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT active_principal_requires_token CHECK ((active AND token_hash IS NOT NULL) OR NOT active)
);

CREATE TABLE auth_administration_lock (
    id INTEGER PRIMARY KEY CHECK (id = 1)
);
INSERT INTO auth_administration_lock (id) VALUES (1);

ALTER TABLE financial_transactions ADD COLUMN owner_id UUID;
ALTER TABLE financial_transactions ADD CONSTRAINT fk_financial_owner
    FOREIGN KEY (owner_id) REFERENCES auth_principals(id);
CREATE INDEX ix_financial_owner_occurred ON financial_transactions(owner_id, occurred_at);

ALTER TABLE ai_interactions ADD COLUMN owner_id UUID;
ALTER TABLE ai_interactions ADD CONSTRAINT fk_ai_owner
    FOREIGN KEY (owner_id) REFERENCES auth_principals(id);
CREATE INDEX ix_ai_owner_created ON ai_interactions(owner_id, created_at);

