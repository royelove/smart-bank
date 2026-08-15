package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.common.constant.ResponseCode;
import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.common.util.RedisUtil;
import com.bank.bankfinance.common.util.TicketNoUtil;
import com.bank.bankfinance.dto.trade.*;
import com.bank.bankfinance.dto.user.*;
import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.entity.Trade;
import com.bank.bankfinance.entity.User;
import com.bank.bankfinance.mapper.AccountMapper;
import com.bank.bankfinance.service.AccountService;
import com.bank.bankfinance.service.RiskAssessmentService;
import com.bank.bankfinance.service.TradeService;
import com.bank.bankfinance.service.UserService;
import com.bank.bankfinance.vo.AccountVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.bank.bankfinance.common.constant.ResponseCode.*;

@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService{

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);
    private static final String ACCOUNT_CACHE_PREFIX = "account:cache:";
    private static final long CACHE_EXPIRE_MINUTES = 30;
    private static final String VERIFICATION_CODE_PREFIX = "verify:code:";
    private static final long CODE_EXPIRE_MINUTES = 5;

    @Autowired
    private TradeService tradeService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserService userService;
    @Autowired
    private RiskAssessmentService riskAssessmentService;
    @Autowired
    private AccountMapper accountMapper;
    public Account getAccountEntity(String accountNumber){
        try {
            Account cached = redisUtil.get(ACCOUNT_CACHE_PREFIX + accountNumber);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败，降级查数据库: {}", e.getMessage());
        }
        Account account = this.lambdaQuery().eq(Account::getAccountNumber,accountNumber).one();
        if(account == null){
            throw new BusinessException(ResponseCode.BANK_ID_INVALID);
        }
        try {
            redisUtil.set(ACCOUNT_CACHE_PREFIX + accountNumber, account, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败: {}", e.getMessage());
        }
        return account;
    }

    private void evictAccountCache(String accountNumber) {
        try {
            redisUtil.delete(ACCOUNT_CACHE_PREFIX + accountNumber);
        } catch (Exception e) {
            log.warn("Redis 缓存删除失败: {}", e.getMessage());
        }
    }



    @Transactional
    @Override
    public void deposit(DepositDTO depositDTO) {
        Boolean first = redisUtil.setIfAbsent("idem:deposit:" + depositDTO.getRequestId(), "1", 10, TimeUnit.MINUTES);
        if(!Boolean.TRUE.equals(first)){
            throw new BusinessException(IDEMPOTENT_REJECTED);
        }
        try {
            String accountNumber = depositDTO.getAccountNo();
            Account account = getAccountEntity(accountNumber);
            String password = depositDTO.getPassword();
            if (!passwordEncoder.matches(password, account.getPassword())) {
                throw new BusinessException(PASSWORD_INVALID);
            }
            if (account.getAccountStatus() == 0) {
                throw new BusinessException(TRANSACTION_INVALID_FREEZE);
            }
            accountMapper.addBalance(accountNumber, depositDTO.getAmount());
            evictAccountCache(accountNumber);
            Trade trade = new Trade();
            trade.setTradeNo(TicketNoUtil.tradeNo());
            trade.setAmount(depositDTO.getAmount());
            trade.setToAccount(depositDTO.getAccountNo());
            trade.setFromAccount("");
            LocalDateTime now = LocalDateTime.now();
            trade.setTradeDate(now);
            trade.setUpdateDate(now);
            trade.setTradeType("存款");
            tradeService.saveTrade(trade);
        }catch (Exception e){
            redisUtil.delete("idem:deposit:" + depositDTO.getRequestId());
            throw e;
        }
    }

    @Override
    @Transactional
    public void transfer(TransferDTO transferDTO) {
        Boolean first = redisUtil.setIfAbsent("idem:transfer:" + transferDTO.getRequestId(), "1", 10, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(first)) {
            throw new BusinessException(IDEMPOTENT_REJECTED);
        }
        try {
            String inNumber = transferDTO.getInNumber();
            String outNumber = transferDTO.getOutNumber();
            String firstNo = inNumber.compareTo(outNumber) > 0 ? outNumber : inNumber;
            String secondNo = inNumber.compareTo(outNumber) < 0 ? outNumber : inNumber;
            //用锁，先锁住卡号小的，再锁住卡号大的，防止产生A->B,B->A
            Account firstAccount = accountMapper.selectForUpdate(firstNo);
            Account secondAccount = accountMapper.selectForUpdate(secondNo);
            if(firstAccount ==  null || secondAccount == null){
                throw new BusinessException(ResponseCode.BANK_ID_INVALID);
            }
            if (inNumber.equals(outNumber)) {
                throw new BusinessException(TRANSACTION_INVALID_SELF_TRANSFER);
            }
            Account outAccount;
            Account inAccount;
            if(firstAccount.getAccountNumber().equals(outNumber)){
                 outAccount =  firstAccount;
                 inAccount =  secondAccount;
            }else{
                outAccount =  secondAccount;
                inAccount =  firstAccount;
            }

            String password = transferDTO.getPassword();
            if (!passwordEncoder.matches(password, outAccount.getPassword())) {
                throw new BusinessException(PASSWORD_INVALID);
            }
            riskAssessmentService.assessAndBlock(outNumber, transferDTO.getAmount(), inNumber);
            if(outAccount.getAccountStatus()==0){
                throw new BusinessException(TRANSACTION_INVALID_FREEZE);
            }
            if(inAccount.getAccountStatus()==0){
                throw new BusinessException(TRANSACTION_INVALID_FREEZE_TRANSFER);
            }
            if(transferDTO.getAmount().compareTo(outAccount.getAccountBalance()) > 0){
                throw new BusinessException(TRANSACTION_INVALID_INSUFFICIENT);
            }
            inAccount.setAccountBalance(inAccount.getAccountBalance().add(transferDTO.getAmount()));
            outAccount.setAccountBalance(outAccount.getAccountBalance().subtract(transferDTO.getAmount()));
            this.updateById(outAccount);
            this.updateById(inAccount);
            evictAccountCache(inNumber);
            evictAccountCache(outNumber);
            Trade trade = new Trade();
            trade.setTradeNo(TicketNoUtil.tradeNo());
            trade.setFromAccount(outNumber);
            trade.setToAccount(inNumber);
            trade.setAmount(transferDTO.getAmount());
            trade.setTradeType("转账");
            LocalDateTime now = LocalDateTime.now();
            trade.setTradeDate(now);
            trade.setUpdateDate(now);
            tradeService.saveTrade(trade);
        } catch (Exception e) {
            redisUtil.delete("idem:transfer:" + transferDTO.getRequestId());
            throw e;
        }
    }

    @Override
    public AccountVO getAccountVO(String accountNumber) {
        Account account = getAccountEntity(accountNumber);
        AccountVO vo = new AccountVO();
        vo.setAccountNumber(account.getAccountNumber());
        vo.setAccountBalance(account.getAccountBalance());
        vo.setAccountStatus(account.getAccountStatus());
        vo.setAccountType(account.getAccountType());
        vo.setAccountHolderName(account.getAccountHolderName());
        vo.setUserPhoneNumber(account.getUserPhoneNumber());
        vo.setUserIdCard(account.getUserIdCard());
        vo.setUserId(account.getUserId());
        return vo;
    }

    @Transactional
    @Override
    public void withdraw(WithdrawDTO withdrawDTO) {
        String accountNumber = withdrawDTO.getAccountNumber();
        Account account = getAccountEntity(accountNumber);
        String password = withdrawDTO.getPassword();
        if(account.getAccountStatus()==0){
            throw new BusinessException(TRANSACTION_INVALID_FREEZE);
        }
        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw new BusinessException(PASSWORD_INVALID);
        }
        riskAssessmentService.assessAndBlock(accountNumber, withdrawDTO.getAmount(), null);
        int n = accountMapper.deductBalance(accountNumber, withdrawDTO.getAmount());
        if(n == 0){
            throw new BusinessException(TRANSACTION_INVALID_INSUFFICIENT);
        }
        evictAccountCache(accountNumber);

        Trade trade = new Trade();
        trade.setTradeNo(TicketNoUtil.tradeNo());
        trade.setFromAccount(accountNumber);
        trade.setToAccount("");
        trade.setAmount(withdrawDTO.getAmount());
        trade.setTradeType("取款"); // 3代表取款
        LocalDateTime now = LocalDateTime.now();
        trade.setTradeDate(now);
        trade.setUpdateDate(now);
        tradeService.saveTrade(trade);
    }
    @Transactional
    @Override
    public void register(RegisterDTO registerDTO) {
        long existCount = this.lambdaQuery()
                .eq(Account::getAccountNumber, registerDTO.getAccountNumber())
                .count();
        if (existCount > 0) {
            throw new BusinessException(ACCOUNT_EXISTS);
        }
        if(!registerDTO.getPassword().equals(registerDTO.getCheckPassword())){
            throw new BusinessException(TRANSACTION_INVALID_NOT_SAME_CODE);
        }
        String password = registerDTO.getPassword();
        if(password.length() < 6){
            throw new BusinessException(PASSWORD_TOO_SHORT);
        }
        Account account = new Account();
        account.setAccountBalance(BigDecimal.valueOf(0.00));
        account.setAccountStatus(1);
        account.setAccountNumber(registerDTO.getAccountNumber());
        /*
        对密码进行BCrypt算法，Spring里面集成了这个算法，调用passwordEncoder就行
         */
        account.setPassword(passwordEncoder.encode(password));
        account.setAccountType(registerDTO.getAccountType());
        account.setAccountHolderName(registerDTO.getAccountHolderName());
        account.setUserPhoneNumber(registerDTO.getUserPhoneNumber());
        account.setUserIdCard(registerDTO.getUserIdCard());
        User user = userService.getByUserIdCard(registerDTO.getUserIdCard());
        if (user != null) {
            account.setUserId(user.getId());
        }
        this.baseMapper.insert(account);
        evictAccountCache(registerDTO.getAccountNumber());
    }

    @Override
    @Transactional
    public AccountVO login(LoginDTO loginDTO) {
        Account account = getAccountEntity(loginDTO.getAccountNumber());
        if(!passwordEncoder.matches(loginDTO.getPassword(), account.getPassword())){
            throw new BusinessException(PASSWORD_INVALID);
        }
        AccountVO vo = new AccountVO();
        vo.setAccountNumber(loginDTO.getAccountNumber());
        vo.setAccountBalance(account.getAccountBalance());
        vo.setAccountStatus(account.getAccountStatus());
        vo.setAccountType(account.getAccountType());
        vo.setAccountHolderName(account.getAccountHolderName());
        vo.setUserPhoneNumber(account.getUserPhoneNumber());
        vo.setUserIdCard(account.getUserIdCard());
        vo.setUserId(account.getUserId());
        return vo;
    }

    @Override
    public void updatePsd(UpdatePsdDTO updatePsdDTO) {
        String accountNumber = LoginContext.getAccount();
        if (accountNumber == null) {
            throw new BusinessException(NO_LOGIN);
        }
        Account account = getAccountEntity(accountNumber);
        if(!passwordEncoder.matches(updatePsdDTO.getOldPassword(), account.getPassword())){
            throw new BusinessException(PASSWORD_INVALID);
        }
        if(!updatePsdDTO.getConfirmPassword().equals(updatePsdDTO.getNewPassword())){
            throw new BusinessException(TRANSACTION_INVALID_NOT_SAME_CODE);
        }
        account.setPassword(passwordEncoder.encode(updatePsdDTO.getNewPassword()));
        this.updateById(account);
        evictAccountCache(accountNumber);
    }

    @Override
    public void sendVerificationCode(SendCodeDTO sendCodeDTO) {
        Account account = getAccountEntity(sendCodeDTO.getAccountNumber());
        if (!sendCodeDTO.getUserPhoneNumber().equals(account.getUserPhoneNumber())) {
            throw new BusinessException(PHONE_NUMBER_MISMATCH);
        }
        String code = String.format("%06d", new Random().nextInt(999999));
        redisUtil.set(VERIFICATION_CODE_PREFIX + sendCodeDTO.getAccountNumber(), code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        String phoneTail = sendCodeDTO.getUserPhoneNumber() != null && sendCodeDTO.getUserPhoneNumber().length() >= 4
                ? sendCodeDTO.getUserPhoneNumber().substring(sendCodeDTO.getUserPhoneNumber().length() - 4)
                : "****";
        log.info("验证码已发送 - 账户: {}, 手机尾号: {}", sendCodeDTO.getAccountNumber(), phoneTail);
    }

    @Override
    public void resetPassword(ResetPasswordDTO resetPasswordDTO) {
        if (!resetPasswordDTO.getNewPassword().equals(resetPasswordDTO.getConfirmPassword())) {
            throw new BusinessException(TRANSACTION_INVALID_NOT_SAME_CODE);
        }
        String savedCode = redisUtil.get(VERIFICATION_CODE_PREFIX + resetPasswordDTO.getAccountNumber());
        if (savedCode == null || !savedCode.equals(resetPasswordDTO.getCode())) {
            throw new BusinessException(VERIFICATION_CODE_INVALID);
        }
        Account account = getAccountEntity(resetPasswordDTO.getAccountNumber());
        account.setPassword(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
        this.updateById(account);
        evictAccountCache(resetPasswordDTO.getAccountNumber());
        redisUtil.delete(VERIFICATION_CODE_PREFIX + resetPasswordDTO.getAccountNumber());
    }

    @Override
    public void updateProfile(UpdateProfileDTO updateProfileDTO) {
        String accountNumber = LoginContext.getAccount();
        if (accountNumber == null) {
            throw new BusinessException(NO_LOGIN);
        }
        Account account = getAccountEntity(accountNumber);
        account.setAccountHolderName(updateProfileDTO.getAccountHolderName());
        account.setUserPhoneNumber(updateProfileDTO.getUserPhoneNumber());
        updateById(account);
        evictAccountCache(accountNumber);
    }

    @Override
    public BigDecimal getAccountBalance(String accountNumber) {
        String currentAccountNumber = LoginContext.getAccount();
        if (currentAccountNumber == null) {
            Long userId = LoginContext.getUserId();
            if (userId == null || accountNumber == null || accountNumber.isEmpty()) {
                throw new BusinessException(NO_LOGIN);
            }
            Account account = getAccountEntity(accountNumber);
            if (!userId.equals(account.getUserId())) {
                throw new BusinessException(NO_LOGIN);
            }
            return account.getAccountBalance();
        }
        String targetAccountNumber = accountNumber;
        if(targetAccountNumber == null || targetAccountNumber.isEmpty()){
            targetAccountNumber = currentAccountNumber;
        }
        if(!targetAccountNumber.equals(currentAccountNumber)){
            throw new BusinessException(NO_LOGIN);
        }
        Account account = getAccountEntity(targetAccountNumber);
        return account.getAccountBalance();
    }
}
