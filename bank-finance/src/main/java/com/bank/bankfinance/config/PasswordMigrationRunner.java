package com.bank.bankfinance.config;

import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.entity.User;
import com.bank.bankfinance.mapper.AccountMapper;
import com.bank.bankfinance.mapper.UserMapper;
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
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            int accountCount = 0;
            List<Account> accounts = accountMapper.selectList(null);
            for (Account account : accounts) {
                String pwd = account.getPassword();
                if (pwd != null && !pwd.startsWith("$2a$")) {
                    account.setPassword(passwordEncoder.encode(pwd));
                    accountMapper.updateById(account);
                    accountCount++;
                }
            }
            if (accountCount > 0) {
                log.info("已迁移 {} 个账户密码为 BCrypt 加密", accountCount);
            }

            int userCount = 0;
            List<User> users = userMapper.selectList(null);
            for (User user : users) {
                String pwd = user.getPassword();
                if (pwd != null && !pwd.startsWith("$2a$")) {
                    user.setPassword(passwordEncoder.encode(pwd));
                    userMapper.updateById(user);
                    userCount++;
                }
            }
            if (userCount > 0) {
                log.info("已迁移 {} 个用户密码为 BCrypt 加密", userCount);
            }
        } catch (Exception ex) {
            log.warn("跳过密码迁移：数据库不可用或未初始化，启动将继续。原因: {}", ex.getMessage());
        }
    }
}
