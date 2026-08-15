package com.bank.bankfinance.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
/*
配置类，经过Spring注册后根据是否有@Autowired注册
 */
public class RedisConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
    /*
    该对象返回时要注册为Spring Bean
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       ObjectMapper objectMapper) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        /*
        connectionFactory是方法参数，Spring封装了他的信息，在application.yml有封装.
        redis:
        host: localhost
        port: 6379
        database: 0
        lettuce:
        pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        相当于
        this.connectionFactory = connectionFactory
         */
        redisTemplate.setConnectionFactory(connectionFactory);
        // 开启默认类型信息:序列化时写入 @class,反序列化才能还原成原对象(如 Account/StockQuote),
        // 否则读出来是 LinkedHashMap,强转会报 ClassCastException
        ObjectMapper redisMapper = objectMapper.copy();
        redisMapper.activateDefaultTyping(
                redisMapper.getPolymorphicTypeValidator(),
                DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisMapper);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(serializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
