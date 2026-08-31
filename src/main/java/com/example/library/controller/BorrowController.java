package com.example.library.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.Result;
import com.example.library.common.result.ResultCode;
import com.example.library.service.BorrowService;
import com.example.library.util.UserContext;
import com.example.library.vo.BorrowRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 借阅控制器
 *
 * <p>处理图书借阅、归还、借阅记录查询等接口
 */
@Tag(name = "借阅管理", description = "图书借阅、归还、借阅记录查询等接口")
@RestController
@RequestMapping("/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    /**
     * 借阅图书
     */
    @Operation(summary = "借阅图书", description = "当前登录用户借阅指定图书")
    @PostMapping("/{bookId}")
    public Result<BorrowRecordVO> borrowBook(
            @Parameter(description = "图书ID", required = true)
            @PathVariable Long bookId) {
        Long userId = UserContext.getCurrentUserId();
        BorrowRecordVO recordVO = borrowService.borrowBook(userId, bookId);
        return Result.success(recordVO);
    }

    /**
     * 归还图书
     */
    @Operation(summary = "归还图书", description = "根据借阅记录ID归还图书")
    @PutMapping("/{recordId}/return")
    public Result<BorrowRecordVO> returnBook(
            @Parameter(description = "借阅记录ID", required = true)
            @PathVariable Long recordId) {
        BorrowRecordVO recordVO = borrowService.returnBook(recordId);
        return Result.success(recordVO);
    }

    /**
     * 查询当前用户的借阅记录
     */
    @Operation(summary = "查询我的借阅记录", description = "查询当前登录用户的借阅记录，支持按状态筛选")
    @GetMapping("/me")
    public Result<IPage<BorrowRecordVO>> getMyBorrowRecords(
            @Parameter(description = "借阅状态：BORROWED-借阅中，RETURNED-已归还，OVERDUE-已逾期")
            @RequestParam(required = false) String status,
            @Parameter(description = "页码")
            @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContext.getCurrentUserId();
        IPage<BorrowRecordVO> page = borrowService.pageUserBorrowRecords(userId, status, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 查询所有借阅记录（仅管理员）
     */
    @Operation(summary = "查询所有借阅记录", description = "管理员查询所有用户的借阅记录")
    @GetMapping
    public Result<IPage<BorrowRecordVO>> getAllBorrowRecords(
            @Parameter(description = "借阅状态")
            @RequestParam(required = false) String status,
            @Parameter(description = "页码")
            @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        IPage<BorrowRecordVO> page = borrowService.pageAllBorrowRecords(status, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 查询借阅记录详情
     */
    @Operation(summary = "查询借阅记录详情", description = "根据记录ID查询借阅详情")
    @GetMapping("/{id}")
    public Result<BorrowRecordVO> getBorrowRecordById(
            @Parameter(description = "借阅记录ID", required = true)
            @PathVariable Long id) {
        BorrowRecordVO recordVO = borrowService.toVO(borrowService.getById(id));
        if (recordVO == null) {
            throw new BusinessException(ResultCode.BORROW_RECORD_NOT_FOUND);
        }
        // 普通用户只能查看自己的记录
        if (!UserContext.isAdmin() && !recordVO.getUserId().equals(UserContext.getCurrentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return Result.success(recordVO);
    }
}
