package com.bank.bankfinance.controller;


import com.bank.bankfinance.common.annotation.RateLimit;
import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.common.result.Result;
import com.bank.bankfinance.common.util.JwtUtil;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.common.util.RedisUtil;
import com.bank.bankfinance.dto.*;
import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.entity.Trade;
import com.bank.bankfinance.service.AccountService;
import com.bank.bankfinance.service.TradeService;
import com.bank.bankfinance.vo.AccountVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static com.bank.bankfinance.common.constant.ResponseCode.NO_LOGIN;
import static com.bank.bankfinance.common.result.Result.success;

@RestController
@RequestMapping("/account")
public class AccountController {
    @Autowired
    private AccountService accountService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private TradeService tradeService;
    @Autowired
    private RedisUtil redisUtil;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    @PostMapping("/deposit")
    public Result<Void> deposit(@Valid @RequestBody DepositDTO depositDTO) {
        accountService.deposit(depositDTO);
        return Result.success();
    }
    @PostMapping("/transfer")
    public Result<Void> transfer(@Valid @RequestBody TransferDTO transferDTO) {
        accountService.transfer(transferDTO);
        return Result.success();
    }
    @PostMapping("/withdraw")
    public Result<Void> withdraw(@Valid @RequestBody WithdrawDTO withdrawDTO) {
        accountService.withdraw(withdrawDTO);
        return Result.success();
    }
    @PostMapping("/getAccountEntity")
    public Result<AccountVO> getAccountEntity(@Valid @RequestParam String accountNumber) {
        AccountVO vo = accountService.getAccountVO(accountNumber);
        return Result.success(vo);
    }
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        accountService.register(registerDTO);
        return Result.success();
    }
    @PostMapping("/login")
    @RateLimit(maxRequests = 5, timeWindow = 1, timeUnit = TimeUnit.MINUTES, message = "登录尝试过于频繁，请1分钟后再试")
    public Result<LoginResponse> login(@Valid @RequestBody LoginDTO loginDTO) {
        AccountVO vo = accountService.login(loginDTO);
        String token = jwtUtil.generateToken(vo.getAccountNumber());
        LoginResponse loginResponse = new LoginResponse(token,vo);
        return Result.success(loginResponse);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            redisUtil.set(TOKEN_BLACKLIST_PREFIX + token, "1", jwtExpiration, TimeUnit.MILLISECONDS);
        }
        return Result.success();
    }
    @GetMapping("/getAccountBalance")
    public Result<BigDecimal> getAccountBalance(@RequestParam(required = false) String accountNumber) {
        BigDecimal accountBalance =  accountService.getAccountBalance(accountNumber);
        return Result.success(accountBalance);
    }
    @PostMapping("/updatePsw")
    public Result<Void> updatePsw(@Valid @RequestBody UpdatePsdDTO updatePsdDTO) {
        accountService.updatePsd(updatePsdDTO);
        return Result.success();
    }
    @PostMapping("/updateProfile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileDTO updateProfileDTO) {
        accountService.updateProfile(updateProfileDTO);
        return Result.success();
    }
    @PostMapping("/sendCode")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO sendCodeDTO) {
        accountService.sendVerificationCode(sendCodeDTO);
        return Result.success();
    }
    @PostMapping("/resetPassword")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
        accountService.resetPassword(resetPasswordDTO);
        return Result.success();
    }
    @PostMapping("/admin/freezeAccount")
    public Result<Void> freezeAccount(@Valid @RequestParam("AccountNumber")  String AccountNumber) {
        Account targetAccount = accountService.getAccountEntity(AccountNumber);
        accountService.freezeAccount(targetAccount);
        return Result.success();
    }
    @PostMapping("/admin/unfreezeAccount")
    public Result<Void> unfreezeAccount(@Valid @RequestParam("AccountNumber")  String AccountNumber) {
        Account targetAccount = accountService.getAccountEntity(AccountNumber);
        accountService.unfreezeAccount(targetAccount);
        return Result.success();
    }
    @GetMapping("/trades")
    public Result<IPage<Trade>> getTrades(@ModelAttribute TradeQueryDTO tradeQueryDTO) {
        String currentAccount = LoginContext.getAccount();
        if(currentAccount == null){
            throw new BusinessException(NO_LOGIN);
        }
        Account currentUser = accountService.getAccountEntity(currentAccount);
        boolean isAdmin = !"1".equals(currentUser.getRole());
        String targetAccountNumber;
        if(isAdmin && tradeQueryDTO.getAccountNumber() != null && !tradeQueryDTO.getAccountNumber().isEmpty()){
            targetAccountNumber = tradeQueryDTO.getAccountNumber();
        }else{
            targetAccountNumber = currentAccount;
        }
        Account targetAccount = accountService.getAccountEntity(targetAccountNumber);
        tradeQueryDTO.setAccountNumber(targetAccount.getAccountNumber());
        IPage<Trade> trades = tradeService.getTradesByAccount(tradeQueryDTO);
        return Result.success(trades);
    }
}
