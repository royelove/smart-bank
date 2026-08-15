-- ============================================
-- 智慧银行项目建表脚本 (MySQL 8.0)
-- 数据库: t_bank   端口: 3307
-- ============================================

CREATE DATABASE IF NOT EXISTS t_bank DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE t_bank;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_phone     VARCHAR(20)  NOT NULL,
    user_name      VARCHAR(50)  NULL,
    password       VARCHAR(100) NOT NULL,
    creation_date  DATETIME     DEFAULT NOW(),
    update_date    DATETIME     DEFAULT NOW(),
    id_card_no     VARCHAR(18)  NULL,
    face_feature   LONGTEXT     NULL,
    face_bound     INT          DEFAULT 0,
    realname_bound INT          DEFAULT 0,
    UNIQUE KEY uk_user_phone (user_phone)
) ENGINE=InnoDB;

-- 账户(银行卡)表
CREATE TABLE IF NOT EXISTS t_account (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number     VARCHAR(32)   NOT NULL,
    password           VARCHAR(100)  NOT NULL,
    account_holder_name VARCHAR(50)  NOT NULL,
    account_type       VARCHAR(20)   NULL,
    account_balance    DECIMAL(18,2) DEFAULT 0,
    frozen_balance     DECIMAL(18,2) DEFAULT 0,
    creation_date      DATETIME      DEFAULT NOW(),
    update_date        DATETIME      DEFAULT NOW(),
    account_status     INT           DEFAULT 0,
    user_phone_number  VARCHAR(20)   NULL,
    user_id_card       VARCHAR(18)   NULL,
    user_id            BIGINT        NULL,
    UNIQUE KEY uk_account_number (account_number)
) ENGINE=InnoDB;

-- 交易流水表
CREATE TABLE IF NOT EXISTS t_trade (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    trade_no     VARCHAR(32)   NOT NULL,
    from_account VARCHAR(32)   NULL,
    to_account   VARCHAR(32)   NULL,
    amount       DECIMAL(18,2) DEFAULT 0,
    trade_type   VARCHAR(20)   NOT NULL,
    trade_date   DATETIME      DEFAULT NOW(),
    update_date  DATETIME      DEFAULT NOW(),
    UNIQUE KEY uk_trade_no (trade_no)
) ENGINE=InnoDB;

-- 股票池表
CREATE TABLE IF NOT EXISTS t_stock (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code     VARCHAR(10)   NOT NULL,
    stock_name     VARCHAR(50)   NOT NULL,
    market         VARCHAR(5)    NULL,
    status         INT           DEFAULT 1,
    prev_close     DECIMAL(12,2) DEFAULT 0,
    current_price  DECIMAL(12,2) DEFAULT 0,
    change_percent DECIMAL(10,2) DEFAULT 0,
    creation_date  DATETIME      DEFAULT NOW(),
    update_date    DATETIME      DEFAULT NOW(),
    UNIQUE KEY uk_stock_code (stock_code)
) ENGINE=InnoDB;

-- 股票委托表
CREATE TABLE IF NOT EXISTS t_order (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no         VARCHAR(32)   NOT NULL,
    account_number   VARCHAR(32)   NOT NULL,
    stock_code       VARCHAR(10)   NOT NULL,
    stock_name       VARCHAR(50)   NOT NULL,
    direction        VARCHAR(10)   NOT NULL,
    price_type       VARCHAR(10)   NOT NULL,
    order_price      DECIMAL(12,3) NULL,
    order_quantity   INT           NOT NULL,
    frozen_amount    DECIMAL(18,2) DEFAULT 0,
    filled_amount    DECIMAL(18,2) DEFAULT 0,
    filled_quantity  INT           DEFAULT 0,
    status           INT           DEFAULT 0,
    creation_date    DATETIME      DEFAULT NOW(),
    update_date      DATETIME      DEFAULT NOW(),
    KEY ix_order_account (account_number, status)
) ENGINE=InnoDB;

-- 股票持仓表
CREATE TABLE IF NOT EXISTS t_position (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number   VARCHAR(32)   NOT NULL,
    stock_code       VARCHAR(10)   NOT NULL,
    stock_name       VARCHAR(50)   NOT NULL,
    quantity         INT           DEFAULT 0,
    avg_cost         DECIMAL(18,4) DEFAULT 0,
    creation_date    DATETIME      DEFAULT NOW(),
    update_date      DATETIME      DEFAULT NOW(),
    UNIQUE KEY uk_position (account_number, stock_code)
) ENGINE=InnoDB;

-- 理财产品表
CREATE TABLE IF NOT EXISTS t_financial_product (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code     VARCHAR(32)   NOT NULL,
    product_name     VARCHAR(100)  NOT NULL,
    annual_return_rate DECIMAL(10,4) DEFAULT 0,
    term_days        INT           NOT NULL,
    min_amount       DECIMAL(18,2) DEFAULT 0,
    total_amount     DECIMAL(18,2) DEFAULT 0,
    remaining_amount DECIMAL(18,2) DEFAULT 0,
    status           INT           DEFAULT 1,
    description      VARCHAR(1000) NULL,
    creation_date    DATETIME      DEFAULT NOW(),
    update_date      DATETIME      DEFAULT NOW(),
    UNIQUE KEY uk_product_code (product_code)
) ENGINE=InnoDB;

-- 理财持仓表
CREATE TABLE IF NOT EXISTS t_financial_holding (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number    VARCHAR(32)   NOT NULL,
    product_id        BIGINT        NOT NULL,
    product_name      VARCHAR(100)  NOT NULL,
    annual_return_rate DECIMAL(10,4) DEFAULT 0,
    term_days         INT           NOT NULL,
    invest_amount     DECIMAL(18,2) DEFAULT 0,
    status            INT           DEFAULT 0,
    purchase_date     DATETIME      NULL,
    redeem_date       DATETIME      NULL,
    creation_date     DATETIME      DEFAULT NOW(),
    update_date       DATETIME      DEFAULT NOW(),
    KEY ix_holding_account (account_number)
) ENGINE=InnoDB;

-- 客服人员表
CREATE TABLE IF NOT EXISTS t_staff (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password      VARCHAR(100) NOT NULL,
    staff_name    VARCHAR(50)  NULL,
    creation_date DATETIME     DEFAULT NOW(),
    update_date   DATETIME     DEFAULT NOW(),
    UNIQUE KEY uk_staff_username (username)
) ENGINE=InnoDB;

-- 客服工单表
CREATE TABLE IF NOT EXISTS t_support_ticket (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no      VARCHAR(32)    NOT NULL,
    account_number VARCHAR(32)    NULL,
    user_name      VARCHAR(50)    NULL,
    user_phone     VARCHAR(20)    NULL,
    category       VARCHAR(20)    NOT NULL,
    title          VARCHAR(100)   NOT NULL,
    description    VARCHAR(1000)  NULL,
    status         INT            DEFAULT 0,
    reply          VARCHAR(1000)  NULL,
    handler_id     BIGINT         NULL,
    handler_name   VARCHAR(50)    NULL,
    sentiment      VARCHAR(20)    NULL,
    ai_score       INT            NULL,
    ai_valid       INT            DEFAULT 1,
    ai_eval        VARCHAR(500)   NULL,
    creation_date  DATETIME       DEFAULT NOW(),
    update_date    DATETIME       DEFAULT NOW(),
    close_date     DATETIME       NULL,
    UNIQUE KEY uk_ticket_no (ticket_no)
) ENGINE=InnoDB;

-- 身份与人脸库表
CREATE TABLE IF NOT EXISTS t_id_card_lib (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_card       VARCHAR(18) NOT NULL,
    name          VARCHAR(50) NOT NULL,
    face_feature  LONGTEXT    NULL,
    creation_date DATETIME    NULL,
    update_date   DATETIME    NULL,
    UNIQUE KEY uk_id_card (id_card)
) ENGINE=InnoDB;

-- 初始客服账号（username=admin，明文密码为 password，存储为 BCrypt 加密值）
INSERT INTO t_staff (username, password, staff_name)
SELECT 'admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', '系统客服'
WHERE NOT EXISTS (SELECT 1 FROM t_staff WHERE username = 'admin');