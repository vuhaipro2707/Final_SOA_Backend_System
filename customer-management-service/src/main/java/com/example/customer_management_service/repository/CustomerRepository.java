package com.example.customer_management_service.repository;

import com.example.customer_management_service.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerId(Long customerId);
    List<Customer> findByPhoneNumberContaining(String phoneNumber);
    List<Customer> findByEmailOrPhoneNumber(String email, String phoneNumber);
    List<Customer> findByFullNameContainingIgnoreCase(String fullName);

    Optional<Customer> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}