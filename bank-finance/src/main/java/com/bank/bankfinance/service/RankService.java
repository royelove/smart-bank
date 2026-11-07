package com.bank.bankfinance.service;

import com.bank.bankfinance.entity.StockRank;

import java.util.List;
import java.util.Map;

public interface RankService {

    List<StockRank> getRankList(String rankType, int topN);

    StockRank getRankDetail(String stockCode, String rankType);

    void refreshAllRank();

    Map<String, List<StockRank>> getAllRankTopN(int topN);

    Integer getStockRank(String stockCode, String rankType);

    Double getRankScore(String stockCode, String rankType);
    void refreshRank(String rankType);
}