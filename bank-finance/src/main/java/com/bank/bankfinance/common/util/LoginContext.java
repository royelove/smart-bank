package com.bank.bankfinance.common.util;

public class LoginContext {
    private static ThreadLocal<String> ACCOUNT_THREAD_LOCAL = new ThreadLocal<>();
    private static ThreadLocal<Long> USER_ID_THREAD_LOCAL = new ThreadLocal<>();
    private static ThreadLocal<Long> STAFF_ID_THREAD_LOCAL = new ThreadLocal<>();

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
    public static void setStaffId(Long staffId) {
        STAFF_ID_THREAD_LOCAL.set(staffId);
    }
    public static Long getStaffId() {
        return STAFF_ID_THREAD_LOCAL.get();
    }
    public static void clear() {
        ACCOUNT_THREAD_LOCAL.remove();
        USER_ID_THREAD_LOCAL.remove();
        STAFF_ID_THREAD_LOCAL.remove();
    }
    LoginContext(){}
}
