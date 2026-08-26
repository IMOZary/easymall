# 数据库设计与索引验证

## 版本管理

项目不再依赖 Hibernate 自动修改表结构。Flyway 在应用启动时按顺序执行：

- `V1__create_schema.sql`：创建用户、分类、商品、购物车、优惠券、订单和订单项表。
- `V2__add_order_expiry_and_indexes.sql`：增加订单支付过期时间和业务查询索引。

Hibernate 使用 `ddl-auto: validate`，实体与数据库结构不一致时直接拒绝启动。迁移已分别在 H2 2.3 和 MySQL 8.0.34 空库上验证通过。

## 重点索引

| 索引 | 对应场景 |
| --- | --- |
| `uk_order_user_idempotency(user_id, idempotency_key)` | 用户重复提交订单的数据库兜底 |
| `idx_shop_order_user_created(user_id, created_at)` | 用户订单列表按时间倒序分页 |
| `idx_shop_order_status_expires(status, expires_at)` | 批量扫描超时待支付订单 |
| `idx_product_category_status(category_id, status)` | 分类下已上架商品筛选 |
| `idx_cart_item_user_updated(user_id, updated_at)` | 用户购物车按更新时间查询 |

## MySQL 8.0.34 EXPLAIN ANALYZE 摘要

在完成 100 并发下单后执行 `scripts/mysql-explain.sql`：

- 用户订单列表使用 `idx_shop_order_user_created` 反向索引查找，实测约 `0.014 ms` 返回 1 行。
- 超时订单扫描使用 `idx_shop_order_status_expires` 范围扫描，实测约 `0.014 ms`（当时无过期数据）。
- 分类商品查询使用 `idx_product_category_status` 定位 5 行，再在小结果集内按销量排序，实测约 `0.054 ms`。

耗时只用于说明本次执行计划，不能代表大数据量下的固定性能。可通过以下命令复查：

```bash
mysql -u easymall -p easymall < scripts/mysql-explain.sql
```
