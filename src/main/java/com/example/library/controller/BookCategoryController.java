package com.example.library.controller;

import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.Result;
import com.example.library.common.result.ResultCode;
import com.example.library.entity.BookCategory;
import com.example.library.service.BookCategoryService;
import com.example.library.util.UserContext;
import com.example.library.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图书分类控制器
 */
@Tag(name = "分类管理", description = "图书分类的增删改查接口")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class BookCategoryController {

    private final BookCategoryService categoryService;

    /**
     * 查询所有分类
     */
    @Operation(summary = "查询所有分类", description = "获取所有图书分类列表，按排序号排序")
    @GetMapping
    public Result<List<CategoryVO>> listAllCategories() {
        List<CategoryVO> categories = categoryService.listAllCategories();
        return Result.success(categories);
    }

    /**
     * 查询分类详情
     */
    @Operation(summary = "查询分类详情", description = "根据分类ID查询详细信息")
    @GetMapping("/{id}")
    public Result<CategoryVO> getCategoryById(
            @Parameter(description = "分类ID", required = true)
            @PathVariable Long id) {
        BookCategory category = categoryService.getById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        return Result.success(categoryService.toVO(category));
    }

    /**
     * 新增分类（仅管理员）
     */
    @Operation(summary = "新增分类", description = "管理员新增图书分类")
    @PostMapping
    public Result<CategoryVO> addCategory(@RequestBody BookCategory category) {
        checkAdmin();
        CategoryVO vo = categoryService.addCategory(category);
        return Result.success(vo);
    }

    /**
     * 更新分类（仅管理员）
     */
    @Operation(summary = "更新分类", description = "管理员更新图书分类")
    @PutMapping("/{id}")
    public Result<CategoryVO> updateCategory(
            @Parameter(description = "分类ID", required = true)
            @PathVariable Long id,
            @RequestBody BookCategory category) {
        checkAdmin();
        category.setId(id);
        CategoryVO vo = categoryService.updateCategory(category);
        return Result.success(vo);
    }

    /**
     * 删除分类（仅管理员）
     */
    @Operation(summary = "删除分类", description = "管理员删除图书分类（分类下无图书时才能删除）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(
            @Parameter(description = "分类ID", required = true)
            @PathVariable Long id) {
        checkAdmin();
        categoryService.deleteCategory(id);
        return Result.success();
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
