package com.example.library.controller;

import com.example.library.common.result.Result;
import com.example.library.service.UserService;
import com.example.library.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.getProfile());
    }

    @Operation(summary = "修改当前用户信息")
    @PutMapping("/me")
    public Result<Void> updateMe(@RequestBody Map<String, String> body) {
        userService.updateProfile(
                body.get("nickname"),
                body.get("email"),
                body.get("phone"));
        return Result.success();
    }
}
