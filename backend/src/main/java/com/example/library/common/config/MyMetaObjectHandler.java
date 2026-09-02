package com.example.library.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 *
 * <p>在插入和更新时自动填充 createTime 和 updateTime 字段，
 * 无需在业务代码中手动设置。
 *
 * <p>配合实体类中的注解使用：
 * <ul>
 *   <li>@TableField(fill = FieldFill.INSERT)：插入时填充</li>
 *   <li>@TableField(fill = FieldFill.INSERT_UPDATE)：插入和更新时填充</li>
 * </ul>
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 插入时自动填充创建时间和更新时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时自动填充更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
