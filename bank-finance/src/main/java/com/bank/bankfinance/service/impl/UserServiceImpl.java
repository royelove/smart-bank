package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.dto.user.UserRegisterDTO;
import com.bank.bankfinance.dto.user.UserUpdatePsdDTO;
import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.entity.User;
import com.bank.bankfinance.mapper.AccountMapper;
import com.bank.bankfinance.mapper.UserMapper;
import com.bank.bankfinance.service.FaceService;
import com.bank.bankfinance.service.UserService;
import com.bank.bankfinance.vo.AccountVO;
import com.bank.bankfinance.vo.FaceVerifyResult;
import com.bank.bankfinance.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.bank.bankfinance.common.constant.ResponseCode.FACE_NOT_BOUND;
import static com.bank.bankfinance.common.constant.ResponseCode.PASSWORD_INVALID;
import static com.bank.bankfinance.common.constant.ResponseCode.TRANSACTION_INVALID_NOT_SAME_CODE;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private FaceService faceService;

    @Override
    public void userRegister(UserRegisterDTO userRegisterDTO) {
        long existCount = this.lambdaQuery()
                .eq(User::getUserPhone, userRegisterDTO.getUserPhone())
                .count();
        if (existCount > 0) {
            throw new RuntimeException("该账号已存在");
        }
        String password = userRegisterDTO.getPassword();
        String checkPassword = userRegisterDTO.getCheckPassword();
        if (password != null && !password.equals(checkPassword)) {
            throw new BusinessException(TRANSACTION_INVALID_NOT_SAME_CODE);
        }
        User user = new User();
        user.setUserPhone(userRegisterDTO.getUserPhone());
        user.setUserName(userRegisterDTO.getUserName());
        user.setPassword(passwordEncoder.encode(password));
        user.setUserIdCard(userRegisterDTO.getUserIdCard());
        this.baseMapper.insert(user);
    }

    @Override
    public User getByUserIdCard(String userIdCard) {
        return this.lambdaQuery().eq(User::getUserIdCard, userIdCard).one();
    }

    @Override
    public User login(String userPhone, String password) {
        User user = this.lambdaQuery().eq(User::getUserPhone, userPhone).one();
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        return user;
    }

    @Override
    public List<AccountVO> getAccountVOsByUserId(Long userId) {
        List<Account> accounts = accountMapper.selectList(
                new LambdaQueryWrapper<Account>().eq(Account::getUserId, userId)
        );
        return accounts.stream().map(this::toAccountVO).collect(Collectors.toList());
    }

    @Override
    public void addAccountToUser(Long userId, String userIdCard, String accountNumber) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!user.getUserIdCard().equals(userIdCard)) {
            throw new RuntimeException("身份证号不匹配");
        }
        Account account = accountMapper.selectOne(
                new LambdaQueryWrapper<Account>().eq(Account::getAccountNumber, accountNumber)
        );
        if (account == null) {
            throw new RuntimeException("账户不存在");
        }
        if (!userIdCard.equals(account.getUserIdCard())) {
            throw new RuntimeException("该卡不属于此身份证号");
        }
        account.setUserId(userId);
        accountMapper.updateById(account);
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = this.getById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUserPhone(user.getUserPhone());
        vo.setUserName(user.getUserName());
        vo.setUserIdCard(user.getUserIdCard());
        vo.setFaceBound(user.getFaceBound() == null ? 0 : user.getFaceBound());
        return vo;
    }

    @Override
    public void bindFace(Long userId, String base64Image) {
        User user = this.getById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        List<Double> feature = faceService.extractFeature(base64Image);
        String joined = feature.stream()
                .map(v -> String.format("%.6f", v))
                .collect(Collectors.joining(","));
        user.setFaceFeature(joined);
        user.setFaceBound(1);
        this.updateById(user);
    }

    @Override
    public FaceVerifyResult verifyFace(Long userId, String base64Image) {
        User user = this.getById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (user.getFaceBound() == null || user.getFaceBound().intValue() != 1
                || user.getFaceFeature() == null || user.getFaceFeature().isEmpty()) {
            throw new BusinessException(FACE_NOT_BOUND);
        }
        List<Double> stored = Arrays.stream(user.getFaceFeature().split(","))
                .map(s -> Double.parseDouble(s.trim()))
                .toList();
        return faceService.verify(base64Image, stored);
    }

    @Override
    public void updatePsw(Long userId, UserUpdatePsdDTO userUpdatePsdDTO) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(userUpdatePsdDTO.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        if (!userUpdatePsdDTO.getConfirmPassword().equals(userUpdatePsdDTO.getNewPassword())) {
            throw new BusinessException(TRANSACTION_INVALID_NOT_SAME_CODE);
        }
        user.setPassword(passwordEncoder.encode(userUpdatePsdDTO.getNewPassword()));
        this.updateById(user);
    }

    private AccountVO toAccountVO(Account account) {
        AccountVO vo = new AccountVO();
        vo.setAccountNumber(account.getAccountNumber());
        vo.setAccountHolderName(account.getAccountHolderName());
        vo.setAccountType(account.getAccountType());
        vo.setAccountBalance(account.getAccountBalance());
        vo.setAccountStatus(account.getAccountStatus());
        vo.setUserPhoneNumber(account.getUserPhoneNumber());
        vo.setUserIdCard(account.getUserIdCard());
        vo.setUserId(account.getUserId());
        return vo;
    }
}
