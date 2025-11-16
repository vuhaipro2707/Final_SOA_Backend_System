package com.example.chat_query_service.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "readMarkers")
@CompoundIndexes({
    @CompoundIndex(name = "unique_read_marker_idx", def = "{'roomId': 1, 'customerId': 1}", unique = true)
})
public class ReadMarker {
    @Id
    private String id;

    private Long roomId;

    private Long customerId;
    
    private Long lastReadMessageId;
}