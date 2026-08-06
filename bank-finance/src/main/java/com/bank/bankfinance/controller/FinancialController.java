package com.bank.bankfinance.controller;

import com.bank.bankfinance.common.result.Result;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.dto.trade.PurchaseDTO;
import com.bank.bankfinance.dto.trade.RedeemDTO;
import com.bank.bankfinance.service.FinancialService;
import com.bank.bankfinance.service.RecommendationService;
import com.bank.bankfinance.vo.FinancialHoldingVO;
import com.bank.bankfinance.vo.FinancialProductVO;
import com.bank.bankfinance.vo.FinancialRecommendVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/financial")
public class FinancialController {

    @Autowired
    private FinancialService financialService;

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/products")
    public Result<List<FinancialProductVO>> listProducts() {
        return Result.success(financialService.listProducts());
    }

    @GetMapping("/recommend")
    public Result<List<FinancialRecommendVO>> recommend(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(defaultValue = "3") int topN) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            accountNumber = LoginContext.getAccount();
        }
        return Result.success(recommendationService.recommend(accountNumber, topN));
    }

    @GetMapping("/product/{id}")
    public Result<FinancialProductVO> getProductDetail(@PathVariable Long id) {
        return Result.success(financialService.getProductDetail(id));
    }

    @PostMapping("/purchase")
    public Result<Void> purchase(@Valid @RequestBody PurchaseDTO purchaseDTO) {
        financialService.purchase(purchaseDTO);
        return Result.success();
    }

    @GetMapping("/holdings")
    public Result<List<FinancialHoldingVO>> listHoldings(@RequestParam String accountNumber) {
        return Result.success(financialService.listHoldings(accountNumber));
    }

    @PostMapping("/redeem")
    public Result<Void> redeem(@Valid @RequestBody RedeemDTO redeemDTO) {
        financialService.redeem(redeemDTO);
        return Result.success();
    }
}
