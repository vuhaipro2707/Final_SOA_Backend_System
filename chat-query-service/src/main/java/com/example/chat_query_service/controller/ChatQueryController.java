package com.example.chat_query_service.controller;

import com.example.chat_query_service.document.ChatRoomView;
import com.example.chat_query_service.document.MessageDocument;
import com.example.chat_query_service.document.ReadMarker;
import com.example.chat_query_service.dto.GenericResponse;
import com.example.chat_query_service.service.ChatProjectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.example.chat_query_service.dto.UserOnlineStatus;
import com.example.chat_query_service.service.OnlineStatusService;

import java.util.List;

@RestController
public class ChatQueryController {

    private final ChatProjectionService chatProjectionService;
    private final OnlineStatusService onlineStatusService;

    public ChatQueryController(ChatProjectionService chatProjectionService, OnlineStatusService onlineStatusService) {
        this.chatProjectionService = chatProjectionService;
        this.onlineStatusService = onlineStatusService;
    }


    @GetMapping("/rooms")
    public ResponseEntity<GenericResponse<List<ChatRoomView>>> getRoomsForCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(GenericResponse.failure("Unauthorized or missing customer ID."));
        }

        try {
            Long customerId = Long.parseLong(authentication.getPrincipal().toString());
            
            List<ChatRoomView> rooms = chatProjectionService.getRoomsByCustomerId(customerId);

            if (rooms.isEmpty()) {
                return ResponseEntity.ok(GenericResponse.success("No rooms found for this user.", rooms));
            }

            return ResponseEntity.ok(GenericResponse.success("Rooms retrieved successfully.", rooms));
            
        } catch (NumberFormatException e) {
            return ResponseEntity.status(400).body(GenericResponse.failure("Invalid customer ID format."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/internal/rooms/customer/{customerId}")
    public ResponseEntity<GenericResponse<List<ChatRoomView>>> getRoomsByCustomerId(@PathVariable Long customerId) {
        try {
            List<ChatRoomView> rooms = chatProjectionService.getRoomsByCustomerId(customerId);
            return ResponseEntity.ok(GenericResponse.success("Rooms retrieved successfully for internal use.", rooms)); 
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred while fetching rooms: " + e.getMessage()));
        }
    }

    @GetMapping("/message/roomId/{roomId}")
    public ResponseEntity<GenericResponse<List<MessageDocument>>> getLatestMessages(@PathVariable Long roomId) {
        try {
            List<MessageDocument> messages = chatProjectionService.getLatestMessagesByRoomId(roomId);

            if (messages.isEmpty()) {
                return ResponseEntity.ok(GenericResponse.success("No messages found for this room."));
            }

            return ResponseEntity.ok(GenericResponse.success("Latest messages retrieved successfully.", messages));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/message/roomId/{roomId}/index/{indexMessageId}")
    public ResponseEntity<GenericResponse<List<MessageDocument>>> getNextMessagesByIndex(@PathVariable Long roomId, @PathVariable Long indexMessageId) {
        try {
            List<MessageDocument> messages = chatProjectionService.getNextMessagesByRoomIdAndIndex(roomId, indexMessageId);

            if (messages.isEmpty()) {
                return ResponseEntity.ok(GenericResponse.success("No more older messages found."));
            }

            return ResponseEntity.ok(GenericResponse.success("Next batch of older messages retrieved successfully.", messages));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/onlineStatus/roomId/{roomId}")
    public ResponseEntity<GenericResponse<List<UserOnlineStatus>>> getOnlineStatusByRoom(@PathVariable Long roomId) {
        try {
            List<UserOnlineStatus> statuses = onlineStatusService.getOnlineStatusForRoom(roomId);
            
            if (statuses.isEmpty()) {
                return ResponseEntity.ok(GenericResponse.success("No participants found for this room.", statuses));
            }
            
            return ResponseEntity.ok(GenericResponse.success("Online statuses retrieved successfully.", statuses));
            
        } catch (RuntimeException e) {
             return ResponseEntity.status(404).body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/readMarkers/roomId/{roomId}")
    public ResponseEntity<GenericResponse<List<ReadMarker>>> getReadMarkersByRoomId(@PathVariable Long roomId) {
        try {
            List<ReadMarker> markers = chatProjectionService.getReadMarkersByRoomId(roomId);
            return ResponseEntity.ok(GenericResponse.success("Read markers retrieved.", markers)); 
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred while fetching read markers: " + e.getMessage()));
        }
    }
}