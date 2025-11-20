package com.example.customer_management_service.service;

import com.example.customer_management_service.dto.ChangePasswordRequest;
import com.example.customer_management_service.dto.CreateCustomerRequest;
import com.example.customer_management_service.dto.ForgetPasswordConfirmRequest;
import com.example.customer_management_service.dto.ForgetPasswordInitiateRequest;
import com.example.customer_management_service.dto.GenericResponse;
import com.example.customer_management_service.dto.PasswordResetRequest;
import com.example.customer_management_service.dto.UpdateCustomerInfoRequest;
import com.example.customer_management_service.model.Customer;
import com.example.customer_management_service.repository.CustomerRepository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

@Service
public class CustomerService {

    private final StringRedisTemplate redisTemplate;
    private static final String CONFIRMATION_KEY_PREFIX = "RESET_CONFIRMATION:";
    private static final long CONFIRMATION_TTL_SECONDS = 600;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; 

    private final WebClient webClient;
    
    private static final String OTP_SERVICE_BASE_URL = "http://otp-service:8087";
    private static final String MAIL_SERVICE_BASE_URL = "http://mail-service:8088";

    private static final long OTP_EXPIRY_SECONDS = 300; 

    public CustomerService(WebClient.Builder webClientBuilder, StringRedisTemplate redisTemplate) {
        this.webClient = webClientBuilder.build();
        this.redisTemplate = redisTemplate;
    }

    public Optional<Customer> getCustomerById(Long customerId) {
        return customerRepository.findByCustomerId(customerId);
    }

    public List<Customer> searchCustomerByPhoneNumber(String phoneNumber) {
        return customerRepository.findByPhoneNumberContaining(phoneNumber);
    }

    public List<Customer> searchCustomerByFullName(String fullName) {
        return customerRepository.findByFullNameContainingIgnoreCase(fullName);
    }

    private String buildConfirmationKey(String email) {
        return CONFIRMATION_KEY_PREFIX + email;
    }
    
    @Transactional
    public Optional<Customer> updateCustomerInfo(Long customerId, UpdateCustomerInfoRequest request) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        
        if (customerOpt.isEmpty()) {
            return Optional.empty();
        }

        Customer customer = customerOpt.get();
        
        List<Customer> existingCustomerList = customerRepository.findByEmailOrPhoneNumber(request.getEmail(), request.getPhoneNumber());

        for (Customer existingCustomer : existingCustomerList) {
            if (!existingCustomer.getCustomerId().equals(customerId)) {
                if (existingCustomer.getEmail().equals(request.getEmail())) {
                    System.out.println("Email already in use.");
                    throw new IllegalArgumentException("Email already in use.");
                }
                if (existingCustomer.getPhoneNumber().equals(request.getPhoneNumber())) {
                    throw new IllegalArgumentException("Phone number already in use.");
                }
            }
        }
        
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setAvatarColor(request.getAvatarColor());

        return Optional.of(customerRepository.save(customer));
    }

    @Transactional
    public Optional<Customer> createCustomer(CreateCustomerRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword()) || !StringUtils.hasText(request.getEmail()) || !StringUtils.hasText(request.getFullName())) {
            throw new IllegalArgumentException("Username, password, full name, and email are required.");
        }
        
        if (customerRepository.existsByUsername(request.getUsername())) {
             throw new IllegalArgumentException("Username already exists.");
        }
        if (customerRepository.existsByEmail(request.getEmail())) {
             throw new IllegalArgumentException("Email already exists.");
        }
        
        if (StringUtils.hasText(request.getPhoneNumber())) {
            List<Customer> existingByPhone = customerRepository.findByEmailOrPhoneNumber(null, request.getPhoneNumber());
            if (!existingByPhone.isEmpty()) {
                 throw new IllegalArgumentException("Phone number already exists.");
            }
        }

        Customer newCustomer = new Customer();
        newCustomer.setUsername(request.getUsername());
        newCustomer.setPassword(passwordEncoder.encode(request.getPassword()));
        newCustomer.setFullName(request.getFullName());
        newCustomer.setEmail(request.getEmail());
        newCustomer.setPhoneNumber(request.getPhoneNumber());
        newCustomer.setAvatarColor(request.getAvatarColor() != null ? request.getAvatarColor() : "#000000"); // Default color
        
        return Optional.of(customerRepository.save(newCustomer));
    }

    @Transactional(readOnly = true)
    public OtpResponse initiatePasswordReset(ForgetPasswordInitiateRequest request) {
        Optional<Customer> customerOpt = customerRepository.findByEmail(request.getEmail());
        if (customerOpt.isEmpty()) {
            throw new IllegalArgumentException("Email not found.");
        }
        
        Customer customer = customerOpt.get();
        
        OtpResponse otpResponse = callOtpServiceForEmail(request.getEmail(), false, customer.getCustomerId());

        if (!otpResponse.getMessage().contains("Existing")) {
            sendOtpEmail(customer.getEmail(), customer.getFullName(), otpResponse.getOtpCode(), customer.getCustomerId());
        }
        
        redisTemplate.opsForValue().set(buildConfirmationKey(request.getEmail()), "PENDING", OTP_EXPIRY_SECONDS, TimeUnit.SECONDS);
        
        return otpResponse;
    }

    @Transactional
    public void resendPasswordResetOtp(ForgetPasswordInitiateRequest request) {
        Optional<Customer> customerOpt = customerRepository.findByEmail(request.getEmail());
        if (customerOpt.isEmpty()) {
            throw new IllegalArgumentException("Email not found.");
        }
        
        Customer customer = customerOpt.get();
        
        OtpResponse otpResponse = callOtpServiceForEmail(request.getEmail(), true, customer.getCustomerId());
        
        sendOtpEmail(customer.getEmail(), customer.getFullName(), otpResponse.getOtpCode(), customer.getCustomerId());

        redisTemplate.opsForValue().set(buildConfirmationKey(request.getEmail()), "PENDING", OTP_EXPIRY_SECONDS, TimeUnit.SECONDS);
    }

    @Transactional(readOnly = true)
    public boolean confirmPasswordReset(ForgetPasswordConfirmRequest request) {
        Optional<Customer> customerOpt = customerRepository.findByEmail(request.getEmail());
        if (customerOpt.isEmpty()) {
            throw new IllegalArgumentException("Email not found.");
        }

        Customer customer = customerOpt.get();
        String confirmationKey = buildConfirmationKey(request.getEmail());
        String status = redisTemplate.opsForValue().get(confirmationKey);

        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Initiation step not completed or session expired.");
        }
        System.out.println("111111111111111");
        System.out.println(request.getEmail() + ", " + request.getOtpCode() + ", " + customer.getCustomerId());
        boolean isValid = validateOtpServiceForEmail(request.getEmail(), request.getOtpCode(), customer.getCustomerId());
        System.out.println("22222222222222");
        if (isValid) {
            redisTemplate.opsForValue().set(buildConfirmationKey(request.getEmail()), "CONFIRMED", CONFIRMATION_TTL_SECONDS, TimeUnit.SECONDS);
        }
        
        return isValid;
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String confirmationKey = buildConfirmationKey(request.getEmail());
        String status = redisTemplate.opsForValue().get(confirmationKey);
        
        if (!"CONFIRMED".equals(status)) {
            throw new IllegalStateException("OTP confirmation is required before resetting password.");
        }
        
        Optional<Customer> customerOpt = customerRepository.findByEmail(request.getEmail());
        if (customerOpt.isEmpty()) {
            throw new IllegalArgumentException("Email not found.");
        }
        
        Customer customer = customerOpt.get();
        customer.setPassword(passwordEncoder.encode(request.getNewPassword()));
        customerRepository.save(customer);
        
        redisTemplate.delete(confirmationKey);
    }

    @Transactional
    public void changePassword(Long customerId, ChangePasswordRequest request) {
        Optional<Customer> customerOpt = customerRepository.findByCustomerId(customerId);
        if (customerOpt.isEmpty()) {
            throw new IllegalArgumentException("Customer not found.");
        }
        
        Customer customer = customerOpt.get();
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), customer.getPassword())) {
            throw new BadCredentialsException("Invalid current password.");
        }

        customer.setPassword(passwordEncoder.encode(request.getNewPassword()));
        customerRepository.save(customer);
    }
    
    private OtpResponse callOtpServiceForEmail(String email, boolean isResend, Long customerId) {
        String url = isResend ? OTP_SERVICE_BASE_URL + "/internal/resend/email" : OTP_SERVICE_BASE_URL + "/internal/generate/email";
        try {
            @Data @NoArgsConstructor @AllArgsConstructor
            class LocalOtpEmailRequest { private String email; }
            
            LocalOtpEmailRequest otpRequest = new LocalOtpEmailRequest(email);

            GenericResponse<Map<String, String>> response = webClient.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header("X-Customer-Id", String.valueOf(customerId))
                .bodyValue(otpRequest)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<GenericResponse<Map<String, String>>>() {})
                .block();

            if (response != null && response.isSuccess() && response.getData() != null) {
                String otpCode = response.getData().get("otpCode");
                String message = response.getData().get("statusMessage");
                return new OtpResponse(otpCode, message);
            }
            throw new RuntimeException("OTP Service failed: " + (response != null ? response.getMessage() : "No response body."));
        } catch (Exception e) {
            throw new RuntimeException("Error calling OTP Service: " + e.getMessage());
        }
    }

    private boolean validateOtpServiceForEmail(String email, String otpCode, Long customerId) {
        String url = OTP_SERVICE_BASE_URL + "/internal/validate/email";
        System.out.println("Validating OTP for email: " + email + " with code: " + otpCode + " for customerId: " + customerId);
        try {
            @Data @NoArgsConstructor @AllArgsConstructor
            class LocalOtpEmailValidateRequest { private String email; private String otpCode; }
            
            LocalOtpEmailValidateRequest otpRequest = new LocalOtpEmailValidateRequest(email, otpCode);

            GenericResponse<Object> response = webClient.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header("X-Customer-Id", String.valueOf(customerId))
                .bodyValue(otpRequest)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<GenericResponse<Object>>() {})
                .block();

            return response != null && response.isSuccess();
        } catch (Exception e) {
            System.err.println("Error calling OTP Validate Service: " + e.getMessage());
            return false; 
        }
    }

    private void sendOtpEmail(String email, String fullName, String otpCode, Long customerId) {
         try {
            String subject = "Mã OTP đặt lại mật khẩu của bạn";
            String body = String.format("Xin chào %s,\n\nMã OTP của bạn là: %s. Mã này có hiệu lực trong %d phút.", 
                                        fullName, otpCode, OTP_EXPIRY_SECONDS / 60);

            @Data @NoArgsConstructor @AllArgsConstructor
            class LocalSendMailRequest { private String to; private String subject; private String body; }
            
            LocalSendMailRequest mailRequest = new LocalSendMailRequest(email, subject, body);

            webClient.post()
                .uri(MAIL_SERVICE_BASE_URL + "/internal/send")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header("X-Customer-Id", String.valueOf(customerId)) 
                .bodyValue(mailRequest)
                .retrieve()
                .bodyToMono(GenericResponse.class)
                .block();
            System.out.println("OTP email sent to " + email);
            
        } catch (Exception e) {
            System.err.println("Error sending OTP email: " + e.getMessage());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OtpResponse {
        private String otpCode;
        private String message;
    }
}