package com.bank.bankfinance.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiService {
    String chat(String message);
    void chatStream(String message, SseEmitter emitter);
}
