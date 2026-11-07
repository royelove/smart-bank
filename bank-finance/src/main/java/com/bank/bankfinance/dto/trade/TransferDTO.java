
/*DTO层
    用来接收前端传来的封装好的请求并进行参数校验
 */
package com.bank.bankfinance.dto.trade;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    /*
     * 修复：原为 @NotBlank。@NotBlank 只对 CharSequence 生效，作用在 BigDecimal 上会让
     * Hibernate Validator 抛 UnexpectedTypeException(HV000030)，请求在校验阶段即失败，
     * 转账功能整体不可用。与 DepositDTO 保持一致，改用 @NotNull + @DecimalMin。
     */
    @NotNull(message = "转账金额不能为空")
    @DecimalMin(value = "0.01", message = "转账金额必须大于0")
    private BigDecimal amount;
    @NotBlank
    private String password;
    @NotBlank(message = "请求Id不能为空")
    private String requestId;
    TransferDTO() {}
}
