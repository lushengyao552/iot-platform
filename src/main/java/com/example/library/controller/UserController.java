package com.example.library.controller;

import com.example.library.common.result.Result;
import com.example.library.service.UserService;
import com.example.library.util.UserContext;
import com.example.library.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 *
 * <p>处理当前登录用户相关的接口
 */
@Tag(name = "用户管理", description = "当前用户信息查询等接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        Long userId = UserContext.getCurrentUserId();
        UserVO userVO = userService.toVO(userService.getById(userId));
        return Result.success(userVO);
    }
}
