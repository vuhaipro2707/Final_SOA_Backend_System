package com.example.customer_management_service.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgetPasswordConfirmRequest {
    private String email;
    private String otpCode;
}