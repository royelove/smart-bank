package com.bank.bankfinance.dto.trade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RedeemDTO {
    @NotBlank(message = "卡号不能为空")
    private String accountNumber;
    @NotNull(message = "持仓ID不能为空")
    private Long holdingId;
    @NotBlank(message = "交易密码不能为空")
    private String password;
}
