package com.example.chat_command_service.config;

import com.example.chat_command_service.model.Room;
import com.example.chat_command_service.repository.RoomRepository;
import com.example.chat_command_service.service.ChatCommandService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Lớp khởi tạo dữ liệu mẫu cho Chat Command Service.
 * Tự động chạy khi ứng dụng khởi động.
 */
@Configuration
public class DataInitializer {

    // // Giữ RoomRepository chỉ để kiểm tra xem DB đã có dữ liệu chưa
    // private final RoomRepository roomRepository;
    
    // // <-- THAY THẾ Repository bằng Service để kích hoạt Event
    // private final ChatCommandService chatCommandService; 

    // // Sử dụng Constructor Injection
    // public DataInitializer(RoomRepository roomRepository, ChatCommandService chatCommandService) {
    //     this.roomRepository = roomRepository;
    //     this.chatCommandService = chatCommandService;
    // }

    // @Bean
    // public CommandLineRunner initializeRoomData() {
    //     return args -> {
    //         if (roomRepository.count() == 0) {
    //             List<Long> targetCustomerIds = Arrays.asList(1L, 2L);
                
    //             Room newRoom = chatCommandService.processNewRoom(
    //                 "Direct Chat 1-2",
    //                 1L,
    //                 targetCustomerIds
    //             );

    //             System.out.println("---Đã khởi tạo thành công phòng DIRECT (ID: " + newRoom.getRoomId() + ") thông qua ChatCommandService để kích hoạt Kafka Event. ---");
    //         }
    //     };
    // }
}