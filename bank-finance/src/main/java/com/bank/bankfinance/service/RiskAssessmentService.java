package com.bank.bankfinance.service;

import java.math.BigDecimal;

public interface RiskAssessmentService {
    void assessAndBlock(String accountNumber, BigDecimal amount, String counterparty);
}
