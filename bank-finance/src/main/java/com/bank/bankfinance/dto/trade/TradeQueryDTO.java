package com.bank.bankfinance.dto.trade;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradeQueryDTO {
    @NotBlank
    String accountNumber;
    //每条页数
    Integer size = 10;
    //页号
    Integer page = 1;

    String type;

    String startDate;

    String endDate;
    TradeQueryDTO(){}
}
