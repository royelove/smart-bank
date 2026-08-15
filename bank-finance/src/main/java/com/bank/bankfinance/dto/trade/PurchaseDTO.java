package com.bank.bankfinance.dto.trade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PurchaseDTO {
    @NotBlank(message = "卡号不能为空")
    private String accountNumber;
    @NotNull(message = "产品ID不能为空")
    private Long productId;
    @NotNull(message = "购买金额不能为空")
    @Positive(message = "购买金额必须大于0")
    private BigDecimal amount;
    @NotBlank(message = "交易密码不能为空")
    private String password;
    @NotBlank(message = "请求Id不能为空")
    private String requestId;
}
