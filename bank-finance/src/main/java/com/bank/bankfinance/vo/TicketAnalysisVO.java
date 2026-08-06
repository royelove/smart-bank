package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketAnalysisVO {
    private String sentiment;
    private Integer valid;
    private Integer score;
    private String eval;
}
