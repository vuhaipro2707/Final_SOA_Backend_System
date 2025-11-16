package com.example.chat_query_service.repository;

import com.example.chat_query_service.document.ReadMarker;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadMarkerRepository extends MongoRepository<ReadMarker, String> {
    Optional<ReadMarker> findByRoomIdAndCustomerId(Long roomId, Long customerId);
    List<ReadMarker> findByRoomId(Long roomId); 
}