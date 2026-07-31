package com.bank.bankfinance.service;

import com.bank.bankfinance.dto.*;
import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.vo.AccountVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


public interface AccountService {
    void deposit(DepositDTO depositdto);
    void transfer(TransferDTO transferDTO);
    AccountVO getAccountVO(String accountNumber);
    void withdraw(WithdrawDTO withdrawDTO);
    void register(RegisterDTO registerDTO);
    AccountVO login(LoginDTO loginDTO);
    void updatePsd(UpdatePsdDTO updatePsdDTO);
    Account getAccountEntity(String accountNumber);
    BigDecimal getAccountBalance(String accountNumber);
    void sendVerificationCode(SendCodeDTO sendCodeDTO);
    void resetPassword(ResetPasswordDTO resetPasswordDTO);
    void updateProfile(UpdateProfileDTO updateProfileDTO);
}
