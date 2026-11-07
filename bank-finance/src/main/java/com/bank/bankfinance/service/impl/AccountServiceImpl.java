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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.bank.bankfinance.common.constant.ResponseCode.*;

@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService{

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);
    private static final String ACCOUNT_CACHE_PREFIX = "account:cache:";
    private static final long CACHE_EXPIRE_MINUTES = 30;
    private static final String VERIFICATION_CODE_PREFIX = "verify:code:";
    private static final long CODE_EXPIRE_MINUTES = 5;
    /** 新增：验证码连续输错次数计数前缀 */
    private static final String VERIFICATION_CODE_FAIL_PREFIX = "verify:code:fail:";
    /** 新增：同一账户验证码最多允许输错几次，超过即作废当前验证码 */
    private static final int MAX_CODE_ATTEMPTS = 5;
    /** 新增：验证码必须用密码学安全随机数生成（原为 java.util.Random，可被预测） */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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

    /**
     * 清理账户缓存。
     *
     * <p>修复记录：原实现在事务内直接删 key。事务提交前删掉缓存，如果这时有并发请求读到
     * 「还没提交」的旧数据并回填缓存，30 分钟的缓存过期前所有人看到的都是旧余额
     * （删除动作发生在提交之前，等于白删）。现在只要处于事务中就注册
     * {@link TransactionSynchronization}，把删除推迟到事务完成之后再执行。
     */
    @Override
    public void evictAccountCache(String accountNumber) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    doEvictAccountCache(accountNumber);
                }
            });
            return;
        }
        doEvictAccountCache(accountNumber);
    }

    private void doEvictAccountCache(String accountNumber) {
        try {
            redisUtil.delete(ACCOUNT_CACHE_PREFIX + accountNumber);
        } catch (Exception e) {
            log.warn("Redis 缓存删除失败: {}", e.getMessage());
        }
    }



    @Transactional
    @Override
    public void deposit(DepositDTO depositDTO) {
        //依据是requestId，而不是accountId,因此不是10分钟内智能存一次款
        String accountNumber = depositDTO.getAccountNo();
        /*
         * 修复记录：原幂等 key 为 "idem:deposit:" + requestId，缺少账户维度。
         * requestId 由客户端生成，不同用户之间会互相干扰：只要有人猜到/复用了你的 requestId，
         * 你 10 分钟内的存款就会被判成「重复提交」而失败；反过来攻击者可以拿一个固定 requestId
         * 反复占用，让受害者无法存款。幂等域必须是「谁的哪一次请求」。
         */
        String idemKey = "idem:deposit:" + accountNumber + ":" + depositDTO.getRequestId();
        Boolean first = redisUtil.setIfAbsent(idemKey, "1", 10, TimeUnit.MINUTES);
        if(!Boolean.TRUE.equals(first)){
            throw new BusinessException(IDEMPOTENT_REJECTED);
        }
        try {
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
            redisUtil.delete(idemKey);
            throw e;
        }
    }

    @Override
    @Transactional
    public void transfer(TransferDTO transferDTO) {
        String inNumber = transferDTO.getInNumber();
        String outNumber = transferDTO.getOutNumber();
        /*
         * 修复记录：原幂等 key 为 "idem:transfer:" + requestId，缺少账户维度。
         * 与存款同理：A 用户只要复用 B 用过的 requestId，就能让 B 的转账被判成重复提交而失败，
         * 或者反过来利用别人的 requestId 把自己的重复请求「洗」成唯一请求。
         * 幂等域改为「付款方:收款方:requestId」。
         */
        String idemKey = "idem:transfer:" + outNumber + ":" + inNumber + ":" + transferDTO.getRequestId();
        Boolean first = redisUtil.setIfAbsent(idemKey, "1", 10, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(first)) {
            throw new BusinessException(IDEMPOTENT_REJECTED);
        }
        try {
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
            /*
             * 修复：原实现是内存里 read-modify-write：
             *   outAccount.setAccountBalance(outAccount.getAccountBalance().subtract(amount));
             *   this.updateById(outAccount);
             * 两个问题：
             * 1) 守卫用的是「余额」而不是「可用资金（余额-冻结）」，可以把已委托冻结的钱转走；
             * 2) 整行 updateById 会把读取时的其余字段一并写回，并对余额做「读-算-写」，
             *    任何绕过 selectForUpdate 的并发写都会被覆盖（丢失更新）。
             * 改为原子条件更新：付款方按可用资金扣减，收款方增量入账。
             */
            if (accountMapper.deductAvailableBalance(outNumber, transferDTO.getAmount()) == 0) {
                throw new BusinessException(TRANSACTION_INVALID_INSUFFICIENT);
            }
            accountMapper.addBalance(inNumber, transferDTO.getAmount());
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
            redisUtil.delete(idemKey);
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
        // 修复记录：同存款/转账，幂等 key 补上账户维度，避免不同用户之间用 requestId 互相干扰
        String idemKey = "idem:withdraw:" + accountNumber + ":" + withdrawDTO.getRequestId();
        Boolean first = redisUtil.setIfAbsent(idemKey, "1", 10, TimeUnit.MINUTES);
        if(!Boolean.TRUE.equals(first)){
            throw new BusinessException(IDEMPOTENT_REJECTED);
        }
        try {
            Account account = getAccountEntity(accountNumber);
            String password = withdrawDTO.getPassword();
            if(account.getAccountStatus()==0){
                throw new BusinessException(TRANSACTION_INVALID_FREEZE);
            }
            if (!passwordEncoder.matches(password, account.getPassword())) {
                throw new BusinessException(PASSWORD_INVALID);
            }
            riskAssessmentService.assessAndBlock(accountNumber, withdrawDTO.getAmount(), null);
            /*
             * 修复：原调用 deductBalance（只守卫 account_balance >= amount），不看冻结额。
             * 于是「先挂股票买单冻结资金，再提现同一笔钱」可以成功，账户余额被抽到低于冻结额，
             * 之后成交结算要么把余额扣成负数、要么直接结算失败。改用可用资金（余额-冻结）守卫。
             * 同时这也是原子的条件更新，不再依赖前面查出来的内存余额快照。
             */
            int n = accountMapper.deductAvailableBalance(accountNumber, withdrawDTO.getAmount());
            if(n == 0){
                throw new BusinessException(TRANSACTION_INVALID_INSUFFICIENT);
            }
            evictAccountCache(accountNumber);

            Trade trade = new Trade();
            trade.setTradeNo(TicketNoUtil.tradeNo());
            trade.setFromAccount(accountNumber);
            trade.setToAccount("");
            trade.setAmount(withdrawDTO.getAmount());
            trade.setTradeType("取款");
            LocalDateTime now = LocalDateTime.now();
            trade.setTradeDate(now);
            trade.setUpdateDate(now);
            tradeService.saveTrade(trade);
        } catch (Exception e) {
            redisUtil.delete(idemKey);
            throw e;
        }
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
        /*
         * 修复：原为 new Random().nextInt(999999)。
         * 1) java.util.Random 是可预测的伪随机（48 位线性同余），攻击者观察到几个验证码后
         *    即可推算后续验证码，配合重置密码就等于接管账户；改用 SecureRandom。
         * 2) nextInt(999999) 的取值区间是 [0, 999998]，永远取不到 999999，
         *    且取模偏差使分布不均匀；改为 nextInt(1_000_000)。
         */
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        redisUtil.set(VERIFICATION_CODE_PREFIX + sendCodeDTO.getAccountNumber(), code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        // 重发验证码时清空失败计数，避免用户被自己之前的输错锁死
        redisUtil.delete(verifyFailKey(sendCodeDTO.getAccountNumber()));
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
        String accountNumber = resetPasswordDTO.getAccountNumber();
        String codeKey = VERIFICATION_CODE_PREFIX + accountNumber;
        String savedCode = redisUtil.get(codeKey);
        if (savedCode == null || !savedCode.equals(resetPasswordDTO.getCode())) {
            /*
             * 修复：原实现只比较验证码，没有任何失败次数限制。验证码只有 6 位（10^6）且有效期
             * CODE_EXPIRE_MINUTES 分钟，攻击者可以对同一账户持续尝试直到命中——接口限流只能减缓
             * 速率，不能从根本上限制尝试次数。这里在 Redis 里对该账户累计失败次数，
             * 达到上限即直接作废当前验证码，必须重新发码，使「猜中概率」与「尝试次数」解耦。
             */
            long fails = incrementVerifyFail(accountNumber);
            if (fails >= MAX_CODE_ATTEMPTS) {
                redisUtil.delete(codeKey);
                log.warn("验证码连续输错 {} 次，已作废该账户验证码 account={}", fails, accountNumber);
            }
            throw new BusinessException(VERIFICATION_CODE_INVALID);
        }
        Account account = getAccountEntity(accountNumber);
        account.setPassword(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
        this.updateById(account);
        evictAccountCache(accountNumber);
        redisUtil.delete(codeKey);
        redisUtil.delete(verifyFailKey(accountNumber));
    }

    private String verifyFailKey(String accountNumber) {
        return VERIFICATION_CODE_FAIL_PREFIX + accountNumber;
    }

    /** 累计并返回验证码失败次数，首次写入时设置过期时间（与验证码同生命周期量级） */
    private long incrementVerifyFail(String accountNumber) {
        String key = verifyFailKey(accountNumber);
        Long fails = redisUtil.increment(key);
        if (fails != null && fails == 1L) {
            redisUtil.expire(key, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        return fails == null ? 0L : fails;
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
    public boolean freezeBuyFunds(String accountNumber, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return false;
        }
        int rows = accountMapper.freezeBalance(accountNumber, amount);
        if (rows > 0) {
            evictAccountCache(accountNumber);
        }
        return rows > 0;
    }

    @Override
    public boolean releaseBuyFunds(String accountNumber, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return false;
        }
        int rows = accountMapper.unfreezeBalance(accountNumber, amount);
        if (rows > 0) {
            evictAccountCache(accountNumber);
        }
        return rows > 0;
    }

    @Override
    public boolean settleBuyFunds(String accountNumber, BigDecimal freezeAmount, BigDecimal actualAmount) {
        // 返回 0 行 = 冻结额或余额不足（记账已不一致），必须让调用方回滚，绝不能静默继续
        int rows = accountMapper.settleBuy(accountNumber, freezeAmount, actualAmount);
        if (rows > 0) {
            evictAccountCache(accountNumber);
        }
        return rows > 0;
    }

    @Override
    public void creditBalance(String accountNumber, BigDecimal amount) {
        accountMapper.addBalance(accountNumber, amount);
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
