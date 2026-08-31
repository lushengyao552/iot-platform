package com.example.library.controller;

import com.example.library.common.result.Result;
import com.example.library.dto.LoginDTO;
import com.example.library.dto.RegisterDTO;
import com.example.library.service.UserService;
import com.example.library.vo.LoginVO;
import com.example.library.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * <p>处理用户登录、注册等认证相关接口
 * <p>这些接口在 JWT 拦截器白名单中，无需登录即可访问
 */
@Tag(name = "认证管理", description = "用户登录、注册等认证接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录
     *
     * @param loginDTO 登录请求（用户名 + 密码）
     * @return 登录响应（包含 JWT Token 和用户信息）
     */
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT Token")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    /**
     * 用户注册
     *
     * @param registerDTO 注册请求
     * @return 注册成功的用户信息
     */
    @Operation(summary = "用户注册", description = "新用户注册，默认角色为普通用户")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        UserVO userVO = userService.register(registerDTO);
        return Result.success(userVO);
    }

    /**
     * 健康检查接口
     */
    @Operation(summary = "健康检查", description = "验证服务是否正常运行")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("服务运行正常");
    }
}
