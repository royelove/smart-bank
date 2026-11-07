-- 股票订单簿与撮合成交表

-- 1. 订单簿（挂单队列）
CREATE TABLE IF NOT EXISTS t_order_book (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number     VARCHAR(32)   NOT NULL,
    order_no           VARCHAR(32)   NOT NULL,
    stock_code         VARCHAR(10)   NOT NULL,
    stock_name         VARCHAR(50)   NOT NULL,
    direction          VARCHAR(10)   NOT NULL COMMENT 'BUY/SELL',
    price              DECIMAL(12,3) NOT NULL COMMENT '委托价格',
    quantity           INT           NOT NULL COMMENT '委托数量',
    filled_quantity    INT           DEFAULT 0 COMMENT '已成交数量',
    remaining_quantity INT           NOT NULL COMMENT '未成交数量',
    status             INT           DEFAULT 0 COMMENT '0=待撮合 1=部分成交 2=全部成交 3=已撤单',
    create_time        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version            INT           DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_stock_status (stock_code, status, direction),
    INDEX idx_account (account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 撮合成交记录
CREATE TABLE IF NOT EXISTS t_stock_trade (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    trade_no       VARCHAR(32)   NOT NULL,
    stock_code     VARCHAR(10)   NOT NULL,
    stock_name     VARCHAR(50)   NOT NULL,
    buy_order_no   VARCHAR(32)   NOT NULL,
    sell_order_no  VARCHAR(32)   NOT NULL,
    buy_account    VARCHAR(32)   NOT NULL,
    sell_account   VARCHAR(32)   NOT NULL,
    trade_price    DECIMAL(12,3) NOT NULL,
    trade_quantity INT           NOT NULL,
    trade_amount   DECIMAL(18,2) NOT NULL,
    trade_time     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    create_time    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_trade_no (trade_no),
    INDEX idx_stock_time (stock_code, trade_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
