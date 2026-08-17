package com.bank.bankfinance.common.provider;

import com.bank.bankfinance.entity.Stock;
import com.bank.bankfinance.vo.StockQuoteVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟行情：基于当前价做 ±2% 随机游走
 */
@Component
public class SimulatedStockQuoteProvider implements StockQuoteProvider {

    @Override
    public String name() {
        return "simulated";
    }

    @Override
    public StockQuoteVO quote(Stock stock) {
        BigDecimal prevClose = stock.getPrevClose();
        BigDecimal base = stock.getCurrentPrice() == null ? prevClose : stock.getCurrentPrice();
        double factor = 1 + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.04;
        BigDecimal price = base.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
        if (price.signum() <= 0) {
            price = BigDecimal.valueOf(0.01);
        }
        BigDecimal change = prevClose != null && prevClose.signum() > 0
                ? price.subtract(prevClose).multiply(BigDecimal.valueOf(100))
                        .divide(prevClose, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        StockQuoteVO vo = new StockQuoteVO();
        vo.setStockCode(stock.getStockCode());
        vo.setStockName(stock.getStockName());
        vo.setCurrentPrice(price);
        vo.setPrevClose(prevClose);
        vo.setChangePercent(change);
        vo.setQuoteTime(LocalDateTime.now());
        return vo;
    }
}