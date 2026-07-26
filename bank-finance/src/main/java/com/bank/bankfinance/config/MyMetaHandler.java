package com.bank.bankfinance.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaHandler implements MetaObjectHandler {


    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "creationDate", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject,"updateDate",LocalDateTime.class,LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject,"updateDate",LocalDateTime.class,LocalDateTime.now());
    }
}
