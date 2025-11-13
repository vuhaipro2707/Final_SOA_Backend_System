package com.example.chat_command_service.repository;

import com.example.chat_command_service.model.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {
}