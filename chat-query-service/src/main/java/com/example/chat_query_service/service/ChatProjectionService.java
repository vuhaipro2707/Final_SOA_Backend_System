package com.example.chat_query_service.service;

import com.example.chat_query_service.document.ChatRoomView;
import com.example.chat_query_service.document.ChatRoomView.LastMessageInfo;
import com.example.chat_query_service.document.MessageDocument;
import com.example.chat_query_service.document.ReadMarker;
import com.example.chat_query_service.kafka.dto.MessageSentEvent;
import com.example.chat_query_service.kafka.dto.ReadMarkerEvent;
import com.example.chat_query_service.kafka.dto.RoomCreatedEvent;
import com.example.chat_query_service.repository.ChatRoomViewRepository;
import com.example.chat_query_service.repository.MessageDocumentRepository;
import com.example.chat_query_service.repository.ReadMarkerRepository;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatProjectionService {

    private final MessageDocumentRepository messageRepository;
    private final ChatRoomViewRepository chatRoomViewRepository;
    private final ReadMarkerRepository readMarkerRepository;

    public ChatProjectionService(MessageDocumentRepository messageRepository, ChatRoomViewRepository chatRoomViewRepository, ReadMarkerRepository readMarkerRepository) {
        this.messageRepository = messageRepository;
        this.chatRoomViewRepository = chatRoomViewRepository;
        this.readMarkerRepository = readMarkerRepository;
    }

    public void handleRoomCreatedEvent(RoomCreatedEvent event) {
        System.out.println("Processing RoomCreatedEvent for Room ID: " + event.getRoomId());
        
        List<Long> participantIds = event.getParticipants().stream()
                .map(dto -> dto.getId())
                .collect(Collectors.toList());

        ChatRoomView roomView = new ChatRoomView();
        roomView.setRoomId(event.getRoomId());
        roomView.setRoomName(event.getRoomName());
        roomView.setParticipantIds(participantIds);
        roomView.setCreatedAt(event.getCreatedAt()); 
        roomView.setUpdatedAt(event.getCreatedAt());
        roomView.setLastMessage(null); 
        
        chatRoomViewRepository.save(roomView);
        System.out.println("--- Projected Room (ID: " + event.getRoomId() + ") to MongoDB chatRoomViews.");
    }

    public void handleMessageSentEvent(MessageSentEvent event) {
        System.out.println("Processing MessageSentEvent for Room ID: " + event.getRoomId());

        Instant sentAt = event.getSentAt();

        MessageDocument messageDoc = new MessageDocument();
        messageDoc.setRoomId(event.getRoomId());
        messageDoc.setMessageId(event.getMessageId());
        messageDoc.setSenderId(event.getSenderId());
        messageDoc.setSenderFullName(event.getSenderFullName());
        messageDoc.setContent(event.getContent());
        messageDoc.setSentAt(sentAt);
        
        messageRepository.save(messageDoc);
        
        chatRoomViewRepository.findById(event.getRoomId()).ifPresent(roomView -> {
            LastMessageInfo lastMsg = new LastMessageInfo();
            lastMsg.setMessageId(event.getMessageId());
            lastMsg.setSenderId(event.getSenderId());
            lastMsg.setContent(event.getContent());
            lastMsg.setSentAt(sentAt);
            
            roomView.setLastMessage(lastMsg);
            roomView.setUpdatedAt(sentAt);
            
            chatRoomViewRepository.save(roomView);
            System.out.println("--- Projected Message (ID: " + event.getMessageId() + ") to MongoDB messages and updated ChatRoomView.");
        });
    }

    public void handleReadMarkerEvent(ReadMarkerEvent event) {
        System.out.println("Processing ReadMarkerEvent for Room ID: " + event.getRoomId() + ", Customer ID: " + event.getCustomerId());

        Long roomId = event.getRoomId();
        Long customerId = event.getCustomerId();
        
        Optional<ReadMarker> markerOpt = readMarkerRepository.findByRoomIdAndCustomerId(roomId, customerId);
        
        ReadMarker marker;
        if (markerOpt.isPresent()) {
            marker = markerOpt.get();
        } else {
            marker = new ReadMarker();
            marker.setRoomId(roomId);
            marker.setCustomerId(customerId);
        }

        marker.setLastReadMessageId(event.getLastReadMessageId());
        
        readMarkerRepository.save(marker);
        
        System.out.println("--- Projected Read Marker for (Room: " + roomId + ", Customer: " + customerId + ") to Message ID " + event.getLastReadMessageId() + ".");
    }

    public List<ChatRoomView> getRoomsByCustomerId(Long customerId) {
        return chatRoomViewRepository.findByParticipantIdsContainingOrderByUpdatedAtDesc(customerId);
    }

    public List<MessageDocument> getLatestMessagesByRoomId(Long roomId) {
        return messageRepository.findTop20ByRoomIdOrderByMessageIdDesc(roomId);
    }

    public List<MessageDocument> getNextMessagesByRoomIdAndIndex(Long roomId, Long indexMessageId) {
        return messageRepository.findTop20ByRoomIdAndMessageIdLessThanOrderByMessageIdDesc(roomId, indexMessageId);
    }

    public List<ReadMarker> getReadMarkersByRoomId(Long roomId) {
        return readMarkerRepository.findByRoomId(roomId);
    }
}