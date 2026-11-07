package com.bank.bankfinance.event;

import java.util.Map;

public class RankUpdatedEvent {
    private final String rankType;
    private final Map<String, ?> data;

    public RankUpdatedEvent(String rankType, Map<String, ?> data) {
        this.rankType = rankType;
        this.data = data;
    }

    public String getRankType() {
        return rankType;
    }

    public Map<String, ?> getData() {
        return data;
    }
}