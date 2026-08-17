package com.bank.bankfinance.common.provider;

import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.entity.Stock;
import com.bank.bankfinance.vo.StockQuoteVO;
import org.springframework.stereotype.Component;

import static com.bank.bankfinance.common.constant.ResponseCode.STOCK_QUOTE_UNAVAILABLE;

/**
 * 新浪真实行情（预留），当前未接入
 */
@Component
public class SinaStockQuoteProvider implements StockQuoteProvider {

    @Override
    public String name() {
        return "sina";
    }

    @Override
    public StockQuoteVO quote(Stock stock) {
        throw new BusinessException(STOCK_QUOTE_UNAVAILABLE);
    }
}