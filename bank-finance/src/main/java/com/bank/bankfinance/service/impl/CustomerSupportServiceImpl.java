package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.common.util.JwtUtil;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.common.util.TicketNoUtil;
import com.bank.bankfinance.dto.support.StaffLoginDTO;
import com.bank.bankfinance.dto.support.StaffRegisterDTO;
import com.bank.bankfinance.dto.support.TicketReplyDTO;
import com.bank.bankfinance.dto.support.TicketSubmitDTO;
import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.entity.Staff;
import com.bank.bankfinance.entity.SupportTicket;
import com.bank.bankfinance.mapper.AccountMapper;
import com.bank.bankfinance.mapper.StaffMapper;
import com.bank.bankfinance.mapper.SupportTicketMapper;
import com.bank.bankfinance.service.AiService;
import com.bank.bankfinance.service.CustomerSupportService;
import com.bank.bankfinance.vo.SupportLoginVO;
import com.bank.bankfinance.vo.SupportTicketVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.bank.bankfinance.common.constant.ResponseCode.*;

@Service
public class CustomerSupportServiceImpl extends ServiceImpl<SupportTicketMapper, SupportTicket>
        implements CustomerSupportService {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROCESSING = 1;
    public static final int STATUS_REPLIED = 2;
    public static final int STATUS_CLOSED = 3;

    private static final List<String> CATEGORIES = Arrays.asList("业务办理", "咨询", "投诉", "其他");

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AiService aiService;

    @Override
    public SupportLoginVO staffLogin(StaffLoginDTO dto) {
        Staff staff = staffMapper.selectOne(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getUsername, dto.getUsername()));
        if (staff == null || !passwordEncoder.matches(dto.getPassword(), staff.getPassword())) {
            throw new BusinessException(SUPPORT_LOGIN_ERROR);
        }
        String token = jwtUtil.generateToken("S_" + staff.getId());
        return new SupportLoginVO(token, staff.getStaffName());
    }

    @Override
    public void staffRegister(StaffRegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getCheckPassword())) {
            throw new BusinessException("400001", "两次输入的密码不一致");
        }
        Long count = staffMapper.selectCount(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException("400002", "客服账号已存在");
        }
        Staff staff = new Staff();
        staff.setUsername(dto.getUsername());
        staff.setPassword(passwordEncoder.encode(dto.getPassword()));
        staff.setStaffName(dto.getStaffName() == null || dto.getStaffName().isEmpty()
                ? dto.getUsername() : dto.getStaffName());
        staffMapper.insert(staff);
    }

    @Override
    public void submitTicket(TicketSubmitDTO dto) {
        if (!CATEGORIES.contains(dto.getCategory())) {
            throw new BusinessException(TICKET_TYPE_INVALID);
        }
        String accountNumber = dto.getAccountNumber();
        if (accountNumber == null || accountNumber.isEmpty()) {
            accountNumber = LoginContext.getAccount();
        }
        if (accountNumber == null || accountNumber.isEmpty()) {
            throw new BusinessException(NO_LOGIN);
        }
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getAccountNumber, accountNumber));
        if (account == null) {
            throw new BusinessException("400003", "所选账户不存在");
        }
        Long userId = LoginContext.getUserId();
        if (userId != null) {
            if (!userId.equals(account.getUserId())) {
                throw new BusinessException("400004", "只能使用本人名下的银行卡111");
            }
        } else {
            String currentAccount = LoginContext.getAccount();
            if (currentAccount == null || !currentAccount.equals(accountNumber)) {
                throw new BusinessException("400004", "只能使用本人名下的银行卡222");
            }
        }
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo(TicketNoUtil.ticketNo());
        ticket.setAccountNumber(accountNumber);
        ticket.setCategory(dto.getCategory());
        ticket.setTitle(dto.getTitle());
        ticket.setDescription(dto.getDescription());
        ticket.setStatus(STATUS_PENDING);
        ticket.setUserName(account.getAccountHolderName());
        ticket.setUserPhone(account.getUserPhoneNumber());
        com.bank.bankfinance.vo.TicketAnalysisVO ai = aiService.analyzeTicket(dto.getTitle(), dto.getDescription());
        ticket.setSentiment(ai.getSentiment());
        ticket.setAiScore(ai.getScore());
        ticket.setAiValid(ai.getValid());
        ticket.setAiEval(ai.getEval());
        this.save(ticket);
    }

    @Override
    public List<SupportTicketVO> listMyTickets(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            accountNumber = LoginContext.getAccount();
        }
        if (accountNumber == null || accountNumber.isEmpty()) {
            throw new BusinessException(NO_LOGIN);
        }
        List<SupportTicket> tickets = this.lambdaQuery()
                .eq(SupportTicket::getAccountNumber, accountNumber)
                .orderByDesc(SupportTicket::getCreationDate)
                .list();
        return tickets.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<SupportTicketVO> listAllTickets(Integer status, Integer valid) {
        LambdaQueryWrapper<SupportTicket> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(SupportTicket::getStatus, status);
        }
        if (valid != null) {
            wrapper.eq(SupportTicket::getAiValid, valid);
        }
        wrapper.orderByDesc(SupportTicket::getCreationDate);
        return this.list(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void acceptTicket(Long ticketId) {
        Staff staff = currentStaff();
        SupportTicket ticket = requireTicket(ticketId);
        if (ticket.getStatus() != STATUS_PENDING) {
            throw new BusinessException(TICKET_ALREADY_ACCEPTED);
        }
        ticket.setStatus(STATUS_PROCESSING);
        ticket.setHandlerId(staff.getId());
        ticket.setHandlerName(staff.getStaffName());
        this.updateById(ticket);
    }

    @Override
    public void replyTicket(TicketReplyDTO dto) {
        Staff staff = currentStaff();
        SupportTicket ticket = requireTicket(dto.getTicketId());
        if (ticket.getStatus() == STATUS_CLOSED) {
            throw new BusinessException(TICKET_CLOSED_REPLY);
        }
        ticket.setReply(dto.getReply());
        ticket.setStatus(STATUS_REPLIED);
        if (ticket.getHandlerId() == null) {
            ticket.setHandlerId(staff.getId());
            ticket.setHandlerName(staff.getStaffName());
        }
        this.updateById(ticket);
    }

    @Override
    public void closeTicket(Long ticketId) {
        currentStaff();
        SupportTicket ticket = requireTicket(ticketId);
        if (ticket.getStatus() == STATUS_CLOSED) {
            throw new BusinessException(TICKET_CLOSED);
        }
        ticket.setStatus(STATUS_CLOSED);
        ticket.setCloseDate(LocalDateTime.now());
        this.updateById(ticket);
    }

    @Override
    public Staff currentStaff() {
        Long staffId = LoginContext.getStaffId();
        if (staffId == null) {
            throw new BusinessException(NO_ADMIN_PERMISSION);
        }
        Staff staff = staffMapper.selectById(staffId);
        if (staff == null) {
            throw new BusinessException(NO_ADMIN_PERMISSION);
        }
        return staff;
    }

    private SupportTicket requireTicket(Long ticketId) {
        SupportTicket ticket = this.getById(ticketId);
        if (ticket == null) {
            throw new BusinessException(TICKET_NOT_FOUND);
        }
        return ticket;
    }

    private SupportTicketVO toVO(SupportTicket t) {
        SupportTicketVO vo = new SupportTicketVO();
        vo.setId(t.getId());
        vo.setTicketNo(t.getTicketNo());
        vo.setAccountNumber(t.getAccountNumber());
        vo.setUserName(t.getUserName());
        vo.setUserPhone(t.getUserPhone());
        vo.setCategory(t.getCategory());
        vo.setCategoryText(categoryText(t.getCategory()));
        vo.setTitle(t.getTitle());
        vo.setDescription(t.getDescription());
        vo.setStatus(t.getStatus());
        vo.setStatusText(statusText(t.getStatus()));
        vo.setReply(t.getReply());
        vo.setHandlerName(t.getHandlerName());
        vo.setSentiment(t.getSentiment());
        vo.setAiScore(t.getAiScore());
        vo.setAiValid(t.getAiValid());
        vo.setAiEval(t.getAiEval());
        vo.setCreationDate(t.getCreationDate());
        vo.setCloseDate(t.getCloseDate());
        return vo;
    }

    private String categoryText(String category) {
        return category == null ? "--" : category;
    }

    private String statusText(Integer status) {
        if (status == null) return "--";
        switch (status) {
            case STATUS_PENDING: return "待处理";
            case STATUS_PROCESSING: return "处理中";
            case STATUS_REPLIED: return "已回复";
            case STATUS_CLOSED: return "已关闭";
            default: return "--";
        }
    }
}
