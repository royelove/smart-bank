package com.bank.bankfinance.service;

import com.bank.bankfinance.vo.FinancialRecommendVO;

import java.util.List;

public interface RecommendationService {
    List<FinancialRecommendVO> recommend(String accountNumber, int topN);
}
