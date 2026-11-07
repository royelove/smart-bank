package com.bank.bankfinance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 建/补 K 线表 t_stock_kline。
 *
 * <p>背景：{@code docs/sql/stock_rank.sql} 里虽然写了这张表的定义，但那份脚本
 * <b>缺少 {@code stock_name} 与 {@code create_time} 两列</b>，而
 * {@code StockKline} 实体上这两个字段都没有 {@code @TableField(exist = false)}，
 * 于是 MyBatis-Plus 生成的 INSERT 必然带这两列 —— 表建了也插不进去。
 * 这里用幂等 DDL 兜住，避免"脚本跑过但还是报未知列"。
 *
 * <p>同时补一个 {@code (stock_code, period, kline_time)} 唯一索引：
 * K 线是按「股票 + 周期 + 周期起点」定位的唯一一根柱子，
 * 并发补齐历史或定时刷新时靠它兜住重复插入（应用层另有分片锁）。
 */
@Component
@Order(3)
public class StockKlineMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StockKlineMigrationRunner.class);

    private static final String TABLE = "t_stock_kline";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        ensureTable();
        ensureColumn("data_source", "VARCHAR(16) DEFAULT NULL COMMENT '数据来源: trade/quote/simulated'");
        ensureUniqueIndex();
    }

    private void ensureTable() {
        try {
            if (tableExists()) {
                return;
            }
            jdbcTemplate.execute("""
                    CREATE TABLE t_stock_kline (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        stock_code VARCHAR(16) NOT NULL COMMENT '股票代码',
                        stock_name VARCHAR(64) DEFAULT NULL COMMENT '股票名称',
                        period VARCHAR(10) NOT NULL COMMENT '周期: 1min/5min/1day/1week/1month',
                        open_price DECIMAL(18,4) DEFAULT NULL COMMENT '开盘价',
                        close_price DECIMAL(18,4) DEFAULT NULL COMMENT '收盘价',
                        high_price DECIMAL(18,4) DEFAULT NULL COMMENT '最高价',
                        low_price DECIMAL(18,4) DEFAULT NULL COMMENT '最低价',
                        volume BIGINT DEFAULT NULL COMMENT '成交量(手)',
                        amount DECIMAL(18,2) DEFAULT NULL COMMENT '成交额(元)',
                        kline_time DATETIME NOT NULL COMMENT 'K线周期起始时间',
                        data_source VARCHAR(16) DEFAULT NULL COMMENT '数据来源: trade/quote/simulated',
                        create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                        KEY idx_stock_period (stock_code, period),
                        KEY idx_kline_time (kline_time)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股票K线'
                    """);
            log.info("已创建 K 线表 {}", TABLE);
        } catch (Exception e) {
            log.warn("创建 K 线表失败 {} - {}", TABLE, e.getMessage());
        }
    }

    private boolean tableExists() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?",
                String.class, TABLE);
        return !tables.isEmpty();
    }

    private void ensureColumn(String column, String ddl) {
        try {
            List<String> columns = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?",
                    String.class, TABLE, column);
            if (columns.isEmpty()) {
                jdbcTemplate.execute("ALTER TABLE " + TABLE + " ADD COLUMN " + column + " " + ddl);
                log.info("已为 {} 增加列 {}", TABLE, column);
            }
        } catch (Exception e) {
            log.warn("检查/新增列失败 {}:{} - {}", TABLE, column, e.getMessage());
        }
    }

    /**
     * 唯一索引只做「尽力而为」：历史库里可能已经存在重复柱（旧脚本没有唯一约束），
     * 此时建唯一索引会失败。失败只告警不阻断启动 —— 应用层按 (股票, 周期) 分片加锁，
     * 重复插入在单实例下已经被挡住，唯一索引只是第二道保险。
     */
    private void ensureUniqueIndex() {
        final String indexName = "uk_stock_period_time";
        try {
            List<String> indexes = jdbcTemplate.queryForList(
                    "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_NAME = ? AND INDEX_NAME = ?",
                    String.class, TABLE, indexName);
            if (!indexes.isEmpty()) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE " + TABLE
                    + " ADD UNIQUE KEY " + indexName + " (stock_code, period, kline_time)");
            log.info("已为 {} 创建唯一索引 {}", TABLE, indexName);
        } catch (Exception e) {
            log.warn("创建唯一索引失败（可能存在重复K线，请手工清理后重试）{}:{} - {}",
                    TABLE, indexName, e.getMessage());
        }
    }
}
