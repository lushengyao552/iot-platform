package com.example.library.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息 VO（不包含密码等敏感字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息")
public class UserVO {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "昵称", example = "系统管理员")
    private String nickname;

    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800000001")
    private String phone;

    @Schema(description = "角色：ADMIN-管理员，USER-普通用户", example = "ADMIN")
    private String role;

    @Schema(description = "状态：0-禁用，1-正常", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
