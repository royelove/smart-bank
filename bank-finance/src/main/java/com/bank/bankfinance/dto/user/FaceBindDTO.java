package com.bank.bankfinance.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FaceBindDTO {
    @NotBlank(message = "请上传人脸照片")
    private String image;
}
