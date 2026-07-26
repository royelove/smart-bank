package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.dto.PurchaseDTO;
import com.bank.bankfinance.dto.RedeemDTO;
import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.entity.FinancialHolding;
import com.bank.bankfinance.entity.FinancialProduct;
import com.bank.bankfinance.entity.Trade;
import com.bank.bankfinance.mapper.AccountMapper;
import com.bank.bankfinance.mapper.FinancialHoldingMapper;
import com.bank.bankfinance.mapper.FinancialProductMapper;
import com.bank.bankfinance.service.AccountService;
import com.bank.bankfinance.service.FinancialService;
import com.bank.bankfinance.service.TradeService;
import com.bank.bankfinance.vo.FinancialHoldingVO;
import com.bank.bankfinance.vo.FinancialProductVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.bank.bankfinance.common.constant.ResponseCode.*;

@Service
public class FinancialServiceImpl extends ServiceImpl<FinancialProductMapper, FinancialProduct> implements FinancialService {

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
        String accountNumber = LoginContext.getAccount();
        if (accountNumber == null) {
            throw new BusinessException(NO_LOGIN);
        }
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
        if (amount.compareTo(product.getRemainingAmount()) > 0) {
            throw new BusinessException(TRANSACTION_INVALID_INSUFFICIENT);
        }
        Account account = accountService.getAccountEntity(accountNumber);
        if (account.getAccountStatus() == 0) {
            throw new BusinessException(TRANSACTION_INVALID_FREEZE);
        }
        if (amount.compareTo(account.getAccountBalance()) > 0) {
            throw new BusinessException(TRANSACTION_INVALID_INSUFFICIENT);
        }
        if (!passwordEncoder.matches(purchaseDTO.getPassword(), account.getPassword())) {
            throw new BusinessException(PASSWORD_INVALID);
        }
        account.setAccountBalance(account.getAccountBalance().subtract(amount));
        accountMapper.updateById(account);
        product.setRemainingAmount(product.getRemainingAmount().subtract(amount));
        financialProductMapper.updateById(product);

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
        trade.setTradeNo(UUID.randomUUID().toString());
        trade.setFromAccount(accountNumber);
        trade.setToAccount("理财购买");
        trade.setAmount(amount);
        trade.setTradeType("理财购买");
        LocalDateTime now = LocalDateTime.now();
        trade.setTradeDate(now);
        trade.setUpdateDate(now);
        tradeService.saveTrade(trade);
    }

    @Override
    public List<FinancialHoldingVO> listHoldings() {
        String accountNumber = LoginContext.getAccount();
        if (accountNumber == null) {
            throw new BusinessException(NO_LOGIN);
        }
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
        String accountNumber = LoginContext.getAccount();
        if (accountNumber == null) {
            throw new BusinessException(NO_LOGIN);
        }
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
        Account account = accountService.getAccountEntity(accountNumber);
        if (account.getAccountStatus() == 0) {
            throw new BusinessException(TRANSACTION_INVALID_FREEZE);
        }
        if (!passwordEncoder.matches(redeemDTO.getPassword(), account.getPassword())) {
            throw new BusinessException(PASSWORD_INVALID);
        }
        BigDecimal days = BigDecimal.valueOf(holding.getTermDays());
        BigDecimal rate = holding.getAnnualReturnRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal yearDays = BigDecimal.valueOf(365);
        BigDecimal expectedReturn = holding.getInvestAmount().multiply(rate).multiply(days).divide(yearDays, 2, RoundingMode.HALF_UP);
        BigDecimal totalReturn = holding.getInvestAmount().add(expectedReturn);
        account.setAccountBalance(account.getAccountBalance().add(totalReturn));
        accountMapper.updateById(account);
        holding.setStatus(1);
        holding.setRedeemDate(LocalDateTime.now());
        financialHoldingMapper.updateById(holding);

        Trade trade = new Trade();
        trade.setTradeNo(UUID.randomUUID().toString());
        trade.setFromAccount("理财赎回");
        trade.setToAccount(accountNumber);
        trade.setAmount(totalReturn);
        trade.setTradeType("理财赎回");
        LocalDateTime now = LocalDateTime.now();
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
