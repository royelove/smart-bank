package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.dto.user.UserRegisterDTO;
import com.bank.bankfinance.dto.user.UserUpdatePsdDTO;
import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.entity.IdCardLib;
import com.bank.bankfinance.entity.User;
import com.bank.bankfinance.mapper.AccountMapper;
import com.bank.bankfinance.mapper.IdCardLibMapper;
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

import static com.bank.bankfinance.common.constant.ResponseCode.*;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private FaceService faceService;

    @Autowired
    private IdCardLibMapper idCardLibMapper;


    /**
     * 按手机号取用户（登录流程专用）。
     *
     * <p>修复记录：原实现把整个 {@code User} 实体写进 Redis 的 {@code user:cache:<phone>}，
     * 缓存 30 分钟，而该实体同时携带 <b>bcrypt 口令散列</b>和<b>人脸特征模板</b>。两个后果：
     * <ol>
     *   <li>口令散列与生物特征被复制到了第二个存储系统。Redis 通常没有数据库同等的访问控制、
     *       审计与静态加密，一旦 Redis 可读就等于拿到全量凭据与生物特征，且无法通过改密码挽回。</li>
     *   <li>缓存<b>没有任何失效路径</b>：{@code updatePsw} 只更新数据库，登录仍从缓存取旧散列，
     *       于是「改完密码后旧密码在最长 30 分钟内继续可用、新密码反而登录失败」——
     *       对用户而言就是密码改不掉。</li>
     * </ol>
     * 该方法只服务于登录（{@code /user/login} 已限流 5 次/分钟），改为直接查库，
     * 用一次很便宜的索引查询换掉「凭据外泄 + 密码改不掉」两个问题。
     */
    public User getUserEntity(String userPhone){
        User user = this.lambdaQuery().eq(User::getUserPhone,userPhone).one();
        if(user == null){
            throw new BusinessException(BANK_ID_INVALID);
        }
        return user;
    }
    @Override
    public void userRegister(UserRegisterDTO userRegisterDTO) {
        long existCount = this.lambdaQuery()
                .eq(User::getUserPhone, userRegisterDTO.getUserPhone())
                .count();
        if (existCount > 0) {
            throw new BusinessException(USER_EXISTS);
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
        User user = getUserEntity(userPhone);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(USER_PASSWORD_INVALID);
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
            throw new BusinessException(USER_NOT_FOUND);
        }
        if (!user.getUserIdCard().equals(userIdCard)) {
            throw new BusinessException(ID_CARD_MISMATCH);
        }
        Account account = accountMapper.selectOne(
                new LambdaQueryWrapper<Account>().eq(Account::getAccountNumber, accountNumber)
        );
        if (account == null) {
            throw new BusinessException(ACCOUNT_NOT_FOUND);
        }
        if (!userIdCard.equals(account.getUserIdCard())) {
            throw new BusinessException(ACCOUNT_NOT_OWNED);
        }
        account.setUserId(userId);
        accountMapper.updateById(account);
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = this.getById(userId);
        if (user == null) throw new BusinessException(USER_NOT_FOUND);
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUserPhone(user.getUserPhone());
        vo.setUserName(user.getUserName());
        vo.setUserIdCard(user.getUserIdCard());
        vo.setFaceBound(user.getFaceBound() == null ? 0 : user.getFaceBound());
        vo.setRealnameBound(user.getRealnameBound() == null ? 0 : user.getRealnameBound());
        vo.setIdCardMask(maskIdCard(user.getUserIdCard()));
        return vo;
    }

    @Override
    public FaceVerifyResult verifyRealName(Long userId, String base64Image) {
        User user = this.getById(userId);
        if (user == null) throw new BusinessException(USER_NOT_FOUND);
        if (user.getUserIdCard() == null || user.getUserIdCard().isBlank()) {
            throw new BusinessException(ID_CARD_REQUIRED);
        }
        IdCardLib lib = idCardLibMapper.selectOne(new LambdaQueryWrapper<IdCardLib>()
                .eq(IdCardLib::getIdCard, user.getUserIdCard()));
        if (lib == null || lib.getFaceFeature() == null || lib.getFaceFeature().isEmpty()) {
            throw new BusinessException(ID_CARD_NOT_FOUND);
        }
        List<Double> stored = Arrays.stream(lib.getFaceFeature().split(","))
                .map(s -> Double.parseDouble(s.trim()))
                .toList();
        FaceVerifyResult result = faceService.verify(base64Image, stored);
        if (result.isPass()) {
            List<Double> feature = faceService.extractFeature(base64Image);
            user.setFaceFeature(feature.stream()
                    .map(v -> String.format("%.6f", v))
                    .collect(Collectors.joining(",")));
            user.setFaceBound(1);
            user.setRealnameBound(1);
            this.updateById(user);
        }
        return result;
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) return idCard;
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    @Override
    public void bindFace(Long userId, String base64Image) {
        User user = this.getById(userId);
        if (user == null) throw new BusinessException(USER_NOT_FOUND);
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
        if (user == null) throw new BusinessException(USER_NOT_FOUND);
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
            throw new BusinessException(USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(userUpdatePsdDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(OLD_PASSWORD_INVALID);
        }
        if (!userUpdatePsdDTO.getConfirmPassword().equals(userUpdatePsdDTO.getNewPassword())) {
            throw new BusinessException(TRANSACTION_INVALID_NOT_SAME_CODE);
        }
        user.setPassword(passwordEncoder.encode(userUpdatePsdDTO.getNewPassword()));
        this.updateById(user);
        // 说明：修复后 getUserEntity 不再缓存 User 实体，改密码即刻生效（旧实现需等 30 分钟缓存过期）。
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
