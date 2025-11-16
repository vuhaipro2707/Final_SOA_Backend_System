package com.example.websocket_service.service;

import com.example.websocket_service.dto.UserOnlineStatus;
import com.example.websocket_service.document.ChatRoomViewDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;
import com.example.websocket_service.dto.GenericResponse; 

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OnlineStatusService {

    private static final String REDIS_ONLINE_KEY_PREFIX = "online_user:";
    private static final long TTL_SECONDS = 300; // 5 minutes
    private static final String CHAT_QUERY_SERVICE_BASE_URL = "http://chat-query-service:8086";

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebClient webClient;

    public OnlineStatusService(StringRedisTemplate redisTemplate, SimpMessagingTemplate messagingTemplate, WebClient.Builder webClientBuilder) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.webClient = webClientBuilder.build();
    }

    public void extendOnline(Long customerId) {
        String key = REDIS_ONLINE_KEY_PREFIX + customerId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS);
            System.out.println("Extended online status for user " + customerId);
        } else {
            redisTemplate.opsForValue().set(key, "", TTL_SECONDS, TimeUnit.SECONDS);
            System.out.println("Created and extended online status for user " + customerId);
        }
    }

    public void setOnline(Long customerId, String sessionId) {
        String key = REDIS_ONLINE_KEY_PREFIX + customerId;
        redisTemplate.opsForValue().set(key, sessionId, TTL_SECONDS, TimeUnit.SECONDS);
        notifyStatusChange(customerId, true);
    }

    public void setOffline(Long customerId) {
        String key = REDIS_ONLINE_KEY_PREFIX + customerId;
        redisTemplate.delete(key);
        notifyStatusChange(customerId, false);
    }

    private List<Long> getRoomIdsForUser(Long customerId) {
        String uri = CHAT_QUERY_SERVICE_BASE_URL + "/internal/rooms/customer/" + customerId;
        try {
            GenericResponse<List<ChatRoomViewDTO>> response = webClient.get()
                .uri(uri)
                .header("X-Customer-Id", String.valueOf(customerId)) 
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<GenericResponse<List<ChatRoomViewDTO>>>() {})
                .block();

            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData().stream()
                        .map(ChatRoomViewDTO::getRoomId)
                        .collect(Collectors.toList());
            }
            System.err.println("Failed to fetch rooms from chat-query-service for customer " + customerId + ". Message: " + (response != null ? response.getMessage() : "No response body."));
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error calling chat-query-service for rooms for customer " + customerId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void notifyStatusChange(Long customerId, boolean online) {
        List<Long> roomIds = getRoomIdsForUser(customerId);
        
        UserOnlineStatus status = new UserOnlineStatus(customerId, online);

        for (Long roomId : roomIds) {
            String destination = "/topic/onlineStatus/roomId/" + roomId;
            messagingTemplate.convertAndSend(destination, status);
            System.out.println("--- Notified online status (" + (online ? "ONLINE" : "OFFLINE") + ") for user " + customerId + " to room " + roomId + " at " + destination);
        }
    }
}