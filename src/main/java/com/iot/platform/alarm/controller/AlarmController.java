package com.iot.platform.alarm.controller;

import com.iot.platform.alarm.entity.AlarmRule;
import com.iot.platform.alarm.service.AlarmService;
import com.iot.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "告警规则")
@RestController
@RequestMapping("/alarm/rules")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @Operation(summary = "查询告警规则")
    @GetMapping
    public Result<List<AlarmRule>> list(@RequestParam(required = false) String productKey) {
        return Result.success(alarmService.list(productKey));
    }

    @Operation(summary = "创建告警规则")
    @PostMapping
    public Result<Void> create(@RequestBody AlarmRule rule) {
        alarmService.create(rule);
        return Result.success();
    }
}
