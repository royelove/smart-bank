package com.bank.bankfinance.service;

import com.bank.bankfinance.dto.trade.StockOrderDTO;
import com.bank.bankfinance.vo.PositionVO;
import com.bank.bankfinance.vo.StockOrderVO;
import com.bank.bankfinance.vo.StockQuoteVO;
import com.bank.bankfinance.vo.StockVO;

import java.util.List;

public interface StockService {
    List<StockVO> listStocks();
    List<StockQuoteVO> listQuotes();
    String quoteProvider();
    void placeOrder(StockOrderDTO dto);
    List<StockOrderVO> myOrders(String accountNumber);
    List<PositionVO> myPositions(String accountNumber);
    void cancelOrder(Long orderId, String accountNumber);
}