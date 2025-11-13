package com.example.chat_command_service.controller;

import com.example.chat_command_service.dto.SendMessageRequest;
import com.example.chat_command_service.service.ChatCommandService;
import com.example.chat_command_service.dto.CreateRoomRequest;
import com.example.chat_command_service.model.Room;
import org.springframework.security.core.Authentication;

import java.util.List;

import org.springframework.http.ResponseEntity;
import com.example.chat_command_service.dto.GenericResponse;

import org.springframework.web.bind.annotation.*;

@RestController
public class ChatCommandController {

    private final ChatCommandService chatCommandService;

    public ChatCommandController(ChatCommandService chatCommandService) {
        this.chatCommandService = chatCommandService;
    }

    @PostMapping("/message")
    public ResponseEntity<GenericResponse<Void>> sendMessage(@RequestBody SendMessageRequest request, Authentication authentication) {
        if (request.getRoomId() == null || request.getContent() == null || request.getContent().isEmpty()) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("roomId and content cannot be empty."));
        }
        
        Long senderId = Long.parseLong(authentication.getPrincipal().toString());
        
        chatCommandService.processNewMessage(
            request.getRoomId(), 
            senderId, 
            request.getContent()
        );

        return ResponseEntity.ok(GenericResponse.success("Message command (Write) processed and event published successfully."));
    }

    @PostMapping("/room")
    public ResponseEntity<GenericResponse<Long>> createRoom(@RequestBody CreateRoomRequest request, Authentication authentication) {
        List<Long> targetCustomerIds = request.getTargetCustomerIds();
        Long customerId = Long.parseLong(authentication.getPrincipal().toString());
        if (request.getRoomName() == null || targetCustomerIds == null || request.getRoomName().isEmpty()) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("roomName and targetCustomerIds cannot be empty."));
        }

        if (targetCustomerIds.contains(customerId)) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("targetCustomerIds should not contain the creator's customerId."));
        }

        if (targetCustomerIds.size() < 1) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("At least one target customer is required to create a room."));
        }
        
        targetCustomerIds.add(customerId);

        Room newRoom = chatCommandService.processNewRoom(
            request.getRoomName(),
            customerId,
            targetCustomerIds
        );

        return ResponseEntity.ok(GenericResponse.success("Room command (Write) processed and event published successfully.", newRoom.getRoomId()));
    }
}