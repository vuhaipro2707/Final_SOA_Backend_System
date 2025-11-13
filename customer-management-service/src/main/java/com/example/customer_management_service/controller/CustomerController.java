package com.example.customer_management_service.controller;
import com.example.customer_management_service.dto.GenericResponse;
import com.example.customer_management_service.service.CustomerService;
import com.example.customer_management_service.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;
import org.springframework.web.bind.annotation.RequestParam;
 

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

    @GetMapping("/info/customerId/{customerId}")
    public ResponseEntity<GenericResponse<Customer>> getCustomerInfoById(@RequestParam Long customerId) {
        Optional<Customer> customerOpt = customerService.getCustomerById(customerId);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            customer.setPassword(null); 
            return ResponseEntity.ok(GenericResponse.success("Customer info retrieved successfully.", customer));
        } else {
            return ResponseEntity.status(404).body(GenericResponse.failure("Customer not found."));
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