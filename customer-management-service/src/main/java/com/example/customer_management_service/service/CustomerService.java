package com.example.customer_management_service.service;

import com.example.customer_management_service.model.Customer;
import com.example.customer_management_service.repository.CustomerRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public Optional<Customer> getCustomerById(Long customerId) {
        return customerRepository.findByCustomerId(customerId);
    }
}