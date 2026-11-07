package com.bank.bankfinance.schedule;

import com.bank.bankfinance.service.RankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class RankSchedule {

    @Autowired private RankService rankService;
    @Autowired @Qualifier("rankPool") private Executor rankPool;

    @Scheduled(fixedDelay = 5000)
    public void refreshRank() {
        CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> rankService.refreshRank("change"), rankPool);
        CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> rankService.refreshRank("fall"), rankPool);
        CompletableFuture<Void> f3 = CompletableFuture.runAsync(() -> rankService.refreshRank("volume"), rankPool);
        CompletableFuture<Void> f4 = CompletableFuture.runAsync(() -> rankService.refreshRank("amount"), rankPool);
        CompletableFuture<Void> f5 = CompletableFuture.runAsync(() -> rankService.refreshRank("turnover"), rankPool);
        CompletableFuture<Void> f6 = CompletableFuture.runAsync(() -> rankService.refreshRank("amplitude"), rankPool);
        CompletableFuture.allOf(f1, f2, f3, f4, f5, f6).join();
    }
}
