package com.example.library.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Token 前缀", example = "Bearer ")
    private String tokenPrefix;

    @Schema(description = "过期时间（毫秒）", example = "86400000")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private UserVO user;
}
