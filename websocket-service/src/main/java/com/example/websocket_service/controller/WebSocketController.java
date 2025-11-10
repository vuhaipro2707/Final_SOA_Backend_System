package com.example.websocket_service.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
// import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/echo")
    @SendTo("/topic/echo") 
    public String echoMessage(String message, SimpMessageHeaderAccessor headerAccessor) {
        String customerId = headerAccessor.getUser().getName();
        return "Server echoed (ID: " + customerId + "): " + message;
    }
}