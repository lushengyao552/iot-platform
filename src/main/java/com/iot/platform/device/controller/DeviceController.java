package com.iot.platform.device.controller;

import com.iot.platform.common.result.Result;
import com.iot.platform.device.dto.DeviceRegisterDTO;
import com.iot.platform.device.entity.Device;
import com.iot.platform.device.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "设备管理")
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "分页查询设备")
    @GetMapping
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String productKey,
            @RequestParam(required = false) String status) {
        return Result.success(deviceService.page(pageNum, pageSize, productKey, status));
    }

    @Operation(summary = "设备详情")
    @GetMapping("/{id}")
    public Result<Device> getById(@PathVariable Long id) {
        return Result.success(deviceService.getById(id));
    }

    @Operation(summary = "注册设备")
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody DeviceRegisterDTO dto) {
        return Result.success(deviceService.register(dto));
    }
}
