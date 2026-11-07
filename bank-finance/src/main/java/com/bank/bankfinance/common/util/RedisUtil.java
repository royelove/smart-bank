package com.bank.bankfinance.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }
    /*
        setIfAbsent是Spring Data Redis库里面封装的一个方法，可用于实现分布式锁，防止缓存击穿，实现幂等性
     */
    public boolean setIfAbsent(String key,Object value,long timeout,TimeUnit unit){
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    /**
     * 执行 Lua 脚本。
     *
     * <p>新增：用于把「判断 + 自增 + 首次设置 TTL」合并成一次原子操作。
     * 原先用 {@code increment} 之后再单独 {@code expire}，两步之间发生异常/重启会让 key
     * 永久失去 TTL（详见 {@code RateLimitAspect} 的修复记录）。
     */
    @SuppressWarnings("unchecked")
    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        // Lua 脚本的 ARGV 必须是「裸字符串/数字」。若走 RedisTemplate 默认的 value 序列化器
        // （GenericJackson2JsonRedisSerializer），字符串 "60" 会被序列化成带引号的 "\"60\""，
        // 脚本里 tonumber(ARGV[1]) 得到 nil，EXPIRE 拿不到秒数 → key 永远没有 TTL →
        // 限流计数永久累积，一旦超限就再也无法自愈。这里显式用 StringRedisSerializer 序列化参数，
        // 结果仍用 value 序列化器还原（脚本返回整数，行为与原来一致）。
        return redisTemplate.execute(script, new StringRedisSerializer(),
                (RedisSerializer<T>) redisTemplate.getValueSerializer(), keys, args);
    }
}
