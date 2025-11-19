package com.example.websocket_service.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
// import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import com.example.websocket_service.dto.TypingMessageRequest;
import com.example.websocket_service.service.OnlineStatusService;
import com.example.websocket_service.service.TypingStatusService;

@Controller
public class WebSocketController {

    private final OnlineStatusService onlineStatusService;
    private final TypingStatusService typingStatusService;

    public WebSocketController(OnlineStatusService onlineStatusService, TypingStatusService typingStatusService) {
        this.onlineStatusService = onlineStatusService;
        this.typingStatusService = typingStatusService;
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
            typingStatusService.renewTypingStatus(customerId, roomId);
        } else {
            typingStatusService.stopTypingStatus(customerId, roomId);
        }
    }
}