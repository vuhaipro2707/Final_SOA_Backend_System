package com.example.websocket_service.kafka;

import com.example.websocket_service.document.ChatRoomView;
import com.example.websocket_service.kafka.dto.MessageSentEvent;
import com.example.websocket_service.kafka.dto.ReadMarkerEvent;
import com.example.websocket_service.kafka.dto.ReadStatusUpdateEvent;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    private final SimpMessagingTemplate messagingTemplate;

    public KafkaConsumerService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(
        topics = "${spring.kafka.topics.room-updated}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleRoomUpdate(ChatRoomView roomView) { 
        try {
            System.out.println("--- Đã nhận sự kiện RoomUpdatedEvent (View) từ Kafka cho Room ID: " + roomView.getRoomId());
            List<Long> participantIds = roomView.getParticipantIds(); 

            if (participantIds == null || participantIds.isEmpty()) {
                 System.err.println("RoomUpdatedEvent: Participant list is empty for room " + roomView.getRoomId());
                 return;
            }

            for (Long customerId : participantIds) {
                String destination = "/topic/rooms"; 
                messagingTemplate.convertAndSendToUser(
                    String.valueOf(customerId), 
                    destination, 
                    roomView
                );
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý RoomUpdatedEvent (View): " + e.getMessage());
        }
    }

    @KafkaListener(
        topics = "${spring.kafka.topics.message-sent}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMessageSent(MessageSentEvent event) {
        try {
            System.out.println("--- Đã nhận sự kiện MessageSentEvent từ Kafka cho Room ID: " + event.getRoomId());
            String destination = "/topic/message/roomId/" + event.getRoomId();
            
            messagingTemplate.convertAndSend(destination, event);

            System.out.println("--- Đã push tin nhắn mới (Message ID: " + event.getMessageId() + ") tới WebSocket destination: " + destination);

        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý MessageSentEvent: " + e.getMessage());
        }
    }

    @KafkaListener(
        topics = "${spring.kafka.topics.read-marker-updated}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleReadMarkerUpdated(ReadMarkerEvent event) {
        try {
            System.out.println("--- Đã nhận sự kiện ReadMarkerEvent từ Kafka cho Room ID: " + event.getRoomId());
            String destination = "/topic/readMarkers/roomId/" + event.getRoomId(); 
            
            messagingTemplate.convertAndSend(destination, event); 

            System.out.println("--- Đã push Read Marker (Customer ID: " + event.getCustomerId() + ") tới WebSocket destination: " + destination);

        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý ReadMarkerEvent: " + e.getMessage());
        }
    }

    @KafkaListener(
        topics = "${spring.kafka.topics.read-status-updated}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleReadStatusUpdate(ReadStatusUpdateEvent event) {
        try {
            System.out.println("--- Đã nhận sự kiện ReadStatusUpdateEvent (View) từ Kafka cho Room ID: " + event.getRoomId());
            
            messagingTemplate.convertAndSendToUser(
                event.getCustomerId().toString(),
                "/topic/readStatus",
                event
            );
        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý ReadStatusUpdateEvent (View): " + e.getMessage());
        }
    }
}