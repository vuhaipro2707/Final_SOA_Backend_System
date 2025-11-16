package com.example.chat_query_service.kafka;

import com.example.chat_query_service.kafka.dto.MessageSentEvent;
import com.example.chat_query_service.kafka.dto.ReadMarkerEvent;
import com.example.chat_query_service.kafka.dto.RoomCreatedEvent;
import com.example.chat_query_service.service.ChatProjectionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private final ChatProjectionService projectionService;

    public KafkaConsumerService(ChatProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @KafkaListener(
        topics = "${spring.kafka.topics.room-created}", 
        groupId = "${spring.kafka.consumer.group-id-room}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleRoomCreated(RoomCreatedEvent event) {
        try {
            projectionService.handleRoomCreatedEvent(event);
        } catch (Exception e) {
            System.err.println("Error processing RoomCreatedEvent for Room ID " + event.getRoomId() + ": " + e.getMessage());
        }
    }

    @KafkaListener(
        topics = "${spring.kafka.topics.message-sent}", 
        groupId = "${spring.kafka.consumer.group-id-message}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMessageSent(MessageSentEvent event) {
        try {
            projectionService.handleMessageSentEvent(event);
        } catch (Exception e) {
            System.err.println("Error processing MessageSentEvent for Message ID " + event.getMessageId() + ": " + e.getMessage());
        }
    }

    @KafkaListener(
        topics = "${spring.kafka.topics.read-marker-updated}", 
        groupId = "${spring.kafka.consumer.group-id-read-marker}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleReadMarker(ReadMarkerEvent event) {
        try {
            projectionService.handleReadMarkerEvent(event);
        } catch (Exception e) {
            System.err.println("Error processing ReadMarkerEvent for Room ID " + event.getRoomId() + ", Customer ID " + event.getCustomerId() + ": " + e.getMessage());
        }
    }
}