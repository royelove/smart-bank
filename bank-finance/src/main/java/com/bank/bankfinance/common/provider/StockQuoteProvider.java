package com.bank.bankfinance.common.provider;

import com.bank.bankfinance.entity.Stock;
import com.bank.bankfinance.vo.StockQuoteVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 股票行情源
 */
public interface StockQuoteProvider {
    /** 行情源标识，对应 application.yml 的 stock.quote-provider */
    String name();

    StockQuoteVO quote(Stock stock);

    /**
     * 批量行情。默认逐只调用 {@link #quote(Stock)} 并丢弃取不到的行；
     * 支持批量接口的行情源（如腾讯）覆写本方法，避免 N 次外部请求。
     */
    default List<StockQuoteVO> batchQuote(List<Stock> stocks) {
        List<StockQuoteVO> quotes = new ArrayList<>();
        if (stocks == null || stocks.isEmpty()) {
            return quotes;
        }
        for (Stock stock : stocks) {
            StockQuoteVO vo = quote(stock);
            if (vo != null) {
                quotes.add(vo);
            }
        }
        return quotes;
    }
}
