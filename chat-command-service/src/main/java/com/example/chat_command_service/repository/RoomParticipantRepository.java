package com.example.chat_command_service.repository;

import com.example.chat_command_service.model.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {
    @Modifying
    @Query("UPDATE RoomParticipant rp SET rp.lastReadMessageId = :messageId, rp.joinedAt = CURRENT_TIMESTAMP " +
           "WHERE rp.roomId = :roomId AND rp.customerId = :customerId " +
           "AND (rp.lastReadMessageId IS NULL OR rp.lastReadMessageId < :messageId)") 
    int updateLastReadMessageId(@Param("roomId") Long roomId, @Param("customerId") Long customerId, @Param("messageId") Long messageId);

    Optional<RoomParticipant> findByRoomIdAndCustomerId(Long roomId, Long customerId);
}