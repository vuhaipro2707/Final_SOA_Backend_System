package com.example.customer_management_service.controller;
import com.example.customer_management_service.dto.GenericResponse;
import com.example.customer_management_service.dto.UpdateCustomerInfoRequest;
import com.example.customer_management_service.service.CustomerService;
import com.example.customer_management_service.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
}