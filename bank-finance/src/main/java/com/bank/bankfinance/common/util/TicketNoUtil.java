package com.bank.bankfinance.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 业务单号生成器：前缀 + 时间戳(yyyyMMddHHmmss) + 6位随机数。
 *
 * 工单号：TK20260803123045A1B2C3
 * 交易号：T20260803123045A1B2C3
 */
public class TicketNoUtil {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String HEX = "0123456789ABCDEF";

    private TicketNoUtil() {
    }

    public static String ticketNo() {
        return "TK" + timestamp() + randomHex(6);
    }

    public static String tradeNo() {
        return "T" + timestamp() + randomHex(6);
    }

    public static String orderNo() {
        return "O" + timestamp() + randomHex(6);
    }

    public static String requestId() {
        return "REQ" + timestamp() + randomHex(8);
    }

    private static String timestamp() {
        return LocalDateTime.now().format(TS);
    }

    private static String randomHex(int n) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(HEX.charAt(r.nextInt(16)));
        }
        return sb.toString();
    }
}
