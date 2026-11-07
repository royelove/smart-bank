package com.bank.bankfinance.service;

import com.bank.bankfinance.entity.OrderBookEntry;
import com.bank.bankfinance.entity.StockTrade;
import com.bank.bankfinance.entity.Trade;

import java.math.BigDecimal;
import java.util.List;

public interface MatchingEngine {

    /**
     * 对单只股票进行撮合
     * @param stockCode 股票代码
     * @return 本轮撮合产生的成交记录列表
     */
    List<StockTrade> match(String stockCode);

    /**
     * 价格撮合检查：买价 >= 卖价 可成交
     */
    boolean canMatch(BigDecimal buyPrice, BigDecimal sellPrice);
}