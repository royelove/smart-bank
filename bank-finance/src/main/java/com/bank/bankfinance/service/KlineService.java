package com.bank.bankfinance.service;

import com.bank.bankfinance.vo.StockKlineVO;

import java.util.List;

/**
 * 股票 K 线。
 *
 * <p>数据来源优先级（后者优先覆盖前者）：
 * <ol>
 *   <li>远端真实历史（腾讯，可选，见 {@code stock.kline.remote-history}）</li>
 *   <li>本地确定性补齐（历史不足时，标记 {@code simulated}）</li>
 *   <li>本系统真实撮合成交（{@code t_stock_trade} 聚合，标记 {@code trade}）</li>
 *   <li>实时行情最新价（当前这根未走完的柱子，标记 {@code quote}）</li>
 * </ol>
 */
public interface KlineService {

    /**
     * 取某只股票最近若干个周期的 K 线（时间升序）。
     *
     * @param stockCode 股票代码
     * @param period    周期：1min / 5min / 1day / 1week / 1month
     * @param limit     返回条数，为空取配置默认值，超过上限会被截断
     */
    List<StockKlineVO> getKline(String stockCode, String period, Integer limit);
}
