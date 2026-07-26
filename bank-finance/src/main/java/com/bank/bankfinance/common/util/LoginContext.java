package com.bank.bankfinance.common.util;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginContext {
    private static ThreadLocal<String> ACCOUNT_THREAD_LOCAL = new ThreadLocal<>();
    public static void setAccount(String account) {
        ACCOUNT_THREAD_LOCAL.set(account);
    }
    public static String getAccount() {
        return ACCOUNT_THREAD_LOCAL.get();
    }
    public static void clear() {
        ACCOUNT_THREAD_LOCAL.remove();
    }
    LoginContext(){}
}
