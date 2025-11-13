package com.example.chat_query_service.repository;

import com.example.chat_query_service.document.ChatRoomView;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomViewRepository extends MongoRepository<ChatRoomView, Long> {
    List<ChatRoomView> findByParticipantIdsContainingOrderByUpdatedAtDesc(Long customerId);
}