/*DTO层
    用来接收前端传来的封装好的请求并进行参数校验
 */
package com.bank.bankfinance.dto;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DepositDTO {
    @NotBlank
    private String accountNo;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    @NotBlank
    private String password;
    DepositDTO() {}
}
