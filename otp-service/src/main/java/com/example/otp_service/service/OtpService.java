package com.example.otp_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    
    private static final String FORGET_PASS_KEY_PREFIX = "OTP:FORGET_PASS";
    
    @Value("${otp.expiry.seconds:300}")
    private long OTP_EXPIRY_SECONDS; 

    public static final String MESSAGE_NEW_OTP = "New OTP generated successfully.";
    public static final String MESSAGE_EXISTING_OTP = "Existing unexpired OTP found. Reusing it.";
    public static final String MESSAGE_RESEND_SUCCESS = "Old OTP deleted and new OTP generated successfully.";


    public OtpService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildEmailKey(String email) {
        return String.format("%s:%s", FORGET_PASS_KEY_PREFIX, email);
    }

    private String generateRandomOtp() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    public OtpResult getOrCreateOtpForEmail(String email, boolean isResend) {
        String key = buildEmailKey(email);
        
        if (isResend) {
            redisTemplate.delete(key);
            String newOtp = generateAndSaveOtp(key);
            return new OtpResult(newOtp, MESSAGE_RESEND_SUCCESS);
        }

        String existingOtp = redisTemplate.opsForValue().get(key);

        if (existingOtp != null) {
            return new OtpResult(existingOtp, MESSAGE_EXISTING_OTP);
        } else {
            String newOtp = generateAndSaveOtp(key);
            return new OtpResult(newOtp, MESSAGE_NEW_OTP);
        }
    }

    private String generateAndSaveOtp(String key) {
        String newOtp = generateRandomOtp();
        redisTemplate.opsForValue().set(
            key, 
            newOtp, 
            OTP_EXPIRY_SECONDS, 
            TimeUnit.SECONDS
        );
        return newOtp;
    }

    public boolean validateOtpForEmail(String email, String otpCode) {
        String key = buildEmailKey(email);
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(otpCode)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    public static class OtpResult {
        public final String otpCode;
        public final String message;
        
        public OtpResult(String otpCode, String message) {
            this.otpCode = otpCode;
            this.message = message;
        }
    }
}