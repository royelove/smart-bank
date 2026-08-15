package com.bank.bankfinance.mapper;

import com.bank.bankfinance.entity.Account;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
    //Select注释表明下面这个函数不会走Mybatis，只执行括号里面的SQL语句
    @Select("SELECT * FROM t_account WHERE account_number = #{accountNumber} FOR UPDATE")
    Account selectForUpdate(String accountNumber);
    @Update("UPDATE t_account SET account_balance = account_balance + #{amount} " +
            "WHERE account_number = #{accountNumber}")
    int addBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    @Update("UPDATE t_account SET account_balance = account_balance - #{amount} " +
            "WHERE account_number = #{accountNumber} AND account_balance >= #{amount}")
    int deductBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);
}
