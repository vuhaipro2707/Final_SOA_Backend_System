package com.example.websocket_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {
    @Bean
    RedisMessageListenerContainer keyExpirationListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);
        // Khi RedisKeyExpirationListener (KeyspaceEventMessageListener) được khởi tạo,
        // nó sẽ tự động chạy lệnh CONFIG SET notify-keyspace-events Kx (cho key expired)
        return listenerContainer;
    }
}