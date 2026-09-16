package com.iot.platform.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceRegisterDTO {
    @NotBlank(message = "产品Key不能为空")
    private String productKey;
    private String deviceName;
}
