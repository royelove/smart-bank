package com.bank.bankfinance.controller;

import com.bank.bankfinance.common.annotation.RateLimit;
import com.bank.bankfinance.common.result.Result;
import com.bank.bankfinance.dto.trade.StockOrderDTO;
import com.bank.bankfinance.service.KlineService;
import com.bank.bankfinance.service.StockService;
import com.bank.bankfinance.vo.PositionVO;
import com.bank.bankfinance.vo.StockKlineVO;
import com.bank.bankfinance.vo.StockOrderVO;
import com.bank.bankfinance.vo.StockQuoteVO;
import com.bank.bankfinance.vo.StockVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private StockService stockService;

    @Autowired
    private KlineService klineService;

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

    /**
     * 实时 K 线。
     *
     * <p>柱子的 {@code source} 说明数据来源：{@code remote} 真实历史 / {@code trade} 本系统真实成交 /
     * {@code quote} 实时行情合成的当前柱 / {@code simulated} 历史不足时补齐的演示数据；
     * 当前这根未走完的柱子 {@code live=true}。
     *
     * @param period 1min / 5min / 1day / 1week / 1month，默认 1day
     * @param limit  返回条数，默认取 stock.kline.default-limit
     */
    @GetMapping("/kline/{stockCode}")
    public Result<List<StockKlineVO>> kline(@PathVariable String stockCode,
                                            @RequestParam(defaultValue = "1day") String period,
                                            @RequestParam(required = false) Integer limit) {
        return Result.success(klineService.getKline(stockCode, period, limit));
    }

    // 修复：下单接口此前无限流，可被脚本高频刷单，制造撮合压力与资金冻结噪声。
    @PostMapping("/order")
    @RateLimit(maxRequests = 20, timeWindow = 1, timeUnit = TimeUnit.MINUTES, message = "下单过于频繁，请稍后再试")
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

    // 修复：撤单接口此前无限流；撤单要抢分布式锁，高频调用会拖慢撮合。
    @PostMapping("/cancel/{id}")
    @RateLimit(maxRequests = 20, timeWindow = 1, timeUnit = TimeUnit.MINUTES, message = "操作过于频繁，请稍后再试")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam String accountNumber) {
        stockService.cancelOrder(id, accountNumber);
        return Result.success();
    }
}