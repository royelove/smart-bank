package com.bank.bankfinance.event;

import com.bank.bankfinance.entity.OrderBookEntry;
import com.bank.bankfinance.entity.StockTrade;
import com.bank.bankfinance.entity.Trade;

public class TradeExecutedEvent {
    private final StockTrade trade;
    private final OrderBookEntry buyOrder;
    private final OrderBookEntry sellOrder;

    public TradeExecutedEvent(StockTrade trade, OrderBookEntry buyOrder, OrderBookEntry sellOrder) {
        this.trade = trade;
        this.buyOrder = buyOrder;
        this.sellOrder = sellOrder;
    }

    public StockTrade getTrade() {
        return trade;
    }

    public OrderBookEntry getBuyOrder() {
        return buyOrder;
    }

    public OrderBookEntry getSellOrder() {
        return sellOrder;
    }
}