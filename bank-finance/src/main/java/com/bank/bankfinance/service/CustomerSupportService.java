package com.bank.bankfinance.service;

import com.bank.bankfinance.dto.support.StaffLoginDTO;
import com.bank.bankfinance.dto.support.StaffRegisterDTO;
import com.bank.bankfinance.dto.support.TicketReplyDTO;
import com.bank.bankfinance.dto.support.TicketSubmitDTO;
import com.bank.bankfinance.entity.Staff;
import com.bank.bankfinance.vo.SupportLoginVO;
import com.bank.bankfinance.vo.SupportTicketVO;

import java.util.List;

public interface CustomerSupportService {
    SupportLoginVO staffLogin(StaffLoginDTO dto);
    void staffRegister(StaffRegisterDTO dto);
    void submitTicket(TicketSubmitDTO dto);
    List<SupportTicketVO> listMyTickets(String accountNumber);
    List<SupportTicketVO> listAllTickets(Integer status, Integer valid);
    void acceptTicket(Long ticketId);
    void replyTicket(TicketReplyDTO dto);
    void closeTicket(Long ticketId);
    Staff currentStaff();
}
