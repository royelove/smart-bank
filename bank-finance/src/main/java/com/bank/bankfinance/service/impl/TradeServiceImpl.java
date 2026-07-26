package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.dto.TradeQueryDTO;
import com.bank.bankfinance.entity.Trade;
import com.bank.bankfinance.mapper.TradeMapper;
import com.bank.bankfinance.service.TradeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TradeServiceImpl extends ServiceImpl<TradeMapper, Trade> implements TradeService {

    @Override
    public void saveTrade(Trade trade) {
        this.save(trade);
    }

    @Override
    public  IPage<Trade> getTradesByAccount(TradeQueryDTO tradeQueryDTO) {
        String accountNumber = tradeQueryDTO.getAccountNumber();
        Integer page = tradeQueryDTO.getPage();
        Integer size = tradeQueryDTO.getSize();
        Page<Trade> pageParam = new Page<>(page,size);
        LambdaQueryWrapper<Trade> wrapper = new LambdaQueryWrapper<>();
        if(accountNumber != null && !accountNumber.isEmpty()) {
            wrapper.eq(Trade::getFromAccount, accountNumber)
                    .or()
                    .eq(Trade::getToAccount, accountNumber);
        }
        wrapper.orderByDesc(Trade :: getTradeDate);
        return this.page(pageParam,wrapper);
    }
}
