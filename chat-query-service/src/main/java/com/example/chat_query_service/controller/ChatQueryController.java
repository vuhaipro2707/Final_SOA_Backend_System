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
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/internal/rooms/customerId/{customerId}")
    public ResponseEntity<GenericResponse<List<ChatRoomView>>> getRoomsByCustomerIdInternal(@PathVariable Long customerId) {
        try {
            List<ChatRoomView> rooms = chatProjectionService.getRoomsByCustomerId(customerId);
            return ResponseEntity.ok(GenericResponse.success("Rooms retrieved successfully for internal use.", rooms)); 
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred while fetching rooms: " + e.getMessage()));
        }
    }

    @GetMapping("/internal/valid/roomId/{roomId}/customerId/{customerId}")
    public ResponseEntity<GenericResponse<Boolean>> isParticipantOfRoomInternal(@PathVariable Long roomId, @PathVariable Long customerId) {
        boolean isMember = chatProjectionService.checkRoomMembership(roomId, customerId);
        
        if (isMember) {
            return ResponseEntity.ok(new GenericResponse<>(true, "Customer is a participant of the room", true));
        } else {
            return ResponseEntity.ok(new GenericResponse<>(false, "Customer is NOT a participant of the room", false));
        }
    }

    @GetMapping("/message/roomId/{roomId}")
    public ResponseEntity<GenericResponse<List<MessageDocument>>> getLatestMessages(@PathVariable Long roomId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(GenericResponse.failure("Unauthorized or missing customer ID."));
        }
        
        try {
            chatProjectionService.enforceRoomMembership(roomId, Long.parseLong(authentication.getPrincipal().toString()));
            List<MessageDocument> messages = chatProjectionService.getLatestMessagesByRoomId(roomId);

            if (messages.isEmpty()) {
                return ResponseEntity.ok(GenericResponse.success("No messages found for this room."));
            }

            return ResponseEntity.ok(GenericResponse.success("Latest messages retrieved successfully.", messages));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(GenericResponse.failure("Forbidden: " + e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/message/roomId/{roomId}/index/{indexMessageId}")
    public ResponseEntity<GenericResponse<List<MessageDocument>>> getNextMessagesByIndex(@PathVariable Long roomId, @PathVariable Long indexMessageId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(GenericResponse.failure("Unauthorized or missing customer ID."));
        }
        chatProjectionService.enforceRoomMembership(roomId, Long.parseLong(authentication.getPrincipal().toString()));

        try {
            List<MessageDocument> messages = chatProjectionService.getNextMessagesByRoomIdAndIndex(roomId, indexMessageId);

            if (messages.isEmpty()) {
                return ResponseEntity.ok(GenericResponse.success("No more older messages found."));
            }

            return ResponseEntity.ok(GenericResponse.success("Next batch of older messages retrieved successfully.", messages));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(GenericResponse.failure("Forbidden: " + e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/onlineStatus/roomId/{roomId}")
    public ResponseEntity<GenericResponse<List<UserOnlineStatus>>> getOnlineStatusByRoom(@PathVariable Long roomId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(GenericResponse.failure("Unauthorized or missing customer ID."));
        }

        try {
            chatProjectionService.enforceRoomMembership(roomId, Long.parseLong(authentication.getPrincipal().toString()));
            List<UserOnlineStatus> statuses = onlineStatusService.getOnlineStatusForRoom(roomId);
            
            if (statuses.isEmpty()) {
                return ResponseEntity.ok(GenericResponse.success("No participants found for this room.", statuses));
            }
            
            return ResponseEntity.ok(GenericResponse.success("Online statuses retrieved successfully.", statuses));
        } catch (ResponseStatusException e) {
             return ResponseEntity.status(e.getStatusCode()).body(GenericResponse.failure("Forbidden: " + e.getReason()));    
        } catch (RuntimeException e) {
             return ResponseEntity.status(404).body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/readMarkers/roomId/{roomId}")
    public ResponseEntity<GenericResponse<List<ReadMarker>>> getReadMarkersByRoomId(@PathVariable Long roomId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(GenericResponse.failure("Unauthorized or missing customer ID."));
        }

        try {
            chatProjectionService.enforceRoomMembership(roomId, Long.parseLong(authentication.getPrincipal().toString()));
            List<ReadMarker> markers = chatProjectionService.getReadMarkersByRoomId(roomId);
            return ResponseEntity.ok(GenericResponse.success("Read markers retrieved.", markers));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(GenericResponse.failure("Forbidden: " + e.getReason())); 
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("An internal error occurred while fetching read markers: " + e.getMessage()));
        }
    }
}