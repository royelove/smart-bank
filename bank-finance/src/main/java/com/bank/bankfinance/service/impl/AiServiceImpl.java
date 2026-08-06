package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.common.ai.TicketAiAnalyzer;
import com.bank.bankfinance.config.AiProperties;
import com.bank.bankfinance.service.AiService;
import com.bank.bankfinance.vo.TicketAnalysisVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SYSTEM_PROMPT = "你是钱钱银行的智能客服助手。系统支持的功能：开户（开通银行账户）、登录、存款、取款、转账（需输入对方账号、金额并确认交易密码）、交易记录查询、理财产品购买与提前赎回、账户冻结/解冻、修改密码、切换银行卡。请用简洁、友好、专业的中文回答用户问题，回答控制在200字以内。";

    private static final String TICKET_ANALYSIS_SYSTEM = "你是钱钱银行的工单智能审核助手。请对用户提交的服务工单做情绪识别与合理性评估，只输出一个 JSON 对象，禁止输出任何其他内容。\n" +
            "JSON 字段：{\"sentiment\": \"积极|消极|中性|恶意\", \"valid\": 0或1, \"score\": 0到100的整数, \"eval\": \"一句话中文评估意见\"}\n" +
            "判定规则：\n" +
            "- 恶意：包含辱骂、人身攻击、威胁性内容 -> valid=0, sentiment=恶意\n" +
            "- 无价值/灌水：内容过短、重复字符、无实质诉求 -> valid=0\n" +
            "- 有效：有明确业务诉求（退钱/盗刷/冻结/未到账/手续费/多扣款等）-> valid=1\n" +
            "- score 为合理性与紧急程度综合打分，越高越应优先处理\n" +
            "示例1：标题\"垃圾\"，描述\"哈哈哈\" -> {\"sentiment\":\"中性\",\"valid\":0,\"score\":5,\"eval\":\"内容为灌水，无实质诉求，判定无效工单。\"}\n" +
            "示例2：标题\"被多扣了100元\"，描述\"昨天转账后账户被多扣了100元，请帮我查一下并退回\" -> {\"sentiment\":\"消极\",\"valid\":1,\"score\":85,\"eval\":\"明确的多扣款诉求，情绪消极，建议优先核实处理。\"}";

    @Autowired
    private AiProperties aiProperties;

    @Override
    public String chat(String message) {
        return mockReply(message);
    }

    @Override
    public void chatStream(String message, SseEmitter emitter) {
        if (isBlank(aiProperties.getApiKey())) {
            finish(emitter, mockReply(message));
            return;
        }
        try (BufferedReader reader = openChatStream(message)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    String chunk = extractContent(data);
                    if (chunk != null) {
                        emitter.send(SseEmitter.event().name("message").data(chunk));
                    }
                }
            }
            finish(emitter, null);
        } catch (Exception e) {
            log.error("AI stream error", e);
            String msg = e.getMessage();
            if (msg != null && msg.contains("AI API error")) {
                String detail = msg.substring(msg.indexOf("AI API error") + "AI API error".length()).trim();
                finish(emitter, "AI 服务连接失败：" + detail);
            } else {
                finish(emitter, "网络异常，请稍后再试。");
            }
        }
    }

    @Override
    public TicketAnalysisVO analyzeTicket(String title, String description) {
        if (isBlank(aiProperties.getApiKey())) {
            return fallback(title, description);
        }
        try {
            String userMsg = "工单标题：" + (title == null ? "" : title) + "\n工单描述：" + (description == null ? "" : description);
            String content = callChatCompletion(userMsg);
            TicketAnalysisVO vo = parseAnalysis(content);
            if (vo == null || vo.getSentiment() == null || vo.getValid() == null || vo.getScore() == null) {
                log.warn("AI 工单分析返回格式不完整, 回退规则引擎, content={}", content);
                return fallback(title, description);
            }
            return vo;
        } catch (Exception e) {
            log.warn("AI 工单分析失败, 回退规则引擎: {}", e.getMessage());
            return fallback(title, description);
        }
    }

    /**
     * 非流式调用 DeepSeek，返回完整回复文本。
     */
    private String callChatCompletion(String userMessage) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(aiProperties.getBaseUrl() + "/chat/completions").toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + aiProperties.getApiKey());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(MAPPER.writeValueAsString(new ChatRequestBody(
                    aiProperties.getModel(),
                    new Message[]{
                            new Message("system", TICKET_ANALYSIS_SYSTEM),
                            new Message("user", userMessage)
                    }
            )).getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            String err = readError(conn);
            log.error("AI API error {}: {}", status, err);
            throw new IOException("AI API error " + status + ": " + err);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            JsonNode root = MAPPER.readTree(sb.toString());
            JsonNode content = root.path("choices").get(0).path("message").path("content");
            return content.asText();
        }
    }

    private TicketAnalysisVO parseAnalysis(String content) {
        try {
            String json = extractJson(content);
            JsonNode node = MAPPER.readTree(json);
            TicketAnalysisVO vo = new TicketAnalysisVO();
            vo.setSentiment(node.path("sentiment").asText());
            vo.setValid(node.path("valid").asInt());
            vo.setScore(node.path("score").asInt());
            vo.setEval(node.path("eval").asText());
            return vo;
        } catch (Exception e) {
            log.warn("解析 AI 工单分析结果失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractJson(String content) {
        if (content == null) return "";
        String trimmed = content.trim();
        if (trimmed.startsWith("{")) return trimmed;
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        return (start >= 0 && end > start) ? trimmed.substring(start, end + 1) : trimmed;
    }

    private TicketAnalysisVO fallback(String title, String description) {
        TicketAiAnalyzer.AiResult r = TicketAiAnalyzer.analyze(title, description);
        TicketAnalysisVO vo = new TicketAnalysisVO();
        vo.setSentiment(r.getSentiment());
        vo.setValid(r.getValid());
        vo.setScore(r.getScore());
        vo.setEval(r.getEval());
        return vo;
    }

    private BufferedReader openChatStream(String message) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(aiProperties.getBaseUrl() + "/chat/completions").toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + aiProperties.getApiKey());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(MAPPER.writeValueAsString(new RequestBody(
                    aiProperties.getModel(),
                    new Message[]{
                            new Message("system", SYSTEM_PROMPT),
                            new Message("user", message)
                    },
                    true
            )).getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            String err = readError(conn);
            log.error("AI API error {}: {}", status, err);
            throw new IOException("AI API error " + status + ": " + err);
        }
        return new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
    }

    private String extractContent(String data) {
        try {
            JsonNode delta = MAPPER.readTree(data).path("choices").get(0).path("delta").path("content");
            return (delta.isMissingNode() || delta.isNull()) ? null : delta.asText();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readError(HttpURLConnection conn) {
        try (BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = err.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void finish(SseEmitter emitter, String reply) {
        try {
            if (reply != null) {
                emitter.send(SseEmitter.event().name("message").data(reply));
            }
            emitter.send(SseEmitter.event().name("done").data(""));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private String mockReply(String message) {        String msg = message.toLowerCase();
        if (containsAny(msg, "开户", "注册", "开卡"))
            return "开户流程：请点击登录页的「开通银行账户」，填写基本信息、设置密码即可完成开户。需要准备本人身份证和手机号。";
        if (containsAny(msg, "登录", "登陆", "密码错误", "登不上"))
            return "如无法登录，请检查账号密码是否正确。若忘记密码，可在登录页点击「忘记密码」，通过手机验证码重置。";
        if (containsAny(msg, "存款", "存钱", "存入"))
            return "存款操作：登录后在首页点击「存款」，输入金额并确认交易密码即可完成存款。单笔存款限额由系统设定。";
        if (containsAny(msg, "取款", "取钱", "取出"))
            return "取款操作：登录后在首页点击「取款」，输入金额并确认交易密码即可。请确保账户余额充足。";
        if (containsAny(msg, "转账", "转钱", "汇款"))
            return "转账操作：登录后在首页点击「转账」，输入对方账号、金额并确认交易密码。转账会同时扣除转出方和增加接收方余额。";
        if (containsAny(msg, "余额", "多少钱", "查钱", "还剩"))
            return "查询余额：登录后首页顶部会直接显示您的账户总资产，也可以在交易记录中查看明细。";
        if (containsAny(msg, "交易记录", "流水", "明细", "账单"))
            return "查看交易记录：登录后首页点击「交易明细」或侧边栏的「交易记录」，可按时间查看所有存取款、转账、理财等交易流水。";
        if (containsAny(msg, "理财", "产品", "基金", "投资"))
            return "理财产品：登录后点击「理财产品」可查看在售产品列表，选择产品输入金额即可购买。在我的持仓中可查看收益和提前赎回。";
        if (containsAny(msg, "密码", "修改密码", "改密码"))
            return "修改密码：登录后点击右上角用户头像 →「修改密码」，输入原密码和新密码即可。如忘记原密码，请在登录页使用「忘记密码」通过手机验证码重置。";
        if (containsAny(msg, "冻结", "封号", "锁"))
            return "账户冻结：若账户被冻结，将无法进行任何交易操作。请联系管理员（如您是管理员，可在侧边栏「账户管理」中操作冻结/解冻）。";
        if (containsAny(msg, "客服", "人工", "帮助", "你好", "您好"))
            return "您好！我是钱钱银行智能客服，很高兴为您服务。您可以问我关于开户、登录、存款、取款、转账、理财等方面的问题。";
        if (containsAny(msg, "利率", "利息", "收益率"))
            return "理财产品的年化收益率会在产品详情页展示，不同产品期限和收益率不同。您可以在「理财产品」列表中查看具体产品的预期年化收益率。";
        if (containsAny(msg, "赎回", "提前赎回", "退出"))
            return "提前赎回：在我的持仓中，点击「提前赎回」并确认交易密码即可。赎回金额按投资金额 + 预期收益计算，实际收益以赎回时为准。";
        return "抱歉，我暂时无法回答这个问题。您可以咨询开户、登录、存款、取款、转账、理财等方面的问题，或联系人工客服获取帮助。";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    private static class RequestBody {
        public String model;
        public Message[] messages;
        public boolean stream;
        public RequestBody(String model, Message[] messages, boolean stream) {
            this.model = model;
            this.messages = messages;
            this.stream = stream;
        }
    }

    @SuppressWarnings("unused")
    private static class ChatRequestBody {
        public String model;
        public Message[] messages;
        public ChatRequestBody(String model, Message[] messages) {
            this.model = model;
            this.messages = messages;
        }
    }

    @SuppressWarnings("unused")
    private static class Message {
        public String role;
        public String content;
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
