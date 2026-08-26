# EasyMall API 速查

所有接口统一返回：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {},
  "timestamp": "2026-08-19T17:00:00"
}
```

## 认证

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 公开 | 注册并自动登录 |
| POST | `/api/auth/login` | 公开 | 登录并创建 Session |
| GET | `/api/auth/me` | 登录 | 当前用户 |
| POST | `/api/auth/logout` | 公开 | 退出登录 |

## 商品与优惠券

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/categories` | 公开 | 分类列表 |
| GET | `/api/products` | 公开 | 商品分页；支持 `categoryId`、`keyword`、`page`、`size`、`sort` |
| GET | `/api/products/{id}` | 公开 | 商品详情 |
| GET | `/api/coupons` | 公开 | 当前可用优惠券 |

`sort` 可选值：`newest`、`sales`、`priceAsc`、`priceDesc`。

## 购物车

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/cart` | 登录 | 获取购物车 |
| POST | `/api/cart` | 登录 | 加入商品，参数 `productId`、`quantity` |
| PUT | `/api/cart/{itemId}` | 登录 | 修改数量 |
| DELETE | `/api/cart/{itemId}` | 登录 | 移除购物项 |

## 订单

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/orders` | 登录 | 购物车结算，必须传唯一 `idempotencyKey` |
| GET | `/api/orders` | 登录 | 我的订单分页 |
| GET | `/api/orders/{id}` | 登录 | 我的订单详情 |
| POST | `/api/orders/{id}/pay` | 登录 | 模拟支付 |
| POST | `/api/orders/{id}/cancel` | 登录 | 取消并回补库存 |
| POST | `/api/orders/{id}/complete` | 登录 | 确认收货 |

结算请求示例：

```json
{
  "receiver": "张同学",
  "phone": "13800138000",
  "address": "上海市浦东新区测试路1号",
  "remark": "周末送达",
  "couponCode": "NEW20",
  "idempotencyKey": "6cd58d77-a769-4d62-9fa4-94a86e6b9972"
}
```

## 管理后台

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/admin/dashboard` | 管理员 | 用户、商品、订单、销量与成交额 |
| POST | `/api/admin/products` | 管理员 | 新增商品 |
| PUT | `/api/admin/products/{id}` | 管理员 | 修改商品 |
| GET | `/api/admin/orders` | 管理员 | 全部订单分页 |
| POST | `/api/admin/orders/{id}/ship` | 管理员 | 已支付订单发货 |
