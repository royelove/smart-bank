package com.bank.bankfinance.common.ai;

/**
 * 工单 AI 情绪与价值分析引擎（本地规则实现，不依赖外部 API）。
 *
 * 提交工单时自动分析：
 *  - 情绪：积极 / 消极 / 中性 / 恶意
 *  - 价值评分：0~100
 *  - 有效性：1=有效工单，0=无效/恶意（自动屏蔽，进入无效工单池）
 *  - 评估意见：生成可读的分析结论
 */
public class TicketAiAnalyzer {

    public static final String SENTIMENT_POSITIVE = "积极";
    public static final String SENTIMENT_NEGATIVE = "消极";
    public static final String SENTIMENT_NEUTRAL = "中性";
    public static final String SENTIMENT_MALICIOUS = "恶意";

    /** 辱骂 / 攻击性词汇：直接判定恶意 */
    private static final String[] MALICIOUS = {
            "骗子", "王八蛋", "狗东西", "傻逼", "智障", "脑残", "去死", "草泥马",
            "妈的", "他妈", "操你", "操你妈", "fuck", "shit", "混蛋", "贱人", "垃圾货"
    };

    /** 灌水 / 无意义特征 */
    private static final String[] GIBBERISH = {
            "哈哈哈哈", "呵呵", "哈哈", "嘻嘻", "嘿嘿", "111", "666", "999",
            "测试", "test", "试试", "水", "灌水", "qwe", "asdf", "随机", "随便"
    };

    /** 具体业务诉求关键词：命中越多价值越高 */
    private static final String[] VALUABLE = {
            "退钱", "退款", "扣款", "手续费", "盗刷", "冻结", "解冻", "多扣", "少扣",
            "未到账", "转账", "密码", "额度", "利息", "到期", "查询", "卡", "账户",
            "金额", "元", "银行", "业务", "办理", "投诉", "举报", "atm", "app", "错误", "问题", "异常"
    };

    /** 正向情绪词 */
    private static final String[] POSITIVE = {
            "谢谢", "感谢", "很好", "不错", "满意", "赞", "好用", "顺利", "辛苦了", "好评"
    };

    /** 负向情绪词 */
    private static final String[] NEGATIVE = {
            "生气", "愤怒", "失望", "很差", "太差", "垃圾", "不满", "坑", "差评", "被骗",
            "气死", "投诉", "举报", "糟糕", "严重", "崩溃", "无法接受"
    };

    private TicketAiAnalyzer() {
    }

    /**
     * 分析工单内容。
     *
     * @param title       工单标题（可空）
     * @param description 工单描述（可空）
     */
    public static AiResult analyze(String title, String description) {
        String titleText = title == null ? "" : title;
        String descText = description == null ? "" : description;
        String text = (titleText + " " + descText).trim();
        if (text.isEmpty()) {
            return new AiResult(SENTIMENT_NEUTRAL, 0, 0, "内容为空，判定为无价值工单。");
        }

        int score = 50;
        int len = text.length();
        int valuableHit = countHits(text, VALUABLE);

        // 长度维度
        if (len < 6) {
            score -= 30;
        } else if (len < 10) {
            score -= 10;
        } else if (len <= 500) {
            score += 15;
        } else if (len > 1000) {
            score -= 10;
        }

        // 诉求明确度：命中关键词越多越有价值
        score += Math.min(valuableHit * 10, 30);

        // 情绪识别
        boolean pos = containsAny(text, POSITIVE);
        boolean neg = containsAny(text, NEGATIVE);
        String sentiment = SENTIMENT_NEUTRAL;
        if (pos && !neg) sentiment = SENTIMENT_POSITIVE;
        else if (neg) sentiment = SENTIMENT_NEGATIVE;

        // 恶意 / 无价值判定
        boolean malicious = containsAny(text, MALICIOUS);
        boolean gibberish = containsAny(text, GIBBERISH);
        boolean repetitive = isRepetitive(text);
        boolean tooShort = len < 10 && valuableHit == 0;

        String eval;
        if (malicious) {
            sentiment = SENTIMENT_MALICIOUS;
            score = Math.min(score, 15);
            eval = "检测到辱骂/攻击性词汇，判定为恶意投诉，已自动屏蔽进入无效工单。";
        } else if (gibberish || tooShort) {
            score = Math.min(score, 25);
            eval = "内容过于简短或疑似灌水，无明确业务诉求，判定为无效工单。";
        } else if (repetitive) {
            score = Math.min(score, 30);
            eval = "内容存在大量重复字符，疑似灌水，判定为无效工单。";
        } else if (score >= 60) {
            eval = "包含明确业务诉求，属于有效工单，情绪" + sentiment + "，建议优先处理。";
        } else if (score >= 45) {
            eval = "具备一定信息量，判定为有效工单，情绪" + sentiment + "。";
        } else {
            eval = "信息量不足，价值较低，建议谨慎处理。";
        }

        int valid = (malicious || gibberish || tooShort || repetitive || score < 40) ? 0 : 1;
        score = Math.max(0, Math.min(score, 100));

        return new AiResult(sentiment, valid, score, eval);
    }

    private static boolean isRepetitive(String text) {
        if (text.length() < 4) return false;
        int repeats = 0;
        for (int i = 1; i < text.length(); i++) {
            if (text.charAt(i) == text.charAt(i - 1)) repeats++;
        }
        return repeats >= text.length() * 0.4;
    }

    private static int countHits(String text, String[] keywords) {
        int count = 0;
        for (String kw : keywords) {
            int idx = 0;
            while ((idx = text.indexOf(kw, idx)) != -1) {
                count++;
                idx += kw.length();
            }
        }
        return count;
    }

    private static boolean containsAny(String text, String[] keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    /** 分析结果 */
    public static class AiResult {
        private final String sentiment;
        private final int valid;
        private final int score;
        private final String eval;

        public AiResult(String sentiment, int valid, int score, String eval) {
            this.sentiment = sentiment;
            this.valid = valid;
            this.score = score;
            this.eval = eval;
        }

        public String getSentiment() {
            return sentiment;
        }

        public int getValid() {
            return valid;
        }

        public int getScore() {
            return score;
        }

        public String getEval() {
            return eval;
        }
    }
}
