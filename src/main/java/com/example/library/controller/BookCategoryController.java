package com.example.library.controller;

import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.Result;
import com.example.library.common.result.ResultCode;
import com.example.library.service.BookCategoryService;
import com.example.library.util.UserContext;
import com.example.library.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "分类接口")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class BookCategoryController {

    private final BookCategoryService categoryService;

    @Operation(summary = "查询所有分类")
    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.success(categoryService.list());
    }

    @Operation(summary = "新增分类（管理员）")
    @PostMapping
    public Result<Void> add(@RequestBody Map<String, Object> body) {
        checkAdmin();
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Integer sort = body.get("sort") != null ? (Integer) body.get("sort") : null;
        categoryService.add(name, description, sort);
        return Result.success();
    }

    @Operation(summary = "修改分类（管理员）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        checkAdmin();
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Integer sort = body.get("sort") != null ? (Integer) body.get("sort") : null;
        categoryService.update(id, name, description, sort);
        return Result.success();
    }

    @Operation(summary = "删除分类（管理员）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkAdmin();
        categoryService.delete(id);
        return Result.success();
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
