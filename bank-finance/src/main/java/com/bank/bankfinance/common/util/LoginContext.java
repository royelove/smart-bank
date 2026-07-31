package com.bank.bankfinance.common.util;

public class LoginContext {
    private static ThreadLocal<String> ACCOUNT_THREAD_LOCAL = new ThreadLocal<>();
    private static ThreadLocal<Long> USER_ID_THREAD_LOCAL = new ThreadLocal<>();

    public static void setAccount(String account) {
        ACCOUNT_THREAD_LOCAL.set(account);
    }
    public static String getAccount() {
        return ACCOUNT_THREAD_LOCAL.get();
    }
    public static void setUserId(Long userId) {
        USER_ID_THREAD_LOCAL.set(userId);
    }
    public static Long getUserId() {
        return USER_ID_THREAD_LOCAL.get();
    }
    public static void clear() {
        ACCOUNT_THREAD_LOCAL.remove();
        USER_ID_THREAD_LOCAL.remove();
    }
    LoginContext(){}
}
