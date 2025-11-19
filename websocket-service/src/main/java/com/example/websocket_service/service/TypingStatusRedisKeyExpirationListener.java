package com.example.websocket_service.service;

import com.example.websocket_service.dto.TypingMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;


@Component
public class TypingStatusRedisKeyExpirationListener extends KeyExpirationEventMessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    
    private static final String REDIS_TYPING_KEY_PREFIX = "typing:";
    private static final String TYPING_DESTINATION_PREFIX = "/topic/typing/roomId/";

    public TypingStatusRedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer, SimpMessagingTemplate messagingTemplate) {
        super(listenerContainer);
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    protected void doHandleMessage(Message message) {
        String expiredKey = message.toString();

        if (expiredKey.startsWith(REDIS_TYPING_KEY_PREFIX)) {
            String keyWithoutPrefix = expiredKey.substring(REDIS_TYPING_KEY_PREFIX.length());
            String[] parts = keyWithoutPrefix.split(":");

            if (parts.length == 2) {
                try {
                    Long roomId = Long.parseLong(parts[0]);
                    Long customerId = Long.parseLong(parts[1]);

                    String destination = TYPING_DESTINATION_PREFIX + roomId;
                    TypingMessage stopMessage = new TypingMessage(roomId, false, customerId);

                    messagingTemplate.convertAndSend(destination, stopMessage);

                    System.out.println("--- Redis Key Expired: Auto-stopped typing for Customer ID: " + customerId + " in Room ID: " + roomId);

                } catch (NumberFormatException e) {
                    System.err.println("Error parsing IDs from expired typing key: " + expiredKey);
                }
            } else {
                 System.err.println("Invalid format for expired typing key: " + expiredKey);
            }
        }
    }
}