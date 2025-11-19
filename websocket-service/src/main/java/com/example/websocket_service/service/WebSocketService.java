package com.example.websocket_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;
import com.example.websocket_service.dto.GenericResponse; 

@Service
public class WebSocketService {
    private static final String CHAT_QUERY_SERVICE_BASE_URL = "http://chat-query-service:8086";
    private final WebClient webClient;

    public WebSocketService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public boolean isParticipantOfRoom(Long roomId, Long customerId) {
        if (roomId == null || customerId == null) {
            return false;
        }

        String uri = CHAT_QUERY_SERVICE_BASE_URL + "/internal/valid/roomId/" + roomId + "/customerId/" + customerId;
        
        try {
            ParameterizedTypeReference<GenericResponse<Boolean>> typeRef = 
                new ParameterizedTypeReference<GenericResponse<Boolean>>() {};

            GenericResponse<Boolean> response = webClient.get()
                .uri(uri)
                .header("X-Customer-Id", String.valueOf(customerId)) 
                .retrieve()
                .bodyToMono(typeRef)
                .block();

            return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());

        } catch (Exception e) {
            System.err.println("Error calling chat-query-service check membership internal API for customer " + customerId + ", Room ID " + roomId + ": " + e.getMessage());
            return false; 
        }
    }
}
