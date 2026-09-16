package com.iot.platform.telemetry.controller;

import com.iot.platform.common.result.Result;
import com.iot.platform.telemetry.service.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "遥测数据")
@RestController
@RequestMapping("/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    @Operation(summary = "设备历史遥测数据")
    @GetMapping("/history/{deviceName}")
    public Result<Map<String, Object>> history(
            @PathVariable String deviceName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(telemetryService.history(deviceName, pageNum, pageSize));
    }
}
