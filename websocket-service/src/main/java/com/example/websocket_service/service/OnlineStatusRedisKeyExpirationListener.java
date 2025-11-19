package com.example.websocket_service.service;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;


@Component
public class OnlineStatusRedisKeyExpirationListener extends KeyExpirationEventMessageListener {
    
    private final OnlineStatusService onlineStatusService;

    private static final String REDIS_ONLINE_KEY_PREFIX = "online_user:";

    public OnlineStatusRedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer, OnlineStatusService onlineStatusService) {
        super(listenerContainer); 
        this.onlineStatusService = onlineStatusService;
    }

    @Override
    protected void doHandleMessage(Message message) {
        String expiredKey = message.toString(); 

        if (expiredKey.startsWith(REDIS_ONLINE_KEY_PREFIX)) {
            String customerIdStr = expiredKey.substring(REDIS_ONLINE_KEY_PREFIX.length());
            try {
                Long customerId = Long.parseLong(customerIdStr);
                
                System.out.println("--- Redis Key Expired/Deleted: Detected OFFLINE for Customer ID: " + customerId);
                
                onlineStatusService.notifyStatusChange(customerId, false);
                
            } catch (NumberFormatException e) {
                System.err.println("Error parsing customer ID from expired key: " + expiredKey);
            }
        }
    }
}