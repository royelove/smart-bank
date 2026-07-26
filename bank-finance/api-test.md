# 银行账户管理系统 - 接口测试文档

## 前置条件

1. 启动 Redis 服务（端口 6379）
2. 启动 SQL Server，执行建表 SQL
3. 启动项目

## 1. 注册开户

```http
POST /account/register
Content-Type: application/json

{
    "accountNumber": "10001",
    "accountHolderName": "张三",
    "password": "123456",
    "checkPassword": "123456",
    "accountType": "储蓄",
    "userPhoneNumber": "13800138000",
    "amount": 1000
}
```

## 2. 登录

```http
POST /account/login
Content-Type: application/json

{
    "accountNumber": "10001",
    "password": "123456"
}
```

**返回示例**：
```json
{
    "code": "000000",
    "msg": "操作成功",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiJ9...",
        "accountVO": {
            "accountNumber": "10001",
            "accountBalance": 1000,
            "accountHolderName": "张三",
            "accountType": "储蓄",
            "accountStatus": 1,
            "role": "1"
        }
    }
}
```

> 后续请求需在 Header 中添加 `Authorization: Bearer <token>`

## 3. 存款

```http
POST /account/deposit
Content-Type: application/json
Authorization: Bearer <token>

{
    "accountNo": "10001",
    "password": "123456",
    "amount": 500
}
```

## 4. 取款

```http
POST /account/withdraw
Content-Type: application/json
Authorization: Bearer <token>

{
    "accountNumber": "10001",
    "password": "123456",
    "amount": 200
}
```

## 5. 转账

```http
POST /account/transfer
Content-Type: application/json
Authorization: Bearer <token>

{
    "inNumber": "10002",
    "outNumber": "10001",
    "password": "123456",
    "amount": 300
}
```

## 6. 查询账户

```http
GET /account/getAccountEntity?accountNumber=10001
Authorization: Bearer <token>
```

## 7. 查询余额

```http
GET /account/getAccountBalance?accountNumber=10001
Authorization: Bearer <token>
```

不传 `accountNumber` 则查当前登录用户余额：
```http
GET /account/getAccountBalance
Authorization: Bearer <token>
```

## 8. 修改密码

```http
POST /account/updatePsw
Content-Type: application/json
Authorization: Bearer <token>

{
    "accountNumber": "10001",
    "newPassword": "654321",
    "confirmPassword": "654321"
}
```

## 9. 登出

```http
POST /account/logout
Authorization: Bearer <token>
```

登出后再用旧 token 调用其他接口会返回 401。

## 10. 管理员 - 冻结账户

```http
POST /account/admin/freezeAccount?AccountNumber=10002
Authorization: Bearer <token>
```

## 11. 管理员 - 解冻账户

```http
POST /account/admin/unfreezeAccount?AccountNumber=10002
Authorization: Bearer <token>
```

## 12. 交易流水查询

```http
GET /account/trades?pageNum=1&pageSize=10&accountNumber=10001
Authorization: Bearer <token>
```

## 13. Swagger 文档

浏览器打开：http://localhost:8080/doc.html

---

## 测试顺序建议

```
注册 10001 → 注册 10002 → 登录 10001 → 存款 → 取款 → 转账 → 查余额 → 
登出 → 用旧 token 测试被拒 → 重新登录 → 改密码 → 管理员冻结/解冻 → 查流水
```
