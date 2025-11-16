package com.example.websocket_service.kafka;

import com.example.websocket_service.kafka.dto.MessageSentEvent;
import com.example.websocket_service.kafka.dto.ReadMarkerEvent;

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
}