package com.bank.bankfinance.service;

import com.bank.bankfinance.dto.user.UserRegisterDTO;
import com.bank.bankfinance.dto.user.UserUpdatePsdDTO;
import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.entity.User;
import com.bank.bankfinance.vo.AccountVO;
import com.bank.bankfinance.vo.FaceVerifyResult;
import com.bank.bankfinance.vo.UserVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface UserService extends IService<User> {
    void userRegister(UserRegisterDTO userRegisterDTO);
    User getByUserIdCard(String userIdCard);
    User login(String userPhone, String password);
    List<AccountVO> getAccountVOsByUserId(Long userId);
    void addAccountToUser(Long userId, String userIdCard, String accountNumber);
    UserVO getUserInfo(Long userId);
    void updatePsw(Long userId, UserUpdatePsdDTO userUpdatePsdDTO);
    void bindFace(Long userId, String base64Image);
    FaceVerifyResult verifyFace(Long userId, String base64Image);
    FaceVerifyResult verifyRealName(Long userId, String base64Image);
}
