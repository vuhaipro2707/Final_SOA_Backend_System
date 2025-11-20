package com.example.otp_service.controller;

import com.example.otp_service.dto.GenericResponse;
import com.example.otp_service.dto.OtpEmailRequest;
import com.example.otp_service.dto.OtpEmailValidateRequest;
import com.example.otp_service.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class OtpController {

    @Autowired
    private OtpService otpService;
    
    
    @PostMapping("/internal/generate/email")
    public ResponseEntity<GenericResponse<Map<String, String>>> generateOtpByEmail(
        @RequestBody OtpEmailRequest request) 
    {
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("Invalid request: email is required."));
        }
        
        try {
            OtpService.OtpResult result = otpService.getOrCreateOtpForEmail(request.getEmail(), false);

            Map<String, String> data = Map.of(
                "otpCode", result.otpCode,
                "statusMessage", result.message 
            );

            return ResponseEntity.ok(GenericResponse.success(result.message, data));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/internal/resend/email")
    public ResponseEntity<GenericResponse<Map<String, String>>> resendOtpByEmail(
        @RequestBody OtpEmailRequest request) 
    {
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("Invalid request: email is required."));
        }
        
        try {
            OtpService.OtpResult result = otpService.getOrCreateOtpForEmail(request.getEmail(), true);

            Map<String, String> data = Map.of(
                "otpCode", result.otpCode,
                "statusMessage", result.message 
            );

            return ResponseEntity.ok(GenericResponse.success(result.message, data));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Internal server error: " + e.getMessage()));
        }
    }

    
    @PostMapping("/internal/validate/email")
    public ResponseEntity<GenericResponse<Object>> validateOtpByEmail(
        @RequestBody OtpEmailValidateRequest request) 
    {
        System.out.println("Validating OTP for email: " + request.getEmail() + " with OTP code: " + request.getOtpCode());
        if (request.getEmail() == null || request.getOtpCode() == null) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("Invalid request: email and otpCode are required."));
        }
        
        try {
            boolean isValid = otpService.validateOtpForEmail(request.getEmail(), request.getOtpCode());

            if (isValid) {
                return ResponseEntity.ok(GenericResponse.success("OTP validated successfully."));
            } else {
                return ResponseEntity.ok(GenericResponse.failure("Invalid or expired OTP."));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Internal server error: " + e.getMessage()));
        }
    }
}