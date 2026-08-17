package com.bank.bankfinance.dto.trade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StockOrderDTO {
    @NotBlank(message = "卡号不能为空")
    private String accountNumber;
    @NotBlank(message = "股票代码不能为空")
    private String stockCode;
    @NotBlank(message = "交易方向不能为空")
    private String direction;
    @NotBlank(message = "委托类型不能为空")
    private String priceType;
    /** 限价单必填 */
    private BigDecimal price;
    @NotNull(message = "委托数量不能为空")
    private Integer quantity;
    @NotBlank(message = "交易密码不能为空")
    private String password;
}