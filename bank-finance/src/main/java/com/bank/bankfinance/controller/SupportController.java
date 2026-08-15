package com.bank.bankfinance.controller;

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
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/support")
public class SupportController {

    @Autowired
    private CustomerSupportService customerSupportService;

    @Autowired
    private IdCardLibService idCardLibService;

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
    public Result<SupportLoginVO> staffLogin(@Valid @RequestBody StaffLoginDTO dto) {
        return Result.success(customerSupportService.staffLogin(dto));
    }

    @PostMapping("/register")
    public Result<Void> staffRegister(@Valid @RequestBody StaffRegisterDTO dto) {
        customerSupportService.staffRegister(dto);
        return Result.success();
    }

    @PostMapping("/ticket")
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
}
