package com.example.library.entity;
// ## 题 2.7 KnowledgeDocument 实体（AI）

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;
import java.time.LocalDateTime;
import java.io.Serializable;
// **文件**：`entity/KnowledgeDocument.java`，表 `knowledge_document`

// **注意：没有 deleted**

// | 字段 | 类型 |
// |------|------|
// | id | Long (@TableId AUTO) |
// | title | String |
// | content | String |
// | docType | String |
// | source | String |
// | createTime | LocalDateTime (INSERT) |
// | updateTime | LocalDateTime (INSERT_UPDATE) |
// // 
@Data
@TableName("knowledge_document")
public class KnowledgeDocument implements Serializable {
    @TableId (type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String docType;
    private String source;

    @TableField (fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField (fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
