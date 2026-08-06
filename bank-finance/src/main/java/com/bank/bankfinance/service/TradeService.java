package com.bank.bankfinance.service;

import com.bank.bankfinance.dto.trade.TradeQueryDTO;
import com.bank.bankfinance.entity.Trade;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;


public interface TradeService {
    void saveTrade(Trade trade);
    IPage<Trade> getTradesByAccount(TradeQueryDTO tradeQueryDTO);
}
