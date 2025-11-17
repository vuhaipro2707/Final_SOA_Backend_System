package com.example.chat_command_service.service;

import com.example.chat_command_service.model.Message;
import com.example.chat_command_service.model.Room;
import com.example.chat_command_service.model.RoomParticipant;
import com.example.chat_command_service.repository.MessageRepository;
import com.example.chat_command_service.repository.RoomParticipantRepository;
import com.example.chat_command_service.repository.RoomRepository;
import com.example.chat_command_service.kafka.dto.MessageSentEvent;
import com.example.chat_command_service.kafka.dto.ReadMarkerEvent;
import com.example.chat_command_service.kafka.dto.RoomCreatedEvent;
import com.example.chat_command_service.kafka.dto.RoomCreatedEvent.ParticipantDTO;
import com.example.chat_command_service.kafka.KafkaProducerService;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.chat_command_service.dto.GenericResponse;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatCommandService {
    private final MessageRepository messageRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final RoomRepository roomRepository;
    private final KafkaProducerService kafkaProducerService;
    private final WebClient webClient;

    private static final String CUSTOMER_SERVICE_BASE_URL = "http://customer-management-service:8084";

    public ChatCommandService(MessageRepository messageRepository, RoomRepository roomRepository, KafkaProducerService kafkaProducerService, RoomParticipantRepository roomParticipantRepository, WebClient.Builder webClientBuilder) {
        this.messageRepository = messageRepository;
        this.roomParticipantRepository = roomParticipantRepository;
        this.roomRepository = roomRepository;
        
        this.kafkaProducerService = kafkaProducerService;
        this.webClient = webClientBuilder.build();
    }
    
    @Transactional
    public Message processNewMessage(Long roomId, Long senderId, String content) {
        Message message = new Message();
        message.setRoomId(roomId);
        message.setCustomerId(senderId);
        message.setContent(content);
        message.setSentAt(Instant.now());
        
        message = messageRepository.save(message);
        String senderFullName = getCustomerFullName(senderId); 

        
        MessageSentEvent event = new MessageSentEvent(
            message.getMessageId(),
            message.getRoomId(),
            message.getCustomerId(),
            senderFullName, 
            message.getContent(),
            message.getSentAt()
        );
        
        kafkaProducerService.sendMessageSentEvent(event);
        
        return message;
    }

    @Transactional
    public Room processNewRoom(String roomName, Long creatorId, List<Long> targetCustomerIds) {
        
        Instant now = Instant.now();
        List<Long> allParticipantIds = new ArrayList<>(targetCustomerIds);
        if (!allParticipantIds.contains(creatorId)) {
             allParticipantIds.add(creatorId);
        }

        Room room = new Room();
        room.setRoomName(roomName);
        room.setCreatedBy(creatorId);
        room.setCreatedAt(now);

        room = roomRepository.save(room);
        Long newRoomId = room.getRoomId();

        List<RoomParticipant> participants = new ArrayList<>();
        List<ParticipantDTO> participantDTOs = new ArrayList<>();

        for (Long customerId : allParticipantIds) {
            RoomParticipant participant = new RoomParticipant();
            participant.setRoomId(newRoomId);
            participant.setCustomerId(customerId);
            participant.setJoinedAt(now);
            participant.setLastReadMessageId(null);
            participants.add(participant);
            
            String fullName = getCustomerFullName(customerId); 
            
            ParticipantDTO dto = new ParticipantDTO(customerId, fullName, now);
            participantDTOs.add(dto);
        }
        roomParticipantRepository.saveAll(participants);

        RoomCreatedEvent event = new RoomCreatedEvent(
            newRoomId,
            roomName,
            creatorId,
            now,
            participantDTOs
        );

        kafkaProducerService.sendRoomCreatedEvent(event);

        return room;
    }

    @Transactional
    public void processReadMarkerUpdate(Long roomId, Long customerId, Long messageId) {
        roomParticipantRepository.updateLastReadMessageId(roomId, customerId, messageId);
        
        System.out.println("--- Updated RoomParticipant for Customer ID " + customerId + " in Room ID " + roomId + " to Message ID " + messageId);
        
        ReadMarkerEvent event = new ReadMarkerEvent(roomId, customerId, messageId);
        
        kafkaProducerService.sendReadMarkerEvent(event);
        
        System.out.println("--- Processed Read Marker Update for Customer ID " + customerId + " in Room ID " + roomId + " with Message ID " + messageId);
    }

    private Mono<GenericResponse<String>> createGetCustomerFullNameMono(Long customerId) {
        return webClient.get()
            .uri(CUSTOMER_SERVICE_BASE_URL + "/fullName/customerId/" + customerId) 
            .header("X-Customer-Id", String.valueOf(customerId)) 
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse -> 
                clientResponse.bodyToMono(new ParameterizedTypeReference<GenericResponse<String>>() {})
                    .flatMap(response -> {
                        String message = response.getMessage() != null ? response.getMessage() : "Unknown error";
                        return Mono.error(new RuntimeException("Customer Full Name Fetch Failed: " + message));
                    })
            )
            .bodyToMono(new ParameterizedTypeReference<GenericResponse<String>>() {}); 
    }

    private String getCustomerFullName(Long customerId) {
        try {
            Mono<GenericResponse<String>> mono = createGetCustomerFullNameMono(customerId);
            GenericResponse<String> response = mono.block(); 
            
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData(); 
            }
            
            String message = response != null && response.getMessage() != null ? 
                             response.getMessage() : 
                             "Customer full name could not be retrieved.";
            throw new RuntimeException("Missing or invalid 'fullName' in customer record: " + message);
            
        } catch (Exception e) {
            System.err.println("Failed to fetch customer name for ID " + customerId + ": " + e.getMessage());
            throw new RuntimeException("Failed to retrieve user information for ID: " + customerId, e);
        }
    }
}