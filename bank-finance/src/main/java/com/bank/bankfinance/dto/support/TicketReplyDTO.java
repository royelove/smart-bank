package com.bank.bankfinance.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketReplyDTO {
    @NotNull(message = "工单ID不能为空")
    private Long ticketId;
    @NotBlank(message = "回复内容不能为空")
    private String reply;
}
