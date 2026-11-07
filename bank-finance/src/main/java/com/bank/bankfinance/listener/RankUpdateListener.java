package com.bank.bankfinance.listener;
import com.bank.bankfinance.event.RankUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RankUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(RankUpdateListener.class);

    @EventListener
    public void onRankUpdated(RankUpdatedEvent event) {
        log.info("排名已刷新: type={}", event.getRankType());
    }
}