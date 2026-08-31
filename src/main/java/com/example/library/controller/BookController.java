package com.example.library.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 图书控制器
 *
 * <p>RESTful 风格接口：
 * <ul>
 *   <li>GET /books：分页查询</li>
 *   <li>GET /books/{id}：查询详情</li>
 *   <li>POST /books：新增（管理员）</li>
 *   <li>PUT /books/{id}：更新（管理员）</li>
 *   <li>DELETE /books/{id}：删除（管理员）</li>
 * </ul>
 */
@Tag(name = "图书管理", description = "图书的增删改查、分页搜索等接口")
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * 分页查询图书
     */
    @Operation(summary = "分页查询图书", description = "支持按书名、作者、ISBN、分类等条件搜索，支持分页和排序")
    @GetMapping
    public Result<IPage<BookVO>> pageBooks(BookQueryDTO queryDTO) {
        IPage<BookVO> page = bookService.pageBooks(queryDTO);
        return Result.success(page);
    }

    /**
     * 查询图书详情
     */
    @Operation(summary = "查询图书详情", description = "根据图书ID查询详细信息")
    @GetMapping("/{id}")
    public Result<BookVO> getBookById(
            @Parameter(description = "图书ID", required = true)
            @PathVariable Long id) {
        BookVO bookVO = bookService.getBookById(id);
        return Result.success(bookVO);
    }

    /**
     * 新增图书（仅管理员）
     */
    @Operation(summary = "新增图书", description = "管理员新增图书信息")
    @PostMapping
    public Result<BookVO> addBook(@Valid @RequestBody BookAddDTO addDTO) {
        checkAdmin();
        BookVO bookVO = bookService.addBook(addDTO);
        return Result.success(bookVO);
    }

    /**
     * 更新图书（仅管理员）
     */
    @Operation(summary = "更新图书", description = "管理员更新图书信息")
    @PutMapping("/{id}")
    public Result<BookVO> updateBook(
            @Parameter(description = "图书ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody BookUpdateDTO updateDTO) {
        checkAdmin();
        BookVO bookVO = bookService.updateBook(id, updateDTO);
        return Result.success(bookVO);
    }

    /**
     * 删除图书（仅管理员，逻辑删除）
     */
    @Operation(summary = "删除图书", description = "管理员删除图书（逻辑删除）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteBook(
            @Parameter(description = "图书ID", required = true)
            @PathVariable Long id) {
        checkAdmin();
        bookService.deleteBook(id);
        return Result.success();
    }

    /**
     * 校验当前用户是否为管理员
     */
    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
