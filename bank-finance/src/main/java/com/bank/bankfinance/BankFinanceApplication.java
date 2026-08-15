package com.bank.bankfinance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootApplication
@MapperScan("com.bank.bankfinance.mapper")
@EnableScheduling
@EnableRabbit
public class BankFinanceApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(BankFinanceApplication.class, args);
    }
}