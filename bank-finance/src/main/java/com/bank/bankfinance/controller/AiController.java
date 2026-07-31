package com.bank.bankfinance.controller;

import com.bank.bankfinance.common.result.Result;
import com.bank.bankfinance.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    public Result<String> chat(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return Result.fail("400", "消息不能为空");
        }
        String reply = aiService.chat(message);
        return Result.success(reply);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        SseEmitter emitter = new SseEmitter(120000L);
        if (message == null || message.isBlank()) {
            try {
                emitter.send(SseEmitter.event().name("message").data("消息不能为空"));
                emitter.send(SseEmitter.event().name("done").data(""));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        aiService.chatStream(message, emitter);
        return emitter;
    }
}
