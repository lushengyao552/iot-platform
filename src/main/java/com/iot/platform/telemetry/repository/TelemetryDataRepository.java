package com.iot.platform.telemetry.repository;

import com.iot.platform.telemetry.entity.TelemetryData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TelemetryDataRepository extends MongoRepository<TelemetryData, String> {
    Page<TelemetryData> findByDeviceNameOrderByTsDesc(String deviceName, Pageable pageable);
}
