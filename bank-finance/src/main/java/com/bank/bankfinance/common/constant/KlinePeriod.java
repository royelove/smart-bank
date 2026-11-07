package com.bank.bankfinance.common.constant;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * K 线周期：定义「一根柱子覆盖多长时间」以及它落在哪个时间点上。
 *
 * <p>柱子的身份 = 周期起始时间（{@link #startOf} 的结果），因此
 * 「股票 + 周期 + kline_time」可以唯一定位一根柱子。
 */
public enum KlinePeriod {

    MIN1("1min", true) {
        @Override
        public LocalDateTime startOf(LocalDateTime t) {
            return t.truncatedTo(ChronoUnit.MINUTES);
        }

        @Override
        public LocalDateTime previous(LocalDateTime bucket) {
            return bucket.minusMinutes(1);
        }
    },
    MIN5("5min", true) {
        @Override
        public LocalDateTime startOf(LocalDateTime t) {
            return t.truncatedTo(ChronoUnit.HOURS).plusMinutes((t.getMinute() / 5) * 5L);
        }

        @Override
        public LocalDateTime previous(LocalDateTime bucket) {
            return bucket.minusMinutes(5);
        }
    },
    DAY1("1day", false) {
        @Override
        public LocalDateTime startOf(LocalDateTime t) {
            return t.toLocalDate().atStartOfDay();
        }

        @Override
        public LocalDateTime previous(LocalDateTime bucket) {
            return bucket.minusDays(1);
        }
    },
    WEEK1("1week", false) {
        @Override
        public LocalDateTime startOf(LocalDateTime t) {
            return t.toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay();
        }

        @Override
        public LocalDateTime previous(LocalDateTime bucket) {
            return bucket.minusWeeks(1);
        }
    },
    MONTH1("1month", false) {
        @Override
        public LocalDateTime startOf(LocalDateTime t) {
            return t.toLocalDate().withDayOfMonth(1).atStartOfDay();
        }

        @Override
        public LocalDateTime previous(LocalDateTime bucket) {
            return bucket.minusMonths(1);
        }
    };

    private final String code;
    private final boolean intraday;

    KlinePeriod(String code, boolean intraday) {
        this.code = code;
        this.intraday = intraday;
    }

    /** 周期起始时间（柱子身份） */
    public abstract LocalDateTime startOf(LocalDateTime t);

    /** 上一个周期的起始时间 */
    public abstract LocalDateTime previous(LocalDateTime bucket);

    /** 是否是日内周期（分钟级） */
    public boolean isIntraday() {
        return intraday;
    }

    public String getCode() {
        return code;
    }

    /** 解析周期；无法识别返回 {@code null}，由调用方决定抛什么业务码 */
    public static KlinePeriod of(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim().toLowerCase();
        for (KlinePeriod p : values()) {
            if (p.code.equals(normalized)) {
                return p;
            }
        }
        return null;
    }

    /** 全部受支持的周期码，用于错误提示 */
    public static String supportedCodes() {
        return Arrays.stream(values()).map(KlinePeriod::getCode).collect(Collectors.joining("/"));
    }
}
