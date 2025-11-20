package com.example.customer_management_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private GatewayAuthFilter gatewayAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Tắt CSRF vì sử dụng stateless API
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Không sử dụng session
            
            // 1. Thêm Custom Filter
            .addFilterBefore(gatewayAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // 2. Cấu hình ủy quyền (Authorization)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/create/account").permitAll()
                // Tất cả các request quên mật khẩu (initiate, resend, confirm, reset)
                .requestMatchers(HttpMethod.POST, "/forgetPass/**").permitAll()

                // Cho phép tất cả các request đều phải được xác thực
                .anyRequest().authenticated() 
            )
            // .httpBasic(httpBasic -> httpBasic.disable()) // Tắt HTTP Basic nếu không dùng
            // .formLogin(formLogin -> formLogin.disable()) // Tắt Form Login nếu không dùng
        ;
        return http.build();
    }
}