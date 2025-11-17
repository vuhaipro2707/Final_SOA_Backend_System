package com.example.customer_management_service.dto;

import lombok.Data;

@Data
public class UpdateCustomerInfoRequest {
    private String fullName; 
    private String email;
    private String phoneNumber; 
    private String avatarColor;
}