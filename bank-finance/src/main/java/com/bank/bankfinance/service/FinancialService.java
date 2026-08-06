package com.bank.bankfinance.service;

import com.bank.bankfinance.dto.trade.PurchaseDTO;
import com.bank.bankfinance.dto.trade.RedeemDTO;
import com.bank.bankfinance.vo.FinancialHoldingVO;
import com.bank.bankfinance.vo.FinancialProductVO;

import java.util.List;

public interface FinancialService {
    List<FinancialProductVO> listProducts();
    FinancialProductVO getProductDetail(Long id);
    void purchase(PurchaseDTO purchaseDTO);
    List<FinancialHoldingVO> listHoldings(String accountNumber);
    void redeem(RedeemDTO redeemDTO);
}
