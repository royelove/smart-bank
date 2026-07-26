package com.bank.bankfinance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootApplication
@MapperScan("com.bank.bankfinance.mapper")
public class BankFinanceApplication {

    public static void main(String[] args) {
        // 启动 Spring 应用，并获取容器上下文
        ConfigurableApplicationContext context = SpringApplication.run(BankFinanceApplication.class, args);

        // 测试数据库连接
        try {
            DataSource dataSource = context.getBean(DataSource.class);
            try (Connection connection = dataSource.getConnection()) {
                System.out.println("✅ 数据库连接成功！");
                System.out.println("   数据库 URL: " + connection.getMetaData().getURL());
                System.out.println("   数据库产品: " + connection.getMetaData().getDatabaseProductName());
                System.out.println("   数据库版本: " + connection.getMetaData().getDatabaseProductVersion());
            }
        } catch (Exception e) {
            System.err.println("❌ 数据库连接失败！");
            e.printStackTrace();
        }
    }
}