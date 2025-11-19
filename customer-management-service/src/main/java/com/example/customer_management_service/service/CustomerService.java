package com.example.customer_management_service.service;

import com.example.customer_management_service.dto.UpdateCustomerInfoRequest;
import com.example.customer_management_service.model.Customer;
import com.example.customer_management_service.repository.CustomerRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public Optional<Customer> getCustomerById(Long customerId) {
        return customerRepository.findByCustomerId(customerId);
    }

    public List<Customer> searchCustomerByPhoneNumber(String phoneNumber) {
        return customerRepository.findByPhoneNumberContaining(phoneNumber);
    }

    public List<Customer> searchCustomerByFullName(String fullName) {
        return customerRepository.findByFullNameContaining(fullName);
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
}