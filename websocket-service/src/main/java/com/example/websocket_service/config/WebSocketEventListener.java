package com.example.websocket_service.config;

import com.example.websocket_service.service.OnlineStatusService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;

@Component
public class WebSocketEventListener {

    private final OnlineStatusService onlineStatusService;

    public WebSocketEventListener(OnlineStatusService onlineStatusService) {
        this.onlineStatusService = onlineStatusService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        System.out.println("Received a new web socket connection");
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Optional.ofNullable(headerAccessor.getUser())
            .map(user -> user.getName())
            .map(Long::parseLong)
            .ifPresent(customerId -> {
                String sessionId = headerAccessor.getSessionId();
                System.out.println("User Connected: " + customerId + " with Session ID: " + sessionId);
                onlineStatusService.setOnline(customerId, sessionId);
            });
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Optional.ofNullable(headerAccessor.getUser())
            .map(user -> user.getName())
            .map(Long::parseLong)
            .ifPresent(customerId -> {
                System.out.println("User Disconnected: " + customerId);
                onlineStatusService.setOffline(customerId);
            });
    }
}