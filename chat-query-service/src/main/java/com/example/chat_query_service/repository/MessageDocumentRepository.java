package com.example.chat_query_service.repository;

import com.example.chat_query_service.document.MessageDocument;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageDocumentRepository extends MongoRepository<MessageDocument, Long> {
    List<MessageDocument> findTop20ByRoomIdOrderByMessageIdDesc(Long roomId);
    List<MessageDocument> findTop20ByRoomIdAndMessageIdLessThanOrderByMessageIdDesc(Long roomId, Long messageId);
}