package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;


// | 字段 | 类型 | 注解 |
// |------|------|------|
// | id | Long | `@TableId(type = IdType.AUTO)` |
// | username | String | |
// | password | String | |
// | nickname | String | |
// | email | String | |
// | phone | String | |
// | role | String | |
// | status | Integer | |
// | createTime | LocalDateTime | `@TableField(fill = FieldFill.INSERT)` |
// | updateTime | LocalDateTime | `@TableField(fill = FieldFill.INSERT_UPDATE)` |
// | deleted | Integer | `@TableLogic` |

// **类注解**：`@Data` `@TableName("sys_user")` `implements Serializable`
@Data 
@TableName ("sys_user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String role;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic 
    private Integer deleted;

}
