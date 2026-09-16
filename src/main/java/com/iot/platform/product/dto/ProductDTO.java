package com.iot.platform.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductDTO {
    @NotBlank(message = "产品名称不能为空")
    private String name;
    private String protocol;
    private String description;
}
