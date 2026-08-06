package com.bank.bankfinance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class FaceColumnMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FaceColumnMigrationRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        ensureColumn("t_user", "face_feature", "NVARCHAR(MAX) NULL");
        ensureColumn("t_user", "face_bound", "INT DEFAULT 0");
    }

    private void ensureColumn(String table, String column, String ddl) {
        try {
            List<String> columns = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?",
                    String.class, table, column);
            if (columns.isEmpty()) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD " + column + " " + ddl);
                log.info("已为 {} 增加列 {}", table, column);
            }
        } catch (Exception e) {
            log.warn("检查/新增列失败 {}:{} - {}", table, column, e.getMessage());
        }
    }
}
