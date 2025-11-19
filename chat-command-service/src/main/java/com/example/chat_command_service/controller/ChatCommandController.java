package com.example.chat_command_service.controller;

import com.example.chat_command_service.dto.SendMessageRequest;
import com.example.chat_command_service.dto.UpdateReadMarkerRequest;
import com.example.chat_command_service.service.ChatCommandService;
import com.example.chat_command_service.dto.CreateRoomRequest;
import com.example.chat_command_service.model.Room;
import org.springframework.security.core.Authentication;

import java.util.List;

import org.springframework.http.ResponseEntity;
import com.example.chat_command_service.dto.GenericResponse;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ChatCommandController {

    private final ChatCommandService chatCommandService;

    public ChatCommandController(ChatCommandService chatCommandService) {
        this.chatCommandService = chatCommandService;
    }

    @PostMapping("/message")
    public ResponseEntity<GenericResponse<Void>> sendMessage(@RequestBody SendMessageRequest request, Authentication authentication) {
        Long customerId = Long.parseLong(authentication.getPrincipal().toString());
        Long roomId = request.getRoomId();
        String content = request.getContent();
        if (roomId == null || content == null || content.isEmpty()) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("roomId and content cannot be empty."));
        }

        try {
            chatCommandService.enforceRoomMembership(roomId, customerId);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(GenericResponse.failure("Forbidden: " + e.getReason()));
        }
        
        Long senderId = Long.parseLong(authentication.getPrincipal().toString());
        
        chatCommandService.processNewMessage(
            roomId,
            senderId, 
            content
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

    @PostMapping("/read") 
    public ResponseEntity<GenericResponse<Void>> updateReadMarker(@RequestBody UpdateReadMarkerRequest request, Authentication authentication) {
        if (request.getRoomId() == null || request.getMessageId() == null) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("roomId and messageId cannot be empty."));
        }
        
        try {
            chatCommandService.enforceRoomMembership(request.getRoomId(), Long.parseLong(authentication.getPrincipal().toString()));
            Long customerId = Long.parseLong(authentication.getPrincipal().toString());
            
            chatCommandService.processReadMarkerUpdate(
                request.getRoomId(), 
                customerId, 
                request.getMessageId()
            );

            return ResponseEntity.ok(GenericResponse.success("Read Marker command (Write) processed and event published successfully."));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(GenericResponse.failure("Forbidden: " + e.getReason()));
        } catch (NumberFormatException e) {
             return ResponseEntity.status(400).body(GenericResponse.failure("Invalid customer ID format in authentication context."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }
}