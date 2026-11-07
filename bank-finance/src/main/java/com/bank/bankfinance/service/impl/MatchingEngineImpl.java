package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.entity.OrderBookEntry;
import com.bank.bankfinance.entity.Position;
import com.bank.bankfinance.entity.StockTrade;
import com.bank.bankfinance.common.util.TicketNoUtil;
import com.bank.bankfinance.mapper.OrderBookEntryMapper;
import com.bank.bankfinance.mapper.PositionMapper;
import com.bank.bankfinance.mapper.StockTradeMapper;
import com.bank.bankfinance.service.AccountService;
import com.bank.bankfinance.service.MatchingEngine;
import com.bank.bankfinance.event.TradeExecutedEvent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MatchingEngineImpl implements MatchingEngine {

    private static final Logger log = LoggerFactory.getLogger(MatchingEngineImpl.class);

    @Autowired private OrderBookEntryMapper orderBookMapper;
    @Autowired private StockTradeMapper stockTradeMapper;
    @Autowired private AccountService accountService;
    @Autowired private PositionMapper positionMapper;
    @Autowired private ApplicationEventPublisher eventPublisher;

    /**
     * 撮合逻辑本身不再加锁：加锁由调用方（StockServiceImpl.placeOrder）在事务之外完成，
     * 保证锁的生命周期覆盖整个事务提交，避免锁释放后、提交前被并发读取到旧订单簿。
     */
    @Transactional
    @Override
    public List<StockTrade> match(String stockCode) {
        return doMatch(stockCode);
    }

    private List<StockTrade> doMatch(String stockCode) {
        List<OrderBookEntry> buyOrders = orderBookMapper.selectList(
            new LambdaQueryWrapper<OrderBookEntry>()
                .eq(OrderBookEntry::getStockCode, stockCode)
                .eq(OrderBookEntry::getDirection, "BUY")
                .in(OrderBookEntry::getStatus, 0, 1)
                .orderByDesc(OrderBookEntry::getPrice)
                .orderByAsc(OrderBookEntry::getCreateTime)
        );

        List<OrderBookEntry> sellOrders = orderBookMapper.selectList(
            new LambdaQueryWrapper<OrderBookEntry>()
                .eq(OrderBookEntry::getStockCode, stockCode)
                .eq(OrderBookEntry::getDirection, "SELL")
                .in(OrderBookEntry::getStatus, 0, 1)
                .orderByAsc(OrderBookEntry::getPrice)
                .orderByAsc(OrderBookEntry::getCreateTime)
        );

        List<StockTrade> trades = new ArrayList<>();
        int buyIdx = 0, sellIdx = 0;

        while (buyIdx < buyOrders.size() && sellIdx < sellOrders.size()) {
            OrderBookEntry buy = buyOrders.get(buyIdx);
            OrderBookEntry sell = sellOrders.get(sellIdx);

            if (buy.getPrice().compareTo(sell.getPrice()) < 0) {
                break;
            }

            /*
             * 修复：新增自成交防护。同一账户的买卖单互相撮合会凭空制造成交量与「自买自卖」记录，
             * 而且买方结算（释放冻结+扣钱）与卖方入账落在同一账户上，持仓均价与手续费口径全乱。
             * 跳过的方式是推进剩余量更大的一方，保证每轮至少有一个下标前进、循环必然终止。
             */
            if (buy.getAccountNumber().equals(sell.getAccountNumber())) {
                log.warn("检测到自成交，跳过配对 stock={} account={} buyOrder={} sellOrder={}",
                        stockCode, buy.getAccountNumber(), buy.getOrderNo(), sell.getOrderNo());
                if (buy.getRemainingQuantity() >= sell.getRemainingQuantity()) {
                    buyIdx++;
                } else {
                    sellIdx++;
                }
                continue;
            }

            BigDecimal tradePrice = sell.getPrice();
            int tradeQty = Math.min(buy.getRemainingQuantity(), sell.getRemainingQuantity());
            if (tradeQty <= 0) {
                // 理论上不会出现（双方剩余量都 >0 才会进到这里），保底推进避免死循环
                if (buy.getRemainingQuantity() <= 0) buyIdx++;
                if (sell.getRemainingQuantity() <= 0) sellIdx++;
                continue;
            }

            StockTrade trade = buildTrade(buy, sell, tradePrice, tradeQty);
            stockTradeMapper.insert(trade);
            trades.add(trade);

            updateOrder(buy, tradeQty);
            updateOrder(sell, tradeQty);
            settle(buy, sell, tradePrice, tradeQty);

            eventPublisher.publishEvent(new TradeExecutedEvent(trade, buy, sell));

            if (buy.getRemainingQuantity() == 0) buyIdx++;
            if (sell.getRemainingQuantity() == 0) sellIdx++;
        }
        return trades;
    }

    private StockTrade buildTrade(OrderBookEntry buy, OrderBookEntry sell, BigDecimal price, int qty) {
        StockTrade trade = new StockTrade();
        /*
         * 修复：原为 "TRD" + System.currentTimeMillis()。同一轮撮合里多笔成交会落在同一毫秒，
         * 生成完全相同的 trade_no，撞上 t_stock_trade 的 uk_trade_no 唯一键，整个撮合事务回滚——
         * 表现为「明明可以成交却报系统繁忙」。改用与转账/存取款同一套单号生成器。
         */
        trade.setTradeNo(TicketNoUtil.tradeNo());
        trade.setStockCode(buy.getStockCode());
        trade.setStockName(buy.getStockName());
        trade.setBuyOrderNo(buy.getOrderNo());
        trade.setSellOrderNo(sell.getOrderNo());
        trade.setBuyAccount(buy.getAccountNumber());
        trade.setSellAccount(sell.getAccountNumber());
        trade.setTradePrice(price);
        trade.setTradeQuantity(qty);
        trade.setTradeAmount(price.multiply(BigDecimal.valueOf(qty)));
        trade.setTradeTime(LocalDateTime.now());
        return trade;
    }

    private void updateOrder(OrderBookEntry order, int filledQty) {
        int newFilled = order.getFilledQuantity() + filledQty;
        int newRemaining = order.getRemainingQuantity() - filledQty;
        int newStatus = (newRemaining == 0) ? 2 : 1;

        OrderBookEntry update = new OrderBookEntry();
        update.setId(order.getId());
        update.setFilledQuantity(newFilled);
        update.setRemainingQuantity(newRemaining);
        update.setStatus(newStatus);
        // 传当前版本号交给 MyBatis-Plus 乐观锁插件自动生成 SET version=version+1 / WHERE version=旧值
        update.setVersion(order.getVersion());

        int rows = orderBookMapper.updateById(update);
        if (rows == 0) {
            throw new RuntimeException("乐观锁冲突，订单被并发修改: " + order.getOrderNo());
        }

        order.setFilledQuantity(newFilled);
        order.setRemainingQuantity(newRemaining);
        order.setStatus(newStatus);
        order.setVersion(order.getVersion() + 1);
    }

    private void settle(OrderBookEntry buy, OrderBookEntry sell, BigDecimal price, int qty) {
        BigDecimal amount = price.multiply(BigDecimal.valueOf(qty));
        // 买方：释放委托价冻结，扣实际成交额（成交价 <= 委托价，差额自动回到可用余额）
        BigDecimal frozen = buy.getPrice().multiply(BigDecimal.valueOf(qty));
        /*
         * 修复：原调用忽略返回值，而该方法原本是 void。若释放冻结/扣款那条 UPDATE 实际影响了
         * 0 行（冻结额或余额不足），代码仍会继续给买方加持仓、给卖方入账——即「白送持仓」。
         * 现在结算未生效就直接抛异常回滚整个撮合事务。
         */
        if (!accountService.settleBuyFunds(buy.getAccountNumber(), frozen, amount)) {
            throw new IllegalStateException(String.format(
                    "买入结算失败（冻结额或余额不足），回滚撮合: account=%s order=%s frozen=%s actual=%s",
                    buy.getAccountNumber(), buy.getOrderNo(), frozen, amount));
        }
        accountService.creditBalance(sell.getAccountNumber(), amount);
        mergePosition(buy.getAccountNumber(), buy.getStockCode(), buy.getStockName(), qty, price);
        reducePosition(sell.getAccountNumber(), sell.getStockCode(), qty);
    }

    private void mergePosition(String account, String stockCode, String stockName, int qty, BigDecimal price) {
        Position pos = positionMapper.selectOne(
            new LambdaQueryWrapper<Position>()
                .eq(Position::getAccountNumber, account)
                .eq(Position::getStockCode, stockCode)
        );
        if (pos == null) {
            pos = new Position();
            pos.setAccountNumber(account);
            pos.setStockCode(stockCode);
            pos.setStockName(stockName);
            pos.setQuantity(qty);
            pos.setAvgCost(price);
            positionMapper.insert(pos);
        } else {
            int newQty = pos.getQuantity() + qty;
            BigDecimal newAvg = pos.getAvgCost().multiply(BigDecimal.valueOf(pos.getQuantity()))
                    .add(price.multiply(BigDecimal.valueOf(qty)))
                    .divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);
            pos.setQuantity(newQty);
            pos.setAvgCost(newAvg);
            positionMapper.updateById(pos);
        }
    }

    private void reducePosition(String account, String stockCode, int qty) {
        // 成交同时扣减持仓与冻结股数（卖出委托已在下单时冻结）
        int rows = positionMapper.settleSell(account, stockCode, qty);
        if (rows == 0) {
            // 持仓或冻结股数不足时抛异常回滚整个撮合事务，绝不留下「卖方白卖、买方白拿」的半成品
            throw new IllegalStateException(String.format(
                    "卖出持仓结算失败（持仓或冻结股数不足），回滚撮合: account=%s stock=%s qty=%d",
                    account, stockCode, qty));
        }
        positionMapper.delete(new LambdaQueryWrapper<Position>()
                .eq(Position::getAccountNumber, account)
                .eq(Position::getStockCode, stockCode)
                .eq(Position::getQuantity, 0));
    }

    @Override
    public boolean canMatch(BigDecimal buyPrice, BigDecimal sellPrice) {
        return buyPrice.compareTo(sellPrice) >= 0;
    }
}