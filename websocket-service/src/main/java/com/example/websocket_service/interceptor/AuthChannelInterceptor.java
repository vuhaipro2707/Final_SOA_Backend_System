package com.example.websocket_service.interceptor;

import com.example.websocket_service.jwt.JwtDecoder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
// import java.util.List;

@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtDecoder jwtDecoder;

    // @Override // For Production (use Cookie to store JWT)
    // public Message<?> preSend(Message<?> message, MessageChannel channel) {
    //     System.err.println("Intercepting WebSocket message for authentication.");
    //     System.err.flush();
    //     StompHeaderAccessor accessor = 
    //         MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
    //     if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            
    //         List<String> cookieHeaders = accessor.getNativeHeader("Cookie");
    //         String jwtToken = null;
            
    //         if (cookieHeaders != null && !cookieHeaders.isEmpty()) {
    //             for (String cookieHeader : cookieHeaders) {
    //                 String[] cookies = cookieHeader.split(";\\s*");
    //                 for (String cookie : cookies) {
    //                     if (cookie.startsWith("jwt_token=")) {
    //                         jwtToken = cookie.substring("jwt_token=".length());
    //                         break;
    //                     }
    //                 }
    //                 if (jwtToken != null) break;
    //             }
    //         }

    //         if (jwtToken == null) {
    //             System.err.println("Authentication failed: JWT token not found in Cookie.");
    //             throw new SecurityException("No authentication cookie.");
    //         }

    //         try {
    //             Jws<Claims> jws = jwtDecoder.parseToken(jwtToken);
    //             String customerId = jws.getBody().get("customerId", String.class);
                
    //             if (customerId == null) {
    //                 throw new SecurityException("Invalid JWT claims: Missing customerId.");
    //             }

    //             Authentication authentication = new UsernamePasswordAuthenticationToken(
    //                 customerId, 
    //                 null,
    //                 Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
    //             );

    //             accessor.setUser(authentication);

    //         } catch (JwtException e) {
    //             System.err.println("Authentication failed: Invalid JWT token. Reason: " + e.getMessage());
    //             throw new SecurityException("Invalid authentication token.");
    //         } catch (Exception e) {
    //             System.err.println("Authentication failed: General error: " + e.getMessage());
    //             throw new SecurityException("Authentication failed due to internal error.");
    //         }
    //     }
        
    //     return message;
    // }

    @Override // For Testing (JwT through STOMP Header)
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        System.err.println("Intercepting WebSocket message for authentication.");
        System.err.flush();
        StompHeaderAccessor accessor = 
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String jwtToken = accessor.getFirstNativeHeader("jwt_token");

            if (jwtToken == null) {
                System.err.println("Authentication failed: JWT token not found in STOMP header.");
                throw new SecurityException("No authentication token."); 
            }

            try {
                Jws<Claims> jws = jwtDecoder.parseToken(jwtToken);
                String customerId = jws.getBody().get("customerId", String.class);
                
                if (customerId == null) {
                    throw new SecurityException("Invalid JWT claims: Missing customerId.");
                }

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    customerId, 
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );

                accessor.setUser(authentication);

            } catch (JwtException e) {
                System.err.println("Authentication failed: Invalid JWT token. Reason: " + e.getMessage());
                throw new SecurityException("Invalid authentication token.");
            } catch (Exception e) {
                System.err.println("Authentication failed: General error: " + e.getMessage());
                throw new SecurityException("Authentication failed due to internal error.");
            }
        }
        
        return message;
    }
}