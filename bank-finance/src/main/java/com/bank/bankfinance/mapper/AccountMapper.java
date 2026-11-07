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

    /**
     * 出账（提现 / 转账付款 / 理财产品扣款）。
     *
     * <p>修复记录：原方法只守卫 {@code account_balance >= amount}，<b>完全无视 frozen_balance</b>。
     * 于是「买入股票冻结资金 → 再提现/转出同一笔钱」可以成功，账户余额被扣到低于冻结额，
     * 结算买入时再扣一次就出现负数余额或结算失败。冻结资金必须只在可用资金内出账。
     */
    @Update("UPDATE t_account SET account_balance = account_balance - #{amount} " +
            "WHERE account_number = #{accountNumber} " +
            "AND account_balance - frozen_balance >= #{amount}")
    int deductAvailableBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    /** 买入委托冻结资金，以可用资金（余额-冻结）做守卫，返回 0 表示资金不足 */
    @Update("UPDATE t_account SET frozen_balance = frozen_balance + #{amount} " +
            "WHERE account_number = #{accountNumber} " +
            "AND account_balance - frozen_balance >= #{amount}")
    int freezeBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    /**
     * 撤单释放冻结资金。
     *
     * <p>修复记录：原为 {@code GREATEST(frozen_balance - #{amount}, 0)}。GREATEST 会把「释放金额
     * 超过实际冻结额」这一记账错误静默压成 0，既不报错也不留痕；一旦发生重复撤单或并发撤单，
     * 系统会把别人的冻结额当成自己的释放掉，账面看起来永远是「正常」的。现改为强守卫，
     * 释放额超过冻结额时返回 0 行，由调用方回滚事务并把问题暴露出来。
     */
    @Update("UPDATE t_account SET frozen_balance = frozen_balance - #{amount} " +
            "WHERE account_number = #{accountNumber} " +
            "AND frozen_balance >= #{amount}")
    int unfreezeBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    /**
     * 买入成交：按委托价释放冻结，按实际成交额扣减余额。
     *
     * <p>修复记录：原语句既没有 {@code frozen_balance >= freezeAmount} 也没有
     * {@code account_balance >= actualAmount} 守卫，还用 GREATEST 掩盖超额释放。返回值从未被检查，
     * 所以「持仓已经加上去了、钱却没扣成」这种最危险的不一致是静默发生的。现在两个守卫都给上，
     * 调用方必须检查返回行数。
     */
    @Update("UPDATE t_account SET frozen_balance = frozen_balance - #{freezeAmount}, " +
            "account_balance = account_balance - #{actualAmount} " +
            "WHERE account_number = #{accountNumber} " +
            "AND frozen_balance >= #{freezeAmount} " +
            "AND account_balance >= #{actualAmount}")
    int settleBuy(@Param("accountNumber") String accountNumber,
                  @Param("freezeAmount") BigDecimal freezeAmount,
                  @Param("actualAmount") BigDecimal actualAmount);
}
