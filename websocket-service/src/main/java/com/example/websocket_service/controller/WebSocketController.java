package com.example.websocket_service.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
// import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import com.example.websocket_service.dto.TypingMessageRequest;
import com.example.websocket_service.service.OnlineStatusService;
import com.example.websocket_service.service.TypingStatusManager;

@Controller
public class WebSocketController {

    private final OnlineStatusService onlineStatusService;
    private final TypingStatusManager typingStatusManager;

    public WebSocketController(OnlineStatusService onlineStatusService, TypingStatusManager typingStatusManager) {
        this.onlineStatusService = onlineStatusService;
        this.typingStatusManager = typingStatusManager;
    }

    @MessageMapping("/echo")
    @SendTo("/topic/echo") 
    public String echoMessage(String message, SimpMessageHeaderAccessor headerAccessor) {
        String customerId = headerAccessor.getUser().getName();
        return "Server echoed (ID: " + customerId + "): " + message;
    }

    @MessageMapping("/extendOnline")
    public void extendOnline(SimpMessageHeaderAccessor headerAccessor) {
        String customerId = headerAccessor.getUser().getName();
        onlineStatusService.extendOnline(Long.parseLong(customerId));
    }

    @MessageMapping("/typing")
    public void handleTypingStatus(TypingMessageRequest message, SimpMessageHeaderAccessor headerAccessor) {
        String customerIdStr = headerAccessor.getUser().getName();
        Long customerId = Long.parseLong(customerIdStr);
        Long roomId = message.getRoomId();
        System.out.println("--- Received typing status from user " + customerId + " in room " + roomId + ": isTyping=" + message.getIsTyping());
        if (message.getIsTyping() == 1) {
            typingStatusManager.renewTypingStatus(customerId, roomId);
        } else {
            typingStatusManager.stopTypingStatus(customerId, roomId);
        }
    }
}