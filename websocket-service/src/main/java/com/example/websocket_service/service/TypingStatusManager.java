package com.example.websocket_service.service;

import com.example.websocket_service.dto.TypingMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class TypingStatusManager {
    private final ConcurrentHashMap<String, ScheduledFuture<?>> typingTimers = new ConcurrentHashMap<>();
    private final TaskScheduler taskScheduler;
    private final SimpMessagingTemplate messagingTemplate;

    private static final long EXPIRY_MS = 5000; 
    
    private static final String TYPING_DESTINATION_PREFIX = "/topic/typing/roomId/";
    
    public TypingStatusManager(TaskScheduler taskScheduler, SimpMessagingTemplate messagingTemplate) {
        this.taskScheduler = taskScheduler;
        this.messagingTemplate = messagingTemplate;
    }

    private String getCompositeKey(Long userId, Long roomId) {
        return userId + ":" + roomId;
    }

    public void renewTypingStatus(Long userId, Long roomId) {
        String key = getCompositeKey(userId, roomId);

        ScheduledFuture<?> oldTimer = typingTimers.get(key);
        if (oldTimer != null) {
            oldTimer.cancel(false); 
            typingTimers.remove(key, oldTimer);
        } else {
            String destination = TYPING_DESTINATION_PREFIX + roomId;
            TypingMessage typingMessage = new TypingMessage(roomId, true, userId);
            messagingTemplate.convertAndSend(destination, typingMessage);
            System.out.println("--- User " + userId + " started typing in room " + roomId);
        }

        Runnable autoStopTask = () -> {
            autoStopTyping(userId, roomId);
        };

        ScheduledFuture<?> newTimer = taskScheduler.schedule(
            autoStopTask, 
            new java.util.Date(System.currentTimeMillis() + EXPIRY_MS)
        );

        typingTimers.put(key, newTimer);
    }
    
    private void autoStopTyping(Long userId, Long roomId) {
        String key = getCompositeKey(userId, roomId);
        if (typingTimers.remove(key) != null) {
            String destination = TYPING_DESTINATION_PREFIX + roomId;
            TypingMessage stopMessage = new TypingMessage(roomId, false, userId);
            
            messagingTemplate.convertAndSend(destination, stopMessage);
            
            System.out.println("--- Auto-stopped typing status for user " + userId + " in room " + roomId);
        }
    }
    
    public void stopTypingStatus(Long userId, Long roomId) {
        String key = getCompositeKey(userId, roomId);
        ScheduledFuture<?> timer = typingTimers.remove(key);
        if (timer != null) {
            timer.cancel(false);
        }

        String destination = TYPING_DESTINATION_PREFIX + roomId;
        TypingMessage stopMessage = new TypingMessage(roomId, false, userId);
        messagingTemplate.convertAndSend(destination, stopMessage);

        System.out.println("--- Explicitly stopped typing status for user " + userId + " in room " + roomId);
    }
}