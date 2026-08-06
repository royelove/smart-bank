package com.bank.bankfinance.dto.support;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketSubmitDTO {
    private String accountNumber;
    @NotBlank(message = "问题类型不能为空")
    private String category;
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "描述不能为空")
    private String description;
}
