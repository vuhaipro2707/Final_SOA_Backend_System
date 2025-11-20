package com.example.customer_management_service.controller;
import com.example.customer_management_service.dto.ChangePasswordRequest;
import com.example.customer_management_service.dto.CreateCustomerRequest;
import com.example.customer_management_service.dto.ForgetPasswordConfirmRequest;
import com.example.customer_management_service.dto.ForgetPasswordInitiateRequest;
import com.example.customer_management_service.dto.GenericResponse;
import com.example.customer_management_service.dto.PasswordResetRequest;
import com.example.customer_management_service.dto.UpdateCustomerInfoRequest;
import com.example.customer_management_service.service.CustomerService;
import com.example.customer_management_service.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

 

@RestController
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping("/info")
    public ResponseEntity<GenericResponse<Customer>> getCustomerInfo(Authentication authentication) {
        try {
            Long customerId = Long.parseLong(authentication.getPrincipal().toString());

            Optional<Customer> customerOpt = customerService.getCustomerById(customerId);

            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                customer.setPassword(null); 
                return ResponseEntity.ok(GenericResponse.success("Customer info retrieved successfully.", customer));
            } else {
                return ResponseEntity.status(404).body(GenericResponse.failure("Customer not found."));
            }

        } catch (NumberFormatException e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Invalid customer ID format in authentication context."));
        }
    }

    @PostMapping("/info")
    public ResponseEntity<GenericResponse<Customer>> updateCustomerInfo(@RequestBody UpdateCustomerInfoRequest request, Authentication authentication) {
        try {
            Long customerId = Long.parseLong(authentication.getPrincipal().toString());
            
            if (request.getFullName() == null || request.getEmail() == null || request.getAvatarColor() == null) {
                return ResponseEntity.badRequest().body(GenericResponse.failure("Full name, email, and avatar color are required."));
            }
            
            Optional<Customer> updatedCustomerOpt = customerService.updateCustomerInfo(customerId, request);

            if (updatedCustomerOpt.isPresent()) {
                Customer updatedCustomer = updatedCustomerOpt.get();
                updatedCustomer.setPassword(null); 
                return ResponseEntity.ok(GenericResponse.success("Customer info updated successfully.", updatedCustomer));
            } else {
                return ResponseEntity.status(404).body(GenericResponse.failure("Customer not found or failed to update."));
            }

        } catch (NumberFormatException e) {
            return ResponseEntity.status(400).body(GenericResponse.failure("Invalid customer ID format in authentication context."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/info/customerId/{customerId}")
    public ResponseEntity<GenericResponse<Customer>> getCustomerInfoById(@PathVariable Long customerId) {
        Optional<Customer> customerOpt = customerService.getCustomerById(customerId);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            customer.setPassword(null); 
            return ResponseEntity.ok(GenericResponse.success("Customer info retrieved successfully.", customer));
        } else {
            return ResponseEntity.status(404).body(GenericResponse.failure("Customer not found."));
        }
    }

    @GetMapping("/info/phoneNumber/{phoneNumber}")
    public ResponseEntity<GenericResponse<List<Customer>>> getCustomerInfoByPhoneNumber(@PathVariable String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("Phone number cannot be empty."));
        }
        
        List<Customer> customers = customerService.searchCustomerByPhoneNumber(phoneNumber);

        customers.forEach(c -> c.setPassword(null));

        if (customers.isEmpty()) {
            return ResponseEntity.ok(GenericResponse.success("No customers found matching phone number.", customers));
        } else {
            return ResponseEntity.ok(GenericResponse.success("Customers retrieved successfully.", customers));
        }
    }

    @GetMapping("/info/fullName/{fullName}")
    public ResponseEntity<GenericResponse<List<Customer>>> getCustomerInfoByFullName(@PathVariable String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("Full name cannot be empty."));
        }
        
        List<Customer> customers = customerService.searchCustomerByFullName(fullName);

        customers.forEach(c -> c.setPassword(null));

        if (customers.isEmpty()) {
            return ResponseEntity.ok(GenericResponse.success("No customers found matching full name.", customers));
        } else {
            return ResponseEntity.ok(GenericResponse.success("Customers retrieved successfully.", customers));
        }
    }

    @GetMapping("/fullName/customerId/{customerId}")
    public ResponseEntity<GenericResponse<String>> getCustomerFullName(@PathVariable Long customerId) {
        Optional<Customer> customerOpt = customerService.getCustomerById(customerId);

        if (customerOpt.isPresent()) {
            String fullName = customerOpt.get().getFullName();
            return ResponseEntity.ok(GenericResponse.success("Customer full name retrieved successfully.", fullName));
        } else {
            return ResponseEntity.status(404).body(GenericResponse.failure("Customer not found."));
        }
    }

    @PostMapping("/create/account")
    public ResponseEntity<GenericResponse<Customer>> createCustomer(@RequestBody CreateCustomerRequest request) {
        try {
            Optional<Customer> customerOpt = customerService.createCustomer(request);

            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                customer.setPassword(null);
                return ResponseEntity.ok(GenericResponse.success("Account created successfully.", customer));
            } else {
                return ResponseEntity.status(500).body(GenericResponse.failure("Failed to create account."));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/forgetPass/initiate")
    public ResponseEntity<GenericResponse<Void>> forgetPassInitiate(@RequestBody ForgetPasswordInitiateRequest request) {
        try {
            customerService.initiatePasswordReset(request);
            return ResponseEntity.ok(GenericResponse.success("OTP sent to email."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/forgetPass/resend")
    public ResponseEntity<GenericResponse<Void>> forgetPassResend(@RequestBody ForgetPasswordInitiateRequest request) {
        try {
            customerService.resendPasswordResetOtp(request);
            return ResponseEntity.ok(GenericResponse.success("OTP resent to email."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }
    

    @PostMapping("/forgetPass/confirm")
    public ResponseEntity<GenericResponse<Void>> forgetPassConfirm(@RequestBody ForgetPasswordConfirmRequest request) {
        try {
            boolean isConfirmed = customerService.confirmPasswordReset(request);

            if (isConfirmed) {
                return ResponseEntity.ok(GenericResponse.success("OTP confirmed. You can now reset your password."));
            } else {
                return ResponseEntity.ok(GenericResponse.failure("Invalid or expired OTP or session."));
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/forgetPass/reset")
    public ResponseEntity<GenericResponse<Void>> forgetPassReset(@RequestBody PasswordResetRequest request) {
        try {
            customerService.resetPassword(request);
            return ResponseEntity.ok(GenericResponse.success("Password reset successfully."));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/changePass")
    public ResponseEntity<GenericResponse<Void>> changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {
        System.out.println("Changing password for customer ID: " + request.getCurrentPassword() + " -> " + request.getNewPassword());
        if (request.getCurrentPassword() == null || request.getNewPassword() == null) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("currentPassword and newPassword are required."));
        }
        try {
            Long customerId = Long.parseLong(authentication.getPrincipal().toString());
            customerService.changePassword(customerId, request);
            return ResponseEntity.ok(GenericResponse.success("Password changed successfully."));
        
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(GenericResponse.failure(e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }
}