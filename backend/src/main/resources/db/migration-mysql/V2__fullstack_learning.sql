CREATE INDEX orders_owner_created_idx ON shop_orders(owner_id,created_at,id);
CREATE INDEX orders_owner_status_created_idx ON shop_orders(owner_id,status,created_at,id);
CREATE INDEX tickets_owner_status_updated_idx ON tickets(owner_id,status,updated_at,id);
CREATE TABLE catalog_revision (
 id INTEGER PRIMARY KEY, revision BIGINT NOT NULL
);
INSERT INTO catalog_revision(id,revision) VALUES(1,0);
CREATE TABLE index_jobs (
 id VARCHAR(40) PRIMARY KEY, document_id VARCHAR(40),
 actor_id VARCHAR(40) NOT NULL, request_key VARCHAR(120) NOT NULL UNIQUE,
 state VARCHAR(20) NOT NULL, attempts INTEGER NOT NULL DEFAULT 0,
 indexed INTEGER NOT NULL DEFAULT 0, total INTEGER NOT NULL DEFAULT 0,
 available_at BIGINT NOT NULL, lease_until BIGINT NOT NULL DEFAULT 0, lease_token VARCHAR(40),
 error_message VARCHAR(300), created_at VARCHAR(40) NOT NULL, updated_at VARCHAR(40) NOT NULL,
 FOREIGN KEY (document_id) REFERENCES knowledge_documents(id),
 FOREIGN KEY (actor_id) REFERENCES app_users(id)
);
CREATE INDEX index_jobs_dispatch_idx ON index_jobs(state,available_at,lease_until);
