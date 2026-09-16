package com.example.library.controller;

import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.Result;
import com.example.library.common.result.ResultCode;
import com.example.library.dto.BookAddDTO;
import com.example.library.dto.BookQueryDTO;
import com.example.library.dto.BookUpdateDTO;
import com.example.library.service.BookService;
import com.example.library.util.UserContext;
import com.example.library.vo.BookVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "图书接口")
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @Operation(summary = "分页查询图书")
    @GetMapping
    public Result<Map<String, Object>> page(BookQueryDTO query) {
        return Result.success(bookService.page(query));
    }

    @Operation(summary = "图书详情")
    @GetMapping("/{id}")
    public Result<BookVO> getById(@PathVariable Long id) {
        return Result.success(bookService.getById(id));
    }

    @Operation(summary = "新增图书（管理员）")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody BookAddDTO dto) {
        checkAdmin();
        bookService.add(dto);
        return Result.success();
    }

    @Operation(summary = "修改图书（管理员）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BookUpdateDTO dto) {
        checkAdmin();
        bookService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除图书（管理员）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkAdmin();
        bookService.delete(id);
        return Result.success();
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
