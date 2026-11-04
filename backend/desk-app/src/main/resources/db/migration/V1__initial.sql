CREATE TABLE app_users (
 id VARCHAR(40) PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE,
 display_name VARCHAR(80) NOT NULL, password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(20) NOT NULL CHECK(role IN ('CUSTOMER','AGENT','ADMIN'))
);
CREATE TABLE products (
 id VARCHAR(40) PRIMARY KEY, name VARCHAR(120) NOT NULL, category VARCHAR(80) NOT NULL,
 price_cents BIGINT NOT NULL CHECK(price_cents >= 0), description VARCHAR(2000) NOT NULL, active BOOLEAN NOT NULL
);
CREATE TABLE shop_orders (
 id VARCHAR(40) PRIMARY KEY, order_no VARCHAR(40) NOT NULL UNIQUE,
 owner_id VARCHAR(40) NOT NULL REFERENCES app_users(id), product_id VARCHAR(40) NOT NULL REFERENCES products(id),
 quantity INTEGER NOT NULL CHECK(quantity>0), total_cents BIGINT NOT NULL,
 status VARCHAR(30) NOT NULL, created_at VARCHAR(40) NOT NULL, delivered_at VARCHAR(40)
);
CREATE TABLE tickets (
 id VARCHAR(40) PRIMARY KEY, ticket_no VARCHAR(30) NOT NULL UNIQUE,
 owner_id VARCHAR(40) NOT NULL REFERENCES app_users(id), order_id VARCHAR(40) REFERENCES shop_orders(id),
 kind VARCHAR(20) NOT NULL, subject VARCHAR(120) NOT NULL, description VARCHAR(2000) NOT NULL,
 status VARCHAR(30) NOT NULL, assignee_id VARCHAR(40) REFERENCES app_users(id),
 request_key VARCHAR(100) NOT NULL UNIQUE, request_hash VARCHAR(64) NOT NULL,
 version INTEGER NOT NULL DEFAULT 0, created_at VARCHAR(40) NOT NULL, updated_at VARCHAR(40) NOT NULL
);
CREATE INDEX tickets_owner_idx ON tickets(owner_id, created_at);
CREATE TABLE ticket_events (
 id VARCHAR(40) PRIMARY KEY, ticket_id VARCHAR(40) NOT NULL REFERENCES tickets(id),
 actor_id VARCHAR(40) NOT NULL REFERENCES app_users(id), event_type VARCHAR(30) NOT NULL,
 content VARCHAR(2000) NOT NULL, created_at VARCHAR(40) NOT NULL
);
CREATE TABLE knowledge_documents (
 id VARCHAR(40) PRIMARY KEY, title VARCHAR(120) NOT NULL, product_id VARCHAR(40) REFERENCES products(id),
 version INTEGER NOT NULL, active BOOLEAN NOT NULL, created_at VARCHAR(40) NOT NULL
);
CREATE TABLE knowledge_chunks (
 id VARCHAR(40) PRIMARY KEY, document_id VARCHAR(40) NOT NULL REFERENCES knowledge_documents(id),
 ordinal INTEGER NOT NULL, content VARCHAR(2000) NOT NULL, embedding TEXT, embedding_model VARCHAR(100),
 UNIQUE(document_id,ordinal)
);
CREATE TABLE chat_threads (
 id VARCHAR(40) PRIMARY KEY, owner_id VARCHAR(40) NOT NULL REFERENCES app_users(id),
 title VARCHAR(80) NOT NULL, created_at VARCHAR(40) NOT NULL
);
CREATE TABLE chat_messages (
 id VARCHAR(40) PRIMARY KEY, thread_id VARCHAR(40) NOT NULL REFERENCES chat_threads(id),
 role VARCHAR(20) NOT NULL, content VARCHAR(6000) NOT NULL, detail_json TEXT, created_at VARCHAR(40) NOT NULL
);
CREATE TABLE chat_runs (
 id VARCHAR(40) PRIMARY KEY, owner_id VARCHAR(40) NOT NULL REFERENCES app_users(id),
 thread_id VARCHAR(40) NOT NULL REFERENCES chat_threads(id), request_key VARCHAR(100) NOT NULL UNIQUE,
 question VARCHAR(1000) NOT NULL, state VARCHAR(30) NOT NULL, result_json TEXT,
 model_mode VARCHAR(40), latency_ms BIGINT, created_at VARCHAR(40) NOT NULL
);
CREATE TABLE proposals (
 id VARCHAR(40) PRIMARY KEY, owner_id VARCHAR(40) NOT NULL REFERENCES app_users(id),
 order_id VARCHAR(40) REFERENCES shop_orders(id), kind VARCHAR(20) NOT NULL,
 subject VARCHAR(120) NOT NULL, description VARCHAR(2000) NOT NULL,
 expires_at VARCHAR(40) NOT NULL, ticket_id VARCHAR(40) REFERENCES tickets(id), created_at VARCHAR(40) NOT NULL
);
CREATE TABLE audit_log (
 id VARCHAR(40) PRIMARY KEY, actor_id VARCHAR(40), action VARCHAR(60) NOT NULL,
 target_id VARCHAR(40), summary VARCHAR(500) NOT NULL, created_at VARCHAR(40) NOT NULL
);
