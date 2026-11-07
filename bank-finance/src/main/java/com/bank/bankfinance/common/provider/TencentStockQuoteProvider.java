package com.bank.bankfinance.common.provider;

import com.bank.bankfinance.entity.Stock;
import com.bank.bankfinance.vo.StockQuoteVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 腾讯行情接口
 * API: https://qt.gtimg.cn/q=sh600519,sz000001
 * 响应格式: v_sh600519="1~贵州茅台~600519~1480.00~1470.00~..."
 * 字段用 ~ 分隔
 */
@Component
public class TencentStockQuoteProvider implements StockQuoteProvider {

    private static final Logger log = LoggerFactory.getLogger(TencentStockQuoteProvider.class);
    private static final String TENCENT_API = "https://qt.gtimg.cn/q=";
    // 腾讯 qt 接口响应头声明 charset=GBK，必须按 GBK 解码，否则股票名会变成乱码
    private static final Charset QUOTE_CHARSET = Charset.forName("GBK");

    @Override
    public String name() {
        return "tencent";
    }

    @Override
    public StockQuoteVO quote(Stock stock) {
        String symbol = toSymbol(stock.getStockCode(), stock.getMarket());
        String line = fetchLine(symbol);
        if (line == null) return null;
        return parseLine(stock, line);
    }

    @Override
    public List<StockQuoteVO> batchQuote(List<Stock> stocks) {
        if (stocks == null || stocks.isEmpty()) return new ArrayList<>();
        String symbols = stocks.stream()
                .map(s -> toSymbol(s.getStockCode(), s.getMarket()))
                .collect(Collectors.joining(","));
        List<String> lines = fetchLines(symbols);
        Map<String, Stock> stockMap = stocks.stream()
                .collect(Collectors.toMap(Stock::getStockCode, s -> s));
        List<StockQuoteVO> result = new ArrayList<>();
        for (String line : lines) {
            String code = extractCode(line);
            Stock stock = stockMap.get(code);
            if (stock != null) {
                StockQuoteVO vo = parseLine(stock, line);
                if (vo != null) result.add(vo);
            }
        }
        return result;
    }

    private String toSymbol(String stockCode, String market) {
        if ("沪".equals(market) || stockCode.startsWith("6")) {
            return "sh" + stockCode;
        }
        return "sz" + stockCode;
    }

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String fetchLine(String symbol) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TENCENT_API + symbol))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(QUOTE_CHARSET));
            if (response.statusCode() != 200) {
                log.warn("腾讯行情请求失败, symbol={}, code={}", symbol, response.statusCode());
                return null;
            }
            String body = response.body();
            return body != null && !body.isEmpty() ? body.split(";")[0] : null;
        } catch (Exception e) {
            log.error("腾讯行情请求异常, symbol={}", symbol, e);
            return null;
        }
    }

    private List<String> fetchLines(String symbols) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TENCENT_API + symbols))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(QUOTE_CHARSET));
            if (response.statusCode() != 200) {
                log.warn("腾讯行情批量请求失败, code={}", response.statusCode());
                return new ArrayList<>();
            }
            List<String> lines = new ArrayList<>();
            for (String line : response.body().split(";")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) lines.add(trimmed);
            }
            return lines;
        } catch (Exception e) {
            log.error("腾讯行情批量请求异常", e);
            return new ArrayList<>();
        }
    }

    private String extractCode(String line) {
        // v_sh600519="1~贵州茅台~600519~..."  →  600519
        // 从变量名截取（varPart.substring(10)）不可靠：真实变量名 v_sh600519 只有 10 位，
        // 旧逻辑要求 >=12 位再截断，导致每行都返回空串、行情列表恒为空。改为直接取报文 [2] 字段。
        int start = line.indexOf('"');
        int end = line.lastIndexOf('"');
        if (start < 0 || end <= start) return "";
        String[] fields = line.substring(start + 1, end).split("~");
        return fields.length > 2 ? fields[2].trim() : "";
    }

    /**
     * 腾讯行情字段（~分隔）：
     * [0]  未知(0/1/51)
     * [1]  股票名称
     * [2]  股票代码
     * [3]  当前价格
     * [4]  昨收
     * [5]  今开
     * [6]  成交量（手）
     * [7]  外盘
     * [8]  内盘
     * [9]  买一价
     * [10] 买一量
     * [11] 买二价
     * [12] 买二量
     * [13] 买三价
     * [14] 买三量
     * [15] 买四价
     * [16] 买四量
     * [17] 买五价
     * [18] 买五量
     * [19] 卖一价
     * [20] 卖一量
     * [21] 卖二价
     * [22] 卖二量
     * [23] 卖三价
     * [24] 卖三量
     * [25] 卖四价
     * [26] 卖四量
     * [27] 卖五价
     * [28] 卖五量
     * [29] 最近逐笔成交
     * [30] 时间
     * [31] 涨跌
     * [32] 涨跌幅(%)
     * [33] 最高
     * [34] 最低
     * [35] 价格/成交量/成交额
     * [36] 成交量（手）
     * [37] 成交额（万元）
     * [38] 换手率(%)
     */
    private StockQuoteVO parseLine(Stock stock, String line) {
        try {
            int start = line.indexOf('"');
            int end = line.lastIndexOf('"');
            if (start < 0 || end <= start) return null;
            String data = line.substring(start + 1, end);
            if (data.isEmpty()) return null;

            String[] fields = data.split("~");
            if (fields.length < 38) return null;

            String name = fields[1];
            BigDecimal currentPrice = safeParse(fields[3]);
            BigDecimal prevClose = safeParse(fields[4]);
            BigDecimal highPrice = safeParse(fields[33]);
            BigDecimal lowPrice = safeParse(fields[34]);
            long volume = safeParseLong(fields[36]);
            BigDecimal amount = safeParse(fields[37]);

            if (currentPrice.signum() <= 0) return null;

            BigDecimal changePercent = BigDecimal.ZERO;
            if (prevClose != null && prevClose.signum() > 0) {
                changePercent = currentPrice.subtract(prevClose)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(prevClose, 2, RoundingMode.HALF_UP);
            }

            stock.setCurrentPrice(currentPrice);
            stock.setPrevClose(prevClose);
            stock.setHighPrice(highPrice);
            stock.setLowPrice(lowPrice);
            stock.setVolume(volume);
            stock.setAmount(amount.multiply(BigDecimal.valueOf(10000)));
            stock.setChangePercent(changePercent);

            StockQuoteVO vo = new StockQuoteVO();
            vo.setStockCode(stock.getStockCode());
            vo.setStockName(name);
            vo.setCurrentPrice(currentPrice);
            vo.setPrevClose(prevClose);
            vo.setChangePercent(changePercent);
            vo.setHighPrice(highPrice);
            vo.setLowPrice(lowPrice);
            vo.setVolume(volume);
            vo.setAmount(amount.multiply(BigDecimal.valueOf(10000)));
            vo.setQuoteTime(LocalDateTime.now());
            return vo;
        } catch (Exception e) {
            log.error("解析行情失败, stockCode={}", stock.getStockCode(), e);
            return null;
        }
    }

    private BigDecimal safeParse(String s) {
        try {
            if (s == null || s.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private long safeParseLong(String s) {
        try {
            if (s == null || s.isEmpty()) return 0L;
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0L;
        }
    }
}
