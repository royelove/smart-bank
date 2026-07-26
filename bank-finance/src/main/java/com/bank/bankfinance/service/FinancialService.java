package com.bank.bankfinance.service;

import com.bank.bankfinance.dto.PurchaseDTO;
import com.bank.bankfinance.dto.RedeemDTO;
import com.bank.bankfinance.vo.FinancialHoldingVO;
import com.bank.bankfinance.vo.FinancialProductVO;

import java.util.List;

public interface FinancialService {
    List<FinancialProductVO> listProducts();
    FinancialProductVO getProductDetail(Long id);
    void purchase(PurchaseDTO purchaseDTO);
    List<FinancialHoldingVO> listHoldings();
    void redeem(RedeemDTO redeemDTO);
}
