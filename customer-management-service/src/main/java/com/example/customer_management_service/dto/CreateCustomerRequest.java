package com.example.customer_management_service.dto;

import lombok.Data;

@Data
public class CreateCustomerRequest {
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarColor;
}