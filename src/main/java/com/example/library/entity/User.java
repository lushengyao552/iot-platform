package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * <p>对应数据库表 sys_user
 * <p>MyBatis-Plus 注解说明：
 * <ul>
 *   <li>@TableName：指定数据库表名</li>
 *   <li>@TableId：主键，type=AUTO 表示自增</li>
 *   <li>@TableField：字段映射，exist=false 表示非数据库字段</li>
 *   <li>@TableLogic：逻辑删除字段</li>
 *   <li>@Version：乐观锁字段</li>
 * </ul>
 */
@Data
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码（BCrypt加密存储） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 角色：ADMIN-管理员，USER-普通用户 */
    private String role;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;
}
