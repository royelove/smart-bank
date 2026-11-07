package com.bank.bankfinance.service;

import com.bank.bankfinance.dto.trade.*;
import com.bank.bankfinance.dto.user.*;
import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.vo.AccountVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


public interface AccountService {
    void deposit(DepositDTO depositdto);
    void transfer(TransferDTO transferDTO);
    AccountVO getAccountVO(String accountNumber);
    void withdraw(WithdrawDTO withdrawDTO);
    void register(RegisterDTO registerDTO);
    AccountVO login(LoginDTO loginDTO);
    void updatePsd(UpdatePsdDTO updatePsdDTO);
    Account getAccountEntity(String accountNumber);
    BigDecimal getAccountBalance(String accountNumber);
    /** 买入委托冻结资金，返回 false 表示可用资金不足 */
    boolean freezeBuyFunds(String accountNumber, BigDecimal amount);
    /**
     * 撤单释放冻结资金，返回 false 表示释放额超过实际冻结额（记账已不一致，调用方必须回滚）。
     */
    boolean releaseBuyFunds(String accountNumber, BigDecimal amount);
    /**
     * 买入成交结算：释放委托价冻结并扣减实际成交额。
     *
     * <p>修复记录：原返回 {@code void}，于是「钱没扣成但持仓已经加上去了」这种最危险的不一致
     * 无法被调用方发现（详见 {@code AccountMapper.settleBuy}）。现返回 false 表示结算未生效，
     * 调用方必须抛异常回滚整个撮合事务。
     */
    boolean settleBuyFunds(String accountNumber, BigDecimal freezeAmount, BigDecimal actualAmount);
    /** 入账（卖出成交等），同时清理账户缓存 */
    void creditBalance(String accountNumber, BigDecimal amount);
    /**
     * 清理账户缓存。
     *
     * <p>新增原因：理财购买/赎回也是直接改 {@code t_account} 的余额（不走本类的方法），
     * 但原来没有任何地方清缓存，于是「买完理财余额还是买之前的数」，最长 30 分钟才自愈。
     * 事务中调用时该清理会被推迟到事务完成之后执行，避免并发读把提交前的旧值重新写回缓存。
     */
    void evictAccountCache(String accountNumber);
    void sendVerificationCode(SendCodeDTO sendCodeDTO);
    void resetPassword(ResetPasswordDTO resetPasswordDTO);
    void updateProfile(UpdateProfileDTO updateProfileDTO);
}
