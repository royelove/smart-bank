package com.bank.bankfinance.common.util;

import com.bank.bankfinance.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 认证/授权失败的统一 JSON 出口。
 *
 * <p>新增原因：让鉴权失败与业务失败返回<b>同一种响应体结构</b>
 * （{@code {"code":"...","msg":"...","data":null}}）。
 *
 * <p>在它出现之前有两个问题：
 * <ol>
 *   <li>{@code JwtAuthenticationFilter} 是手写字符串拼接的 JSON，没有 {@code data} 字段，
 *       与 {@code GlobalExceptionHandler} 的输出结构不一致；</li>
 *   <li>更关键的是 {@code SecurityConfig} 里<b>没有任何异常出口配置</b>：Spring Security
 *       在「未认证」时使用默认的 {@code Http403ForbiddenEntryPoint}，返回的是
 *       <b>HTTP 403 加空响应体</b>。前端拿不到 {@code code}，既无法区分「未登录」与「无权限」，
 *       也无法触发「跳转登录页」的逻辑——授权是收紧了，但错误语义丢了。</li>
 * </ol>
 */
public final class AuthResponseWriter {

    private AuthResponseWriter() {
    }

    /** 未认证：401 + {@code Result.fail(...)} 的 JSON 结构 */
    public static void unauthorized(HttpServletResponse response, ObjectMapper objectMapper,
                                    Result<?> body) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED, objectMapper, body);
    }

    /** 已认证但权限不足：403 + {@code Result.fail(...)} 的 JSON 结构 */
    public static void forbidden(HttpServletResponse response, ObjectMapper objectMapper,
                                 Result<?> body) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN, objectMapper, body);
    }

    private static void write(HttpServletResponse response, int status,
                              ObjectMapper objectMapper, Result<?> body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
