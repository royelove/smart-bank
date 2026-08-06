package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FaceVerifyResult {
    private boolean pass;
    private double score;

    public FaceVerifyResult(boolean pass, double score) {
        this.pass = pass;
        this.score = score;
    }
}
