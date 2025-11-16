package com.example.chat_query_service.service;

import com.example.chat_query_service.document.ChatRoomView;
import com.example.chat_query_service.dto.UserOnlineStatus;
import com.example.chat_query_service.repository.ChatRoomViewRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OnlineStatusService {

    private static final String REDIS_ONLINE_KEY_PREFIX = "online_user:";
    private final ChatRoomViewRepository chatRoomViewRepository;
    private final StringRedisTemplate redisTemplate;

    public OnlineStatusService(ChatRoomViewRepository chatRoomViewRepository, StringRedisTemplate redisTemplate) {
        this.chatRoomViewRepository = chatRoomViewRepository;
        this.redisTemplate = redisTemplate;
    }

    public List<UserOnlineStatus> getOnlineStatusForRoom(Long roomId) {
        Optional<ChatRoomView> roomViewOpt = chatRoomViewRepository.findById(roomId);

        if (roomViewOpt.isEmpty()) {
            throw new RuntimeException("Chat room not found with ID: " + roomId);
        }

        List<Long> participantIds = roomViewOpt.get().getParticipantIds();
        
        return participantIds.stream()
            .map(id -> {
                boolean isOnline = Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_ONLINE_KEY_PREFIX + id));
                return new UserOnlineStatus(id, isOnline);
            })
            .collect(Collectors.toList());
    }
}