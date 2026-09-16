package com.iot.platform.telemetry.service;

import com.iot.platform.device.service.DeviceService;
import com.iot.platform.telemetry.entity.TelemetryData;
import com.iot.platform.telemetry.repository.TelemetryDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelemetryService {

    private final TelemetryDataRepository repository;
    private final DeviceService deviceService;

    @RabbitListener(queues = "iot.telemetry.queue")
    public void onTelemetry(Map<String, Object> message) {
        String deviceName = (String) message.get("deviceName");
        log.info("收到设备遥测: {}", deviceName);

        TelemetryData data = new TelemetryData();
        data.setDeviceName(deviceName);
        data.setProductKey((String) message.get("productKey"));
        data.setTs(LocalDateTime.now());
        data.setMetrics((Map<String, Object>) message.get("metrics"));
        repository.save(data);

        deviceService.heartbeat(deviceName);
    }

    public Map<String, Object> history(String deviceName, int pageNum, int pageSize) {
        Page<TelemetryData> page = repository.findByDeviceNameOrderByTsDesc(
                deviceName, PageRequest.of(pageNum - 1, pageSize));
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }
}
