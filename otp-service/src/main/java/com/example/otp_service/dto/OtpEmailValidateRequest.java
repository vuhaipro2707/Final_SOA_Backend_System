package com.example.otp_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpEmailValidateRequest {
    private String email;
    private String otpCode;
}