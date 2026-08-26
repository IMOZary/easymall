EXPLAIN ANALYZE
SELECT id, order_no, status, pay_amount, created_at
FROM shop_order
WHERE user_id = (SELECT MIN(user_id) FROM shop_order)
ORDER BY created_at DESC
LIMIT 10;

EXPLAIN ANALYZE
SELECT id, order_no, expires_at
FROM shop_order
WHERE status = 'PENDING_PAYMENT'
  AND expires_at <= NOW()
ORDER BY expires_at
LIMIT 100;

EXPLAIN ANALYZE
SELECT id, name, price, sales
FROM product
WHERE category_id = 1
  AND status = 'ON_SALE'
ORDER BY sales DESC
LIMIT 12;
