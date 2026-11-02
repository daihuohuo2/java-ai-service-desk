-- Run only against the isolated desk-fullstack-tests PostgreSQL service.
BEGIN;
CREATE TEMP TABLE learning_orders (
 id BIGINT PRIMARY KEY, owner_id VARCHAR(40), status VARCHAR(20), created_at BIGINT
) ON COMMIT DROP;
INSERT INTO learning_orders
 SELECT n, 'customer-' || (n % 1000), CASE WHEN n % 2=0 THEN 'DELIVERED' ELSE 'SHIPPED' END, n
 FROM generate_series(1,50000) n;
ANALYZE learning_orders;
EXPLAIN (ANALYZE, BUFFERS)
 SELECT * FROM learning_orders WHERE owner_id='customer-500' AND status='DELIVERED'
 ORDER BY created_at DESC,id LIMIT 10;
CREATE INDEX learning_order_lookup ON learning_orders(owner_id,status,created_at DESC,id);
ANALYZE learning_orders;
EXPLAIN (ANALYZE, BUFFERS)
 SELECT * FROM learning_orders WHERE owner_id='customer-500' AND status='DELIVERED'
 ORDER BY created_at DESC,id LIMIT 10;
ROLLBACK;
