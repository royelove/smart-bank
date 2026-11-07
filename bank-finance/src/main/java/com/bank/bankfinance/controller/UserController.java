package com.bank.bankfinance.controller;

import com.bank.bankfinance.common.annotation.RateLimit;
import com.bank.bankfinance.common.result.Result;
import com.bank.bankfinance.common.util.JwtUtil;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.dto.user.*;
import com.bank.bankfinance.entity.User;
import com.bank.bankfinance.service.UserService;
import com.bank.bankfinance.vo.AccountVO;
import com.bank.bankfinance.vo.FaceVerifyResult;
import com.bank.bankfinance.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;

    // 修复：用户注册此前无限流，可被批量刷号。
    @PostMapping("/register")
    @RateLimit(maxRequests = 5, timeWindow = 1, timeUnit = TimeUnit.HOURS, message = "注册过于频繁，请稍后再试")
    public Result<Void> userRegister(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        userService.userRegister(userRegisterDTO);
        return Result.success();
    }

    // 修复：手机号登录此前无限流，可被用来暴力猜测密码（且用户名校验错误信息可枚举手机号）。
    @PostMapping("/login")
    @RateLimit(maxRequests = 5, timeWindow = 1, timeUnit = TimeUnit.MINUTES, message = "登录尝试过于频繁，请1分钟后再试")
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

    @PostMapping("/updatePsw")
    public Result<Void> updatePsw(@Valid @RequestBody UserUpdatePsdDTO userUpdatePsdDTO) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            return Result.fail("700002","未登录");
        }
        userService.updatePsw(userId, userUpdatePsdDTO);
        return Result.success();
    }

    @PostMapping("/face/bind")
    public Result<Void> bindFace(@Valid @RequestBody FaceBindDTO faceBindDTO) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            return Result.fail("700002","未登录");
        }
        userService.bindFace(userId, faceBindDTO.getImage());
        return Result.success();
    }

    @PostMapping("/face/verify")
    @RateLimit(maxRequests = 10, timeWindow = 1, timeUnit = TimeUnit.MINUTES, message = "操作过于频繁，请稍后再试")
    public Result<FaceVerifyResult> verifyFace(@Valid @RequestBody FaceVerifyDTO faceVerifyDTO) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            return Result.fail("700002","未登录");
        }
        return Result.success(userService.verifyFace(userId, faceVerifyDTO.getImage()));
    }

    @PostMapping("/face/realname/verify")
    @RateLimit(maxRequests = 10, timeWindow = 1, timeUnit = TimeUnit.MINUTES, message = "操作过于频繁，请稍后再试")
    public Result<FaceVerifyResult> verifyRealName(@Valid @RequestBody FaceVerifyDTO faceVerifyDTO) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            return Result.fail("700002","未登录");
        }
        return Result.success(userService.verifyRealName(userId, faceVerifyDTO.getImage()));
    }
}
