
/*DTO层
    用来接收前端传来的封装好的请求并进行参数校验
 */
package com.bank.bankfinance.dto.trade;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransferDTO {
    @NotBlank
    private String inNumber;
    @NotBlank
    private String outNumber;
    @NotBlank
    private BigDecimal amount;
    @NotBlank
    private String password;
    TransferDTO() {}
}
