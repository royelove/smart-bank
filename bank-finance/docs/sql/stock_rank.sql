-- 股票排行系统数据库脚本

-- 1. 扩展 t_stock 表，新增行情字段
ALTER TABLE t_stock
    ADD COLUMN high_price DECIMAL(18,4) DEFAULT NULL COMMENT '最高价',
    ADD COLUMN low_price DECIMAL(18,4) DEFAULT NULL COMMENT '最低价',
    ADD COLUMN volume BIGINT DEFAULT NULL COMMENT '成交量',
    ADD COLUMN amount DECIMAL(18,2) DEFAULT NULL COMMENT '成交额',
    ADD COLUMN turnover_rate DECIMAL(18,4) DEFAULT NULL COMMENT '换手率',
    ADD COLUMN total_shares DECIMAL(18,0) DEFAULT NULL COMMENT '总股本';

-- 2. 排行快照表
CREATE TABLE IF NOT EXISTS t_stock_rank (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(16) NOT NULL,
    stock_name VARCHAR(64),
    market VARCHAR(10),
    rank_type VARCHAR(20) NOT NULL COMMENT '排行类型: change/fall/volume/amount/turnover/amplitude',
    rank_value DECIMAL(18,4) COMMENT '排行指标值',
    prev_close DECIMAL(18,4) COMMENT '昨收',
    current_price DECIMAL(18,4) COMMENT '当前价',
    high_price DECIMAL(18,4) COMMENT '最高价',
    low_price DECIMAL(18,4) COMMENT '最低价',
    volume BIGINT COMMENT '成交量',
    amount DECIMAL(18,2) COMMENT '成交额',
    turnover_rate DECIMAL(18,4) COMMENT '换手率',
    `rank` INTEGER COMMENT '排名',
    snapshot_time DATETIME COMMENT '快照时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_stock_code (stock_code),
    INDEX idx_rank_type (rank_type),
    INDEX idx_snapshot_time (snapshot_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. K线数据表
CREATE TABLE IF NOT EXISTS t_stock_kline (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(16) NOT NULL,
    period VARCHAR(10) NOT NULL COMMENT '周期: 1min/5min/1day/1week/1month',
    open_price DECIMAL(18,4) COMMENT '开盘价',
    close_price DECIMAL(18,4) COMMENT '收盘价',
    high_price DECIMAL(18,4) COMMENT '最高价',
    low_price DECIMAL(18,4) COMMENT '最低价',
    volume BIGINT COMMENT '成交量',
    amount DECIMAL(18,2) COMMENT '成交额',
    kline_time DATETIME NOT NULL COMMENT 'K线时间',
    INDEX idx_stock_period (stock_code, period),
    INDEX idx_kline_time (kline_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Redis 排行榜 Key 说明
-- rank:change   ZSET  涨幅榜  score=涨跌幅%  member=stockCode
-- rank:fall     ZSET  跌幅榜  score=涨跌幅%  member=stockCode
-- rank:volume   ZSET  成交量榜 score=成交量   member=stockCode
-- rank:amount   ZSET  成交额榜 score=成交额   member=stockCode
-- rank:turnover ZSET  换手率榜 score=换手率%  member=stockCode
-- rank:amplitude ZSET 振幅榜 score=振幅%    member=stockCode

-- 5. 初始数据插入（示例）
INSERT INTO t_stock_rank (stock_code, stock_name, market, rank_type, rank_value, prev_close, current_price, `rank`, snapshot_time)
VALUES ('000001', '平安银行', '深', 'change', 2.30, 11.20, 11.46, 1, NOW());