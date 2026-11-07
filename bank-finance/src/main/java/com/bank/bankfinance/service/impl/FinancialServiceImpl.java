package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.common.util.RedisUtil;
import com.bank.bankfinance.common.util.TicketNoUtil;
import com.bank.bankfinance.dto.trade.PurchaseDTO;
import com.bank.bankfinance.dto.trade.RedeemDTO;
import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.entity.FinancialHolding;
import com.bank.bankfinance.entity.FinancialProduct;
import com.bank.bankfinance.entity.Trade;
import com.bank.bankfinance.mapper.AccountMapper;
import com.bank.bankfinance.mapper.FinancialHoldingMapper;
import com.bank.bankfinance.mapper.FinancialProductMapper;
import com.bank.bankfinance.service.AccountService;
import com.bank.bankfinance.service.FinancialService;
import com.bank.bankfinance.service.RiskAssessmentService;
import com.bank.bankfinance.service.TradeService;
import com.bank.bankfinance.vo.FinancialHoldingVO;
import com.bank.bankfinance.vo.FinancialProductVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.bank.bankfinance.common.constant.ResponseCode.*;

@Service
public class FinancialServiceImpl extends ServiceImpl<FinancialProductMapper, FinancialProduct> implements FinancialService {

    /** 新增：定时结算需要逐笔记录失败原因（原来失败是静默的） */
    private static final Logger log = LoggerFactory.getLogger(FinancialServiceImpl.class);

    @Autowired
    private FinancialProductMapper financialProductMapper;

    @Autowired
    private FinancialHoldingMapper financialHoldingMapper;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @Autowired
    private RedisUtil redisUtil;

    /** 新增：定时结算改为「每笔持仓一个独立事务」，一笔失败不影响其它持仓 */
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public List<FinancialProductVO> listProducts() {
        List<FinancialProduct> products = this.lambdaQuery()
                .eq(FinancialProduct::getStatus, 1)
                .orderByDesc(FinancialProduct::getCreationDate)
                .list();
        return products.stream().map(this::toProductVO).collect(Collectors.toList());
    }

    @Override
    public FinancialProductVO getProductDetail(Long id) {
        FinancialProduct product = financialProductMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(BANK_ID_INVALID);
        }
        return toProductVO(product);
    }

    @Transactional
    @Override
    public void purchase(PurchaseDTO purchaseDTO) {
        String accountNumber = purchaseDTO.getAccountNumber();
        /*
         * 修复记录：原幂等 key 是 "idem:purchase:" + requestId，没带账户维度。
         * requestId 由客户端生成，于是 A 用户撞上（或猜到）B 用户用过的 requestId 就会被判成
         * 重复提交而买不进去；反过来，恶意方只要复用同一个 requestId 就能让别人 10 分钟内无法购买。
         * 幂等的作用域应该是「谁的哪一次请求」，改为按「账户 + requestId」定 key。
         */
        String idemKey = "idem:purchase:" + accountNumber + ":" + purchaseDTO.getRequestId();
        Boolean first = redisUtil.setIfAbsent(idemKey, "1", 10, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(first)) {
            throw new BusinessException(IDEMPOTENT_REJECTED);
        }
        try {
            Account account = validateAccountAccess(accountNumber);
            FinancialProduct product = financialProductMapper.selectById(purchaseDTO.getProductId());
            if (product == null) {
                throw new BusinessException(BANK_ID_INVALID);
            }
            if (product.getStatus() != 1) {
                throw new BusinessException(TRANSACTION_INVALID_FREEZE);
            }
            BigDecimal amount = purchaseDTO.getAmount();
            if (amount.compareTo(product.getMinAmount()) < 0) {
                throw new BusinessException(TRANSACTION_INVALID_INSUFFICIENT);
            }
            if (account.getAccountStatus() == 0) {
                throw new BusinessException(TRANSACTION_INVALID_FREEZE);
            }
            if (!passwordEncoder.matches(purchaseDTO.getPassword(), account.getPassword())) {
                throw new BusinessException(PASSWORD_INVALID);
            }
            riskAssessmentService.assessAndBlock(accountNumber, amount, null);
            /*
             * 修复记录（账户侧）：原实现是「getAccountEntity 拿到内存快照 → setAccountBalance(余额-amount)
             * → accountMapper.updateById(account)」。三个问题：
             * 1) 守卫用余额而不是可用资金（余额-冻结），可以拿已委托冻结的股票买单资金去买理财；
             * 2) 读-算-写会把读取时的整行字段写回，并发下互相覆盖（丢失更新）；
             * 3) getAccountEntity 可能直接命中 30 分钟的 Redis 缓存，余额快照更旧。
             * 改为原子条件更新，返回 0 行即可用资金不足。
             */
            if (accountMapper.deductAvailableBalance(accountNumber, amount) == 0) {
                throw new BusinessException(TRANSACTION_INVALID_INSUFFICIENT);
            }
            /*
             * 修复记录（产品侧）：原来的额度校验是「读出 product.remainingAmount 比一下，
             * 再 setRemainingAmount(remaining - amount) + updateById(product)」。
             * 并发购买时两个请求读到同一个 remaining_amount，各自减完再写回，后写的覆盖先写的，
             * 于是产品被超卖（额度永远减不到 0，卖出去的总额可以超过 total_amount）。
             * 改成数据库层的条件更新：剩余额度不足时返回 0 行。
             */
            if (financialProductMapper.reserveAmount(product.getId(), amount) == 0) {
                throw new BusinessException(TRANSACTION_INVALID_INSUFFICIENT);
            }
            // 余额已变，清缓存（处于事务中，实际删除会推迟到事务完成之后）
            accountService.evictAccountCache(accountNumber);

            FinancialHolding holding = new FinancialHolding();
            holding.setAccountNumber(accountNumber);
            holding.setProductId(product.getId());
            holding.setProductName(product.getProductName());
            holding.setAnnualReturnRate(product.getAnnualReturnRate());
            holding.setTermDays(product.getTermDays());
            holding.setInvestAmount(amount);
            holding.setStatus(0);
            holding.setPurchaseDate(LocalDateTime.now());
            financialHoldingMapper.insert(holding);

            Trade trade = new Trade();
            trade.setTradeNo(TicketNoUtil.tradeNo());
            trade.setFromAccount(accountNumber);
            trade.setToAccount("理财购买");
            trade.setAmount(amount);
            trade.setTradeType("理财购买");
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
    public List<FinancialHoldingVO> listHoldings(String accountNumber) {
        validateAccountAccess(accountNumber);
        List<FinancialHolding> holdings = financialHoldingMapper.selectList(
                new LambdaQueryWrapper<FinancialHolding>()
                        .eq(FinancialHolding::getAccountNumber, accountNumber)
                        .orderByDesc(FinancialHolding::getPurchaseDate)
        );
        return holdings.stream().map(this::toHoldingVO).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void redeem(RedeemDTO redeemDTO) {
        String accountNumber = redeemDTO.getAccountNumber();
        Account account = validateAccountAccess(accountNumber);
        FinancialHolding holding = financialHoldingMapper.selectById(redeemDTO.getHoldingId());
        if (holding == null) {
            throw new BusinessException(BANK_ID_INVALID);
        }
        if (!holding.getAccountNumber().equals(accountNumber)) {
            throw new BusinessException(NO_ADMIN_PERMISSION);
        }
        if (holding.getStatus() != 0) {
            throw new BusinessException(TRANSACTION_INVALID_FREEZE);
        }
        if (account.getAccountStatus() == 0) {
            throw new BusinessException(TRANSACTION_INVALID_FREEZE);
        }
        if (!passwordEncoder.matches(redeemDTO.getPassword(), account.getPassword())) {
            throw new BusinessException(PASSWORD_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        if (!claimHolding(holding, now)) {
            throw new BusinessException(TRANSACTION_INVALID_FREEZE);
        }
        settleHolding(holding, now);
    }

    private Account validateAccountAccess(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            throw new BusinessException(NO_LOGIN);
        }
        String currentAccount = LoginContext.getAccount();
        if (currentAccount != null) {
            if (!currentAccount.equals(accountNumber)) {
                throw new BusinessException(NO_LOGIN);
            }
            return accountService.getAccountEntity(accountNumber);
        }
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            throw new BusinessException(NO_LOGIN);
        }
        Account account = accountService.getAccountEntity(accountNumber);
        if (!userId.equals(account.getUserId())) {
            throw new BusinessException(NO_LOGIN);
        }
        return account;
    }

    /**
     * 每日凌晨的到期自动结算。
     *
     * <p>修复记录：原方法在整个 for 循环外面套了一个 {@code @Transactional}，且循环体里
     * 「账户查不到 / 账户已冻结就 continue」「claimHolding 成功但 settleHolding 直接 return」
     * 都不报错。两个后果：
     * 1) 任何一笔持仓结算抛异常（例如插交易流水失败），当天<b>所有</b>已到期的持仓一起回滚，
     *    而回滚是静默的，日志里只有异常没有「哪些持仓没结」；
     * 2) 账户冻结的持仓被 {@code continue} 跳过后，状态位仍是 0，看起来没问题；
     *    但 {@code claimHolding} 已经把状态改成 1 而 {@code settleHolding} 又提前 return 的那条路径，
     *    是「持仓标记已赎回、钱没到账」，用户的资金被静默没收。
     * 现在改为「每笔持仓一个独立事务」，失败只影响自己并留 error 日志，状态位一起回滚，
     * 下一轮定时任务会重试；账户不存在/冻结则明确跳过并告警。
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void autoSettleMaturedHoldings() {
        LocalDateTime now = LocalDateTime.now();
        List<FinancialHolding> holdings = financialHoldingMapper.selectList(
                new LambdaQueryWrapper<FinancialHolding>()
                        .eq(FinancialHolding::getStatus, 0)
        );
        for (FinancialHolding holding : holdings) {
            if (!isMatured(holding, now)) {
                continue;
            }
            try {
                Account account = accountService.getAccountEntity(holding.getAccountNumber());
                if (account.getAccountStatus() == 0) {
                    log.warn("理财到期结算跳过：账户已冻结，持仓保持未结算等待人工处理, holdingId={}, accountNumber={}",
                            holding.getId(), holding.getAccountNumber());
                    continue;
                }
            } catch (BusinessException e) {
                log.warn("理财到期结算跳过：账户不存在, holdingId={}, accountNumber={}",
                        holding.getId(), holding.getAccountNumber());
                continue;
            }
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    if (claimHolding(holding, now)) {
                        settleHolding(holding, now);
                    }
                });
            } catch (Exception e) {
                log.error("理财到期结算失败，持仓已回滚为未结算状态，等待下次重试: holdingId={}, accountNumber={}",
                        holding.getId(), holding.getAccountNumber(), e);
            }
        }
    }

    private boolean isMatured(FinancialHolding holding, LocalDateTime now) {
        LocalDateTime maturityDate = holding.getPurchaseDate().plusDays(holding.getTermDays());
        return !maturityDate.isAfter(now);
    }

    private boolean claimHolding(FinancialHolding holding, LocalDateTime now) {
        LambdaUpdateWrapper<FinancialHolding> uw = new LambdaUpdateWrapper<>();
        uw.eq(FinancialHolding::getId, holding.getId())
                .eq(FinancialHolding::getStatus, 0)
                .set(FinancialHolding::getStatus, 1)
                .set(FinancialHolding::getRedeemDate, now);
        return financialHoldingMapper.update(null, uw) > 0;
    }

    /**
     * 赎回入账。
     *
     * <p>修复记录：原实现先由调用方 {@code claimHolding} 把持仓状态改成「已赎回」，
     * 然后这里「查不到账户 / 账户已冻结就直接 return」。于是钱没入账、持仓却已经是已赎回，
     * 用户的资金被静默没收，连一条日志都没有。另外入账用的是「读余额 → 加 → updateById」，
     * 同样是丢失更新。
     * 现在：入账失败必须抛异常，让调用方事务把状态位一起回滚；是否允许结算
     * （账户是否存在/是否冻结）由调用方先判断——用户主动赎回时直接报错，
     * 定时任务则跳过并告警，留到下一轮。
     */
    private void settleHolding(FinancialHolding holding, LocalDateTime now) {
        BigDecimal totalReturn = holding.getInvestAmount();
        String tradeType = "理财提前赎回";
        if (isMatured(holding, now)) {
            BigDecimal days = BigDecimal.valueOf(holding.getTermDays());
            BigDecimal rate = holding.getAnnualReturnRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
            BigDecimal yearDays = BigDecimal.valueOf(365);
            BigDecimal expectedReturn = holding.getInvestAmount().multiply(rate).multiply(days).divide(yearDays, 2, RoundingMode.HALF_UP);
            totalReturn = totalReturn.add(expectedReturn);
            tradeType = "理财赎回";
        }
        if (accountMapper.addBalance(holding.getAccountNumber(), totalReturn) == 0) {
            throw new IllegalStateException("理财结算入账失败，账户不存在: accountNumber=" + holding.getAccountNumber()
                    + ", holdingId=" + holding.getId() + ", amount=" + totalReturn);
        }
        accountService.evictAccountCache(holding.getAccountNumber());

        Trade trade = new Trade();
        trade.setTradeNo(TicketNoUtil.tradeNo());
        trade.setFromAccount(tradeType);
        trade.setToAccount(holding.getAccountNumber());
        trade.setAmount(totalReturn);
        trade.setTradeType(tradeType);
        trade.setTradeDate(now);
        trade.setUpdateDate(now);
        tradeService.saveTrade(trade);
    }

    private FinancialProductVO toProductVO(FinancialProduct p) {
        FinancialProductVO vo = new FinancialProductVO();
        vo.setId(p.getId());
        vo.setProductCode(p.getProductCode());
        vo.setProductName(p.getProductName());
        vo.setAnnualReturnRate(p.getAnnualReturnRate());
        vo.setTermDays(p.getTermDays());
        vo.setMinAmount(p.getMinAmount());
        vo.setTotalAmount(p.getTotalAmount());
        vo.setRemainingAmount(p.getRemainingAmount());
        vo.setStatus(p.getStatus());
        vo.setDescription(p.getDescription());
        vo.setCreationDate(p.getCreationDate());
        return vo;
    }

    private FinancialHoldingVO toHoldingVO(FinancialHolding h) {
        FinancialHoldingVO vo = new FinancialHoldingVO();
        vo.setId(h.getId());
        vo.setProductId(h.getProductId());
        vo.setProductName(h.getProductName());
        vo.setAnnualReturnRate(h.getAnnualReturnRate());
        vo.setTermDays(h.getTermDays());
        vo.setInvestAmount(h.getInvestAmount());
        vo.setStatus(h.getStatus());
        vo.setPurchaseDate(h.getPurchaseDate());
        vo.setRedeemDate(h.getRedeemDate());
        BigDecimal days = BigDecimal.valueOf(h.getTermDays());
        BigDecimal rate = h.getAnnualReturnRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal yearDays = BigDecimal.valueOf(365);
        BigDecimal expectedReturn = h.getInvestAmount().multiply(rate).multiply(days).divide(yearDays, 2, RoundingMode.HALF_UP);
        vo.setExpectedReturn(expectedReturn);
        return vo;
    }
}
