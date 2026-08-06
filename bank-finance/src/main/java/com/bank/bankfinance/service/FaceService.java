package com.bank.bankfinance.service;

import com.bank.bankfinance.vo.FaceVerifyResult;

import java.util.List;

public interface FaceService {
    /** 提取人脸特征(512维) */
    List<Double> extractFeature(String base64Image);

    /** 与注册特征比对 */
    FaceVerifyResult verify(String base64Image, List<Double> storedFeature);
}
