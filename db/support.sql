-- ============================================
-- 客服系统建表脚本 (SQL Server)
-- ============================================

IF OBJECT_ID('dbo.t_staff', 'U') IS NULL
BEGIN
    CREATE TABLE t_staff (
        id            BIGINT IDENTITY(1,1) PRIMARY KEY,
        username      NVARCHAR(50)  NOT NULL,
        password      NVARCHAR(100) NOT NULL,
        staff_name    NVARCHAR(50)  NULL,
        creation_date DATETIME2     DEFAULT GETDATE(),
        update_date   DATETIME2     DEFAULT GETDATE()
    );
END
GO

IF OBJECT_ID('dbo.t_support_ticket', 'U') IS NULL
BEGIN
    CREATE TABLE t_support_ticket (
        id            BIGINT IDENTITY(1,1) PRIMARY KEY,
        ticket_no     NVARCHAR(32)   NOT NULL,
        account_number NVARCHAR(32)  NULL,
        user_name     NVARCHAR(50)   NULL,
        user_phone    NVARCHAR(20)   NULL,
        category      NVARCHAR(20)   NOT NULL,
        title         NVARCHAR(100)  NOT NULL,
        description   NVARCHAR(1000) NULL,
        status        INT            NOT NULL DEFAULT 0,
        reply         NVARCHAR(1000) NULL,
        handler_id    BIGINT         NULL,
        handler_name  NVARCHAR(50)   NULL,
        sentiment     NVARCHAR(20)   NULL,
        ai_score      INT            NULL,
        ai_valid      INT            NULL DEFAULT 1,
        ai_eval       NVARCHAR(500)  NULL,
        creation_date DATETIME2      DEFAULT GETDATE(),
        update_date   DATETIME2      DEFAULT GETDATE(),
        close_date    DATETIME2      NULL
    );
END
GO

-- 初始客服账号（username=admin，明文密码为 password，存储为 BCrypt 加密值）
INSERT INTO t_staff (username, password, staff_name)
SELECT 'admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', '系统客服'
WHERE NOT EXISTS (SELECT 1 FROM t_staff WHERE username = 'admin');
GO
