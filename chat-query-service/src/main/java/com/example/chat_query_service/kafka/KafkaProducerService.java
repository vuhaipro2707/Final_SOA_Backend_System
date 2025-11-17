package com.example.chat_query_service.kafka;

import com.example.chat_query_service.document.ChatRoomView;
import com.example.chat_query_service.kafka.dto.ReadStatusUpdateEvent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    @Value("${spring.kafka.topics.room-updated}")
    private String roomUpdatedTopic;

    @Value("${spring.kafka.topics.read-status}")
    private String readStatusTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendRoomUpdatedEvent(ChatRoomView roomView) {
        String roomKey = roomView.getRoomId().toString(); 
        kafkaTemplate.send(roomUpdatedTopic, roomKey, roomView);
        System.out.println("--- Đã gửi sự kiện RoomUpdatedEvent (View) cho Room ID: " + roomView.getRoomId());
    }

    public void sendReadStatusUpdatedEvent(Long roomId, Long customerId, Boolean isUnread) {
        ReadStatusUpdateEvent event = new ReadStatusUpdateEvent(roomId, customerId, isUnread);
        System.out.println("Sending ReadStatusUpdateEvent to Kafka topic " + readStatusTopic + " for Customer ID: " + customerId);
        kafkaTemplate.send(readStatusTopic, String.valueOf(roomId), event);
    }
}