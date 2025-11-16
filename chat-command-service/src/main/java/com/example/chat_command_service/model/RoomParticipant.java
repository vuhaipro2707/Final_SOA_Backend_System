package com.example.chat_command_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(
    name = "room_participants",
    indexes = {
        @Index(name = "idx_room_customer", columnList = "roomId, customerId", unique = true) 
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Instant joinedAt;

    @Column(nullable = true)
    private Long lastReadMessageId; 
}