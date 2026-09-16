package com.example.library.controller;

import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.Result;
import com.example.library.common.result.ResultCode;
import com.example.library.service.BorrowService;
import com.example.library.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "借阅接口")
@RestController
@RequestMapping("/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @Operation(summary = "借书")
    @PostMapping("/{bookId}")
    public Result<Void> borrow(@PathVariable Long bookId) {
        borrowService.borrow(bookId);
        return Result.success();
    }

    @Operation(summary = "还书")
    @PutMapping("/{id}/return")
    public Result<Void> returnBook(@PathVariable Long id) {
        borrowService.returnBook(id);
        return Result.success();
    }

    @Operation(summary = "我的借阅记录")
    @GetMapping("/my")
    public Result<Map<String, Object>> myRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(borrowService.myRecords(pageNum, pageSize));
    }

    @Operation(summary = "所有借阅记录（管理员）")
    @GetMapping
    public Result<Map<String, Object>> allRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return Result.success(borrowService.allRecords(pageNum, pageSize, status));
    }
}
