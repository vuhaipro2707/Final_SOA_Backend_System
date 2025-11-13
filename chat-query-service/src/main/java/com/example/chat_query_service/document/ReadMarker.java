package com.example.chat_query_service.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "readMarkers")
// Compound Index DUY NHẤT và UNIQUE trên roomId và customerId
// Đảm bảo mỗi người dùng chỉ có MỘT con trỏ đọc cho mỗi phòng.
@CompoundIndexes({
    @CompoundIndex(name = "unique_read_marker_idx", def = "{'roomId': 1, 'customerId': 1}", unique = true)
})
public class ReadMarker {
    @Id
    private String id;

    private Long roomId;

    private Long customerId;
    
    private Long lastReadMessageId;

    private Instant lastReadAt;
}