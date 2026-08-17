package com.bank.bankfinance.dto.support;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdCardEnrollDTO {
    @NotBlank(message = "身份证号不能为空")
    private String idCard;
    @NotBlank(message = "姓名不能为空")
    private String name;
    @NotBlank(message = "请上传人脸照片")
    private String faceImage;
}