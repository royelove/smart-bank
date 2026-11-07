package com.bank.bankfinance.controller;

import com.bank.bankfinance.common.annotation.RateLimit;
import com.bank.bankfinance.common.constant.ResponseCode;
import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.common.result.Result;
import com.bank.bankfinance.dto.support.IdCardEnrollDTO;
import com.bank.bankfinance.dto.support.StaffLoginDTO;
import com.bank.bankfinance.dto.support.StaffRegisterDTO;
import com.bank.bankfinance.dto.support.TicketReplyDTO;
import com.bank.bankfinance.dto.support.TicketSubmitDTO;
import com.bank.bankfinance.service.CustomerSupportService;
import com.bank.bankfinance.service.IdCardLibService;
import com.bank.bankfinance.vo.IdCardLibVO;
import com.bank.bankfinance.vo.SupportLoginVO;
import com.bank.bankfinance.vo.SupportTicketVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/support")
public class SupportController {

    private static final Logger log = LoggerFactory.getLogger(SupportController.class);

    /** 客服自助注册密钥请求头名 */
    private static final String STAFF_REGISTER_KEY_HEADER = "X-Staff-Register-Key";

    @Autowired
    private CustomerSupportService customerSupportService;

    @Autowired
    private IdCardLibService idCardLibService;

    /**
     * 客服注册密钥。
     *
     * <p>留空（默认）＝不校验，保持演示/初始化流程可用，但启动后每次注册都会打 WARN；
     * 生产环境必须配置 {@code app.support.staff-register-key}，否则任何人都能自建客服账号并
     * 访问 {@code /support/admin/**}。这是「默认可用」与「生产安全」之间的显式取舍，
     * 而不是把公开接口当成了安全接口。
     */
    @Value("${app.support.staff-register-key:}")
    private String staffRegisterKey;

    @PostMapping("/admin/idcard/enroll")
    public Result<Void> enrollIdCard(@Valid @RequestBody IdCardEnrollDTO dto) {
        idCardLibService.enroll(dto);
        return Result.success();
    }

    @GetMapping("/admin/idcard/list")
    public Result<List<IdCardLibVO>> listIdCardLib() {
        return Result.success(idCardLibService.listAll());
    }

    @PostMapping("/login")
    @RateLimit(maxRequests = 5, timeWindow = 1, timeUnit = TimeUnit.MINUTES, message = "登录尝试过于频繁，请1分钟后再试")
    public Result<SupportLoginVO> staffLogin(@Valid @RequestBody StaffLoginDTO dto) {
        return Result.success(customerSupportService.staffLogin(dto));
    }

    @PostMapping("/register")
    @RateLimit(maxRequests = 5, timeWindow = 1, timeUnit = TimeUnit.HOURS, message = "注册过于频繁，请稍后再试")
    public Result<Void> staffRegister(@Valid @RequestBody StaffRegisterDTO dto,
                                      HttpServletRequest request) {
        assertStaffRegisterAllowed(request);
        customerSupportService.staffRegister(dto);
        return Result.success();
    }

    // 修复：工单接口此前无限流，可被脚本批量灌单，撑爆客服队列。
    @PostMapping("/ticket")
    @RateLimit(maxRequests = 10, timeWindow = 1, timeUnit = TimeUnit.HOURS, message = "提交过于频繁，请稍后再试")
    public Result<Void> submitTicket(@Valid @RequestBody TicketSubmitDTO dto) {
        customerSupportService.submitTicket(dto);
        return Result.success();
    }

    @GetMapping("/tickets")
    public Result<List<SupportTicketVO>> myTickets(@RequestParam(required = false) String accountNumber) {
        return Result.success(customerSupportService.listMyTickets(accountNumber));
    }

    @GetMapping("/admin/tickets")
    public Result<List<SupportTicketVO>> allTickets(@RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) Integer valid) {
        return Result.success(customerSupportService.listAllTickets(status, valid));
    }

    @PostMapping("/admin/accept/{id}")
    public Result<Void> accept(@PathVariable Long id) {
        customerSupportService.acceptTicket(id);
        return Result.success();
    }

    @PostMapping("/admin/reply")
    public Result<Void> reply(@Valid @RequestBody TicketReplyDTO dto) {
        customerSupportService.replyTicket(dto);
        return Result.success();
    }

    @PostMapping("/admin/close/{id}")
    public Result<Void> close(@PathVariable Long id) {
        customerSupportService.closeTicket(id);
        return Result.success();
    }

    /**
     * 客服注册闸门。
     *
     * <p>修复记录：{@code /support/register} 是公开接口且直接把请求体落库为客服账号，
     * 而 {@code /support/admin/**} 只要求 STAFF 角色——即「任何人都能给自己发一张后台通行证」。
     * 由于该项目需要开箱即可演示，这里没有把接口直接关死，而是加一个可配置的共享密钥：
     * <ul>
     *   <li>{@code app.support.staff-register-key} 已配置 → 必须携带匹配的
     *       {@value #STAFF_REGISTER_KEY_HEADER} 请求头，否则 403。</li>
     *   <li>未配置（默认） → 放行，但每条 WARN 日志提醒这是不安全状态。</li>
     * </ul>
     */
    private void assertStaffRegisterAllowed(HttpServletRequest request) {
        if (staffRegisterKey == null || staffRegisterKey.isBlank()) {
            log.warn("客服自助注册未配置 app.support.staff-register-key，当前对所有人开放，"
                    + "生产环境请务必配置；remoteAddr={}", request.getRemoteAddr());
            return;
        }
        String provided = request.getHeader(STAFF_REGISTER_KEY_HEADER);
        if (provided == null || !constantTimeEquals(staffRegisterKey, provided)) {
            log.warn("客服注册密钥校验失败 remoteAddr={}", request.getRemoteAddr());
            throw new BusinessException(ResponseCode.STAFF_REGISTER_FORBIDDEN);
        }
    }

    /** 定长比较，避免通过响应时间差逐字节猜测注册密钥 */
    private boolean constantTimeEquals(String expected, String actual) {
        if (expected.length() != actual.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < expected.length(); i++) {
            diff |= expected.charAt(i) ^ actual.charAt(i);
        }
        return diff == 0;
    }
}
