package com.example.chat_command_service.kafka;

import com.example.chat_command_service.kafka.dto.MessageSentEvent;
import com.example.chat_command_service.kafka.dto.ReadMarkerEvent;
import com.example.chat_command_service.kafka.dto.RoomCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    @Value("${spring.kafka.topics.message-sent}")
    private String messageSentTopic; 

    @Value("${spring.kafka.topics.room-created}")
    private String roomCreatedTopic;

    @Value("${spring.kafka.topics.read-marker-updated}")
    private String readMarkerTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessageSentEvent(MessageSentEvent event) {
        kafkaTemplate.send(messageSentTopic, event.getRoomId().toString(), event);
        System.out.println("--- Đã gửi sự kiện MessageSentEvent cho Room ID: " + event.getRoomId());
    }
    
    public void sendRoomCreatedEvent(RoomCreatedEvent event) {
        kafkaTemplate.send(roomCreatedTopic, event.getRoomId().toString(), event);
        System.out.println("--- Đã gửi sự kiện RoomCreatedEvent cho Room ID: " + event.getRoomId());
    }

    public void sendReadMarkerEvent(ReadMarkerEvent event) {
        kafkaTemplate.send(readMarkerTopic, event.getCustomerId().toString(), event); 
        System.out.println("--- Đã gửi sự kiện ReadMarkerEvent cho Customer ID " + event.getCustomerId() + " trong Room ID: " + event.getRoomId());
    }
}