package com.iot.platform.telemetry.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(collection = "telemetry_data")
@CompoundIndex(def = "{'deviceName': 1, 'ts': -1}")
public class TelemetryData {
    @Id
    private String id;

    @Indexed
    private String deviceName;
    private String productKey;
    private LocalDateTime ts;
    private Map<String, Object> metrics;
}
