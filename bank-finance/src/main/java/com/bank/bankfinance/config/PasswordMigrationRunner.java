package com.bank.bankfinance.config;

import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.mapper.AccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordMigrationRunner.class);

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        List<Account> accounts = accountMapper.selectList(null);
        int count = 0;
        for (Account account : accounts) {
            String pwd = account.getPassword();
            if (pwd != null && !pwd.startsWith("$2a$")) {
                account.setPassword(passwordEncoder.encode(pwd));
                accountMapper.updateById(account);
                count++;
            }
        }
        if (count > 0) {
            log.info("已迁移 {} 个账户密码为 BCrypt 加密", count);
        }
    }
}
