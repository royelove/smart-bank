package com.bank.bankfinance.controller;

import com.bank.bankfinance.common.result.Result;
import com.bank.bankfinance.common.util.JwtUtil;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.dto.*;
import com.bank.bankfinance.entity.User;
import com.bank.bankfinance.service.UserService;
import com.bank.bankfinance.vo.AccountVO;
import com.bank.bankfinance.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<Void> userRegister(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        userService.userRegister(userRegisterDTO);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<String> userLogin(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        User user = userService.login(userLoginDTO.getUserPhone(), userLoginDTO.getPassword());
        String token = jwtUtil.generateToken("U_" + user.getId());
        return Result.success(token);
    }

    @PostMapping("/addAccount")
    public Result<Void> addAccount(@Valid @RequestBody UserAddAccountDTO dto) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            return Result.fail("700002","未登录");
        }
        userService.addAccountToUser(userId, dto.getUserIdCard(), dto.getAccountNumber());
        return Result.success();
    }

    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        Long userId = LoginContext.getUserId();
        if (userId == null) return Result.fail("700002","未登录");
        UserVO vo = userService.getUserInfo(userId);
        return Result.success(vo);
    }

    @GetMapping("/accounts")
    public Result<List<AccountVO>> getAccounts() {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            return Result.fail("700002","未登录");
        }
        List<AccountVO> accounts = userService.getAccountVOsByUserId(userId);
        return Result.success(accounts);
    }
}
