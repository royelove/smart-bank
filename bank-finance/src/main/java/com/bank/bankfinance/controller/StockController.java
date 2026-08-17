package com.bank.bankfinance.controller;

import com.bank.bankfinance.common.result.Result;
import com.bank.bankfinance.dto.trade.StockOrderDTO;
import com.bank.bankfinance.service.StockService;
import com.bank.bankfinance.vo.PositionVO;
import com.bank.bankfinance.vo.StockOrderVO;
import com.bank.bankfinance.vo.StockQuoteVO;
import com.bank.bankfinance.vo.StockVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private StockService stockService;

    @GetMapping("/list")
    public Result<List<StockVO>> list() {
        return Result.success(stockService.listStocks());
    }

    @GetMapping("/quote")
    public Result<List<StockQuoteVO>> quote() {
        return Result.success(stockService.listQuotes());
    }

    @GetMapping("/provider")
    public Result<String> provider() {
        return Result.success(stockService.quoteProvider());
    }

    @PostMapping("/order")
    public Result<Void> order(@Valid @RequestBody StockOrderDTO dto) {
        stockService.placeOrder(dto);
        return Result.success();
    }

    @GetMapping("/orders")
    public Result<List<StockOrderVO>> orders(@RequestParam String accountNumber) {
        return Result.success(stockService.myOrders(accountNumber));
    }

    @GetMapping("/positions")
    public Result<List<PositionVO>> positions(@RequestParam String accountNumber) {
        return Result.success(stockService.myPositions(accountNumber));
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam String accountNumber) {
        stockService.cancelOrder(id, accountNumber);
        return Result.success();
    }
}