package com.bank.bankfinance.service;

import com.bank.bankfinance.vo.TicketAnalysisVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiService {
    String chat(String message);
    void chatStream(String message, SseEmitter emitter);

    /**
     * 工单情绪与合理性分析（大模型优先，失败自动回退规则引擎）。
     *
     * @param title       工单标题
     * @param description 工单描述
     * @return 分析结果（情绪 / 有效性 / 评分 / 评估意见）
     */
    TicketAnalysisVO analyzeTicket(String title, String description);
}
