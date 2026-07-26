package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.service.AiService;
import org.springframework.stereotype.Service;

@Service
public class AiServiceImpl implements AiService {

    @Override
    public String chat(String message) {
        String msg = message.toLowerCase();

        if (containsAny(msg, "开户", "注册", "开卡")) {
            return "开户流程：请点击登录页的「开通银行账户」，填写基本信息、设置密码即可完成开户。需要准备本人身份证和手机号。";

        } else if (containsAny(msg, "登录", "登陆", "密码错误", "登不上")) {
            return "如无法登录，请检查账号密码是否正确。若忘记密码，可在登录页点击「忘记密码」，通过手机验证码重置。";

        } else if (containsAny(msg, "存款", "存钱", "存入")) {
            return "存款操作：登录后在首页点击「存款」，输入金额并确认交易密码即可完成存款。单笔存款限额由系统设定。";

        } else if (containsAny(msg, "取款", "取钱", "取出")) {
            return "取款操作：登录后在首页点击「取款」，输入金额并确认交易密码即可。请确保账户余额充足。";

        } else if (containsAny(msg, "转账", "转钱", "汇款")) {
            return "转账操作：登录后在首页点击「转账」，输入对方账号、金额并确认交易密码。转账会同时扣除转出方和增加接收方余额。";

        } else if (containsAny(msg, "余额", "多少钱", "查钱", "还剩")) {
            return "查询余额：登录后首页顶部会直接显示您的账户总资产，也可以在交易记录中查看明细。";

        } else if (containsAny(msg, "交易记录", "流水", "明细", "账单")) {
            return "查看交易记录：登录后首页点击「交易明细」或侧边栏的「交易记录」，可按时间查看所有存取款、转账、理财等交易流水。";

        } else if (containsAny(msg, "理财", "产品", "基金", "投资")) {
            return "理财产品：登录后点击「理财产品」可查看在售产品列表，选择产品输入金额即可购买。在我的持仓中可查看收益和提前赎回。";

        } else if (containsAny(msg, "密码", "修改密码", "改密码")) {
            return "修改密码：登录后点击右上角用户头像 →「修改密码」，输入原密码和新密码即可。如忘记原密码，请在登录页使用「忘记密码」通过手机验证码重置。";

        } else if (containsAny(msg, "冻结", "封号", "锁")) {
            return "账户冻结：若账户被冻结，将无法进行任何交易操作。请联系管理员（如您是管理员，可在侧边栏「账户管理」中操作冻结/解冻）。";

        } else if (containsAny(msg, "客服", "人工", "帮助", "你好", "您好")) {
            return "您好！我是智慧银行智能客服，很高兴为您服务。您可以问我关于开户、登录、存款、取款、转账、理财等方面的问题。";

        } else if (containsAny(msg, "利率", "利息", "收益率")) {
            return "理财产品的年化收益率会在产品详情页展示，不同产品期限和收益率不同。您可以在「理财产品」列表中查看具体产品的预期年化收益率。";

        } else if (containsAny(msg, "赎回", "提前赎回", "退出")) {
            return "提前赎回：在我的持仓中，点击「提前赎回」并确认交易密码即可。赎回金额按投资金额 + 预期收益计算，实际收益以赎回时为准。";

        } else {
            return "抱歉，我暂时无法回答这个问题。您可以咨询开户、登录、存款、取款、转账、理财等方面的问题，或联系人工客服获取帮助。";
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
