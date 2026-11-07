package com.bank.bankfinance.event;

import com.bank.bankfinance.entity.OrderBookEntry;

public class OrderPlacedEvent {
    private final OrderBookEntry order;

    public OrderPlacedEvent(OrderBookEntry order) {
        this.order = order;
    }

    public OrderBookEntry getOrder() {
        return order;
    }
}