package com.bank.bankfinance.common.provider;

import com.bank.bankfinance.entity.Stock;
import com.bank.bankfinance.vo.StockQuoteVO;

/**
 * 股票行情源
 */
public interface StockQuoteProvider {
    /** 行情源标识，对应 application.yml 的 stock.quote-provider */
    String name();

    StockQuoteVO quote(Stock stock);
}