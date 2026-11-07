package com.bank.bankfinance.controller;

import com.bank.bankfinance.common.result.Result;
import com.bank.bankfinance.entity.StockRank;
import com.bank.bankfinance.service.RankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rank")
public class RankController {

    @Autowired private RankService rankService;

    @GetMapping("/top/{rankType}/{n}")
    public Result<List<StockRank>> getTopN(
            @PathVariable String rankType,
            @PathVariable int n) {
        return Result.success(rankService.getRankList(rankType, n));
    }

    @GetMapping("/detail/{stockCode}/{rankType}")
    public Result<StockRank> getDetail(
            @PathVariable String stockCode,
            @PathVariable String rankType) {
        return Result.success(rankService.getRankDetail(stockCode, rankType));
    }

    @GetMapping("/all/top/{n}")
    public Result<Map<String, List<StockRank>>> getAllTopN(@PathVariable int n) {
        return Result.success(rankService.getAllRankTopN(n));
    }

    @GetMapping("/rank/{stockCode}/{rankType}")
    public Result<Integer> getRank(
            @PathVariable String stockCode,
            @PathVariable String rankType) {
        return Result.success(rankService.getStockRank(stockCode, rankType));
    }

    @GetMapping("/score/{stockCode}/{rankType}")
    public Result<Double> getScore(
            @PathVariable String stockCode,
            @PathVariable String rankType) {
        return Result.success(rankService.getRankScore(stockCode, rankType));
    }

    @PostMapping("/refresh")
    public Result<Void> refresh() {
        rankService.refreshAllRank();
        return Result.success();
    }
}