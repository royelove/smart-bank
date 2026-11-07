package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * K 线单根柱子（蜡烛）。
 *
 * <p>{@link #source} 用于区分这根柱子的数据来源，避免把补齐用的模拟数据当成真实成交展示给用户：
 * <ul>
 *   <li>{@code trade}     —— 由本系统撮合产生的真实成交（t_stock_trade）聚合而来</li>
 *   <li>{@code quote}     —— 由实时行情（t_stock 最新价）合成，未产生成交时的当前柱</li>
 *   <li>{@code simulated} —— 历史数据不足时按确定性算法补齐的演示数据</li>
 * </ul>
 */
@Getter
@Setter
public class StockKlineVO {

    private String stockCode;
    private String stockName;
    /** 周期：1min / 5min / 1day / 1week / 1month */
    private String period;
    private BigDecimal openPrice;
    private BigDecimal closePrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    /** 成交量（手） */
    private Long volume;
    /** 成交额（元） */
    private BigDecimal amount;
    /** 该柱所属周期的起始时间 */
    private LocalDateTime klineTime;
    /** 数据来源：trade / quote / simulated */
    private String source;

    /** 是否处于当前（尚未走完）的周期，前端可用来闪烁提示"实时" */
    private Boolean live;
}
