package com.bank.bankfinance.dto.trade;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;

public class ValCheck {
    public static void main(String[] args) {
        Validator v = Validation.buildDefaultValidatorFactory().getValidator();

        // 1) TransferDTO with a perfectly VALID amount
        TransferDTO t = new TransferDTO();
        t.setInNumber("6222002");
        t.setOutNumber("6222001");
        t.setAmount(new BigDecimal("100.00"));
        t.setPassword("123456");
        t.setRequestId("req-1");
        try {
            Set<ConstraintViolation<TransferDTO>> vs = v.validate(t);
            System.out.println("TRANSFER: OK, violations=" + vs.size());
        } catch (Throwable e) {
            System.out.println("TRANSFER: THREW " + e.getClass().getName());
            System.out.println("          msg=" + e.getMessage());
        }

        // 2) DepositDTO (same author, correct annotations) as control
        DepositDTO d = new DepositDTO();
        d.setAccountNo("6222001");
        d.setAmount(new BigDecimal("100.00"));
        d.setPassword("123456");
        d.setRequestId("req-2");
        try {
            Set<ConstraintViolation<DepositDTO>> vs = v.validate(d);
            System.out.println("DEPOSIT : OK, violations=" + vs.size());
        } catch (Throwable e) {
            System.out.println("DEPOSIT : THREW " + e.getClass().getName());
        }

        // 3) StockOrderDTO with negative quantity and null price
        StockOrderDTO s = new StockOrderDTO();
        s.setAccountNumber("6222001");
        s.setStockCode("600519");
        s.setDirection("BUY");
        s.setPriceType("MARKET");
        s.setQuantity(-500);
        s.setPassword("123456");
        try {
            Set<ConstraintViolation<StockOrderDTO>> vs = v.validate(s);
            System.out.println("STOCK   : violations=" + vs.size() + " (negative qty accepted if 0)");
        } catch (Throwable e) {
            System.out.println("STOCK   : THREW " + e.getClass().getName());
        }
    }
}
