package com.bank.bankfinance.service;

import com.bank.bankfinance.vo.BalanceForecastVO;

public interface TrendForecastService {
    BalanceForecastVO forecast(String accountNumber);
}
