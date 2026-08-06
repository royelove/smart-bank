package com.bank.bankfinance.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/*
    用来生成Token,并由 JwtAuthenticationFilter 在每次请求中解析验证
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        // 将字符串密钥转为 HMAC-SHA 密钥对象
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成 JWT Token
     * @param accountNumber 用户标识（如账号）
     * @return JWT 字符串
     */
    public String generateToken(String accountNumber) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setSubject(accountNumber)          // 存放用户账号
                .setIssuedAt(now)                   // 签发时间
                .setExpiration(expiryDate)          // 过期时间
                .signWith(getSigningKey())          // 签名算法（默认 HS256）
                .compact();
    }

    /**
     * 解析 Token，提取用户账号
     * @param token JWT 字符串
     * @return 账号（如果解析失败会抛出异常，调用方需捕获）
     */
    public String parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * 验证 Token 是否有效（可选）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}