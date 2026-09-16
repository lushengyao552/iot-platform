package com.iot.platform.product.controller;

import com.iot.platform.common.result.Result;
import com.iot.platform.product.dto.ProductDTO;
import com.iot.platform.product.entity.Product;
import com.iot.platform.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "产品管理")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "分页查询产品")
    @GetMapping
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name) {
        return Result.success(productService.page(pageNum, pageSize, name));
    }

    @Operation(summary = "产品详情")
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    @Operation(summary = "新增产品")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody ProductDTO dto) {
        productService.add(dto);
        return Result.success();
    }

    @Operation(summary = "修改产品")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductDTO dto) {
        productService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除产品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }
}
