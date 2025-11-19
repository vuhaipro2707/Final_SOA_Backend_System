package com.example.websocket_service.service;

import com.example.websocket_service.dto.TypingMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TypingStatusService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    private static final long EXPIRY_SECONDS = 2; 
    public static final String REDIS_TYPING_KEY_PREFIX = "typing:"; 
    private static final String TYPING_DESTINATION_PREFIX = "/topic/typing/roomId/";

    public TypingStatusService(StringRedisTemplate redisTemplate, SimpMessagingTemplate messagingTemplate) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
    }

    private String getCompositeKey(Long userId, Long roomId) {
        return REDIS_TYPING_KEY_PREFIX + roomId + ":" + userId; 
    }

    public void renewTypingStatus(Long userId, Long roomId) {
        String key = getCompositeKey(userId, roomId);

        Boolean exists = redisTemplate.hasKey(key);
        
        if (exists == null || !exists) {
            String destination = TYPING_DESTINATION_PREFIX + roomId;
            TypingMessage typingMessage = new TypingMessage(roomId, true, userId);
            messagingTemplate.convertAndSend(destination, typingMessage);
            System.out.println("--- User " + userId + " started typing in room " + roomId + " (Redis key set)");
        }
        
        redisTemplate.opsForValue().set(key, "", EXPIRY_SECONDS, TimeUnit.SECONDS);
    }
    
    public void stopTypingStatus(Long userId, Long roomId) {
        String key = getCompositeKey(userId, roomId);
        
        Boolean wasTyping = redisTemplate.hasKey(key);
        
        redisTemplate.delete(key);
        
        if (Boolean.TRUE.equals(wasTyping)) {
            String destination = TYPING_DESTINATION_PREFIX + roomId;
            TypingMessage stopMessage = new TypingMessage(roomId, false, userId);
            messagingTemplate.convertAndSend(destination, stopMessage);
            System.out.println("--- Explicitly stopped typing status for user " + userId + " in room " + roomId);
        }
    }
}