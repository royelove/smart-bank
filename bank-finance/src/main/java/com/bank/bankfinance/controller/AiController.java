package com.bank.bankfinance.controller;

import com.bank.bankfinance.common.result.Result;
import com.bank.bankfinance.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}
