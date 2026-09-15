# Stage 02：Entity（实体类）

## 阶段目标

根据自己在 stage01 设计的表，写出对应的 Java 实体类。理解 MySQL 类型和 Java 类型怎么映射、MyBatis-Plus 注解怎么用、Lombok 怎么省代码、自动填充时间字段怎么搞。

## 前置要求

- stage01 毕业（表已建好）
- 知道 Lombok 是什么

## 本阶段知识点

- @TableName / @TableId / @TableField
- 主键自增 IdType.AUTO
- 逻辑删除 @TableLogic
- 自动填充 @TableField(fill = ...) + MetaObjectHandler
- 字段类型映射：BIGINT→Long, VARCHAR→String, DECIMAL→BigDecimal, DATETIME→LocalDateTime
- Lombok：@Data / @Getter / @Setter / @NoArgsConstructor / @AllArgsConstructor / @Builder
- 为什么不用 @Data 在所有类上都无脑加
- Entity 里要不要加 DTO/VO 的注解（比如校验注解）

---

## 题目

### 【第 2-1 题：User Entity】

**业务背景**
表建好了，现在要让 Java 能操作这张表。

**我的任务**
写出 `User` 实体类，字段和 user 表一一对应。

**限制条件**
- 用 @TableName 指定表名
- 主键用 @TableId(type = IdType.AUTO)
- 逻辑删除字段用 @TableLogic
- 创建时间、更新时间用 @TableField(fill = FieldFill.INSERT / INSERT_UPDATE)
- 密码字段在 Entity 里也不要打日志时被带出来（想清楚怎么做）
- 用 Lombok 简化样板代码

**验收标准**
- 项目启动后 MyBatis-Plus 能识别这张表
- 你能解释 @TableName 不写会怎样
- 你能解释为什么 password 字段要特殊处理
- 你能说出 LocalDateTime 和 Date 的区别，为什么选 LocalDateTime

**思考题**
1. 如果数据库字段是 user_name，Java 属性是 userName，MyBatis-Plus 默认能映射吗？靠什么规则？
2. @TableField(exist = false) 用在什么场景？
3. 为什么 Entity 不直接加 @NotNull 这些校验注解？
4. Lombok 的 @Data 会生成 equals/hashCode/toString，在 Entity 上有什么坑？
5. 字段名和属性名什么时候必须显式 @TableField？

---

### 【第 2-2 题：其他三张表的 Entity】

**业务背景**
趁热打铁，把 Category / Book / BorrowRecord 三个实体类写完。

**我的任务**
写出：
- Category
- Book（注意 total_stock / available_stock 用 Integer 还是 Long；price 用 BigDecimal）
- BorrowRecord（状态字段用 String 还是 Integer 还是枚举？自己决定并说明）

**限制条件**
- Book 的 price 必须 BigDecimal
- BorrowRecord 的 returnTime 允许 null
- 所有时间字段统一 LocalDateTime
- 不要在 Entity 里写业务逻辑

**验收标准**
- 四个 Entity 全部写完
- 你能说清为什么 Book.stock 用 Integer，而不是 int
- 你能说出 Entity 里"业务状态"用枚举 vs 字符串的权衡
- 你能说清 Entity 里能不能加 DTO 不该有的字段

**思考题**
1. Entity 为什么不能直接返回给前端？
2. 枚举类型在 MyBatis-Plus 里怎么存？@EnumValue 是干嘛的？
3. Book 里要不要加一个 transient 的字段比如 borrowedByCurrentUser？
4. 主键用 Long，前端 JS 精度会丢吗？怎么解决？（这是个著名坑）

---

### 【第 2-3 题：自动填充 MetaObjectHandler】

**业务背景**
每个实体都有 create_time / update_time，不想每次 insert/update 都手动 set。

**我的任务**
1. 写一个 `MyMetaObjectHandler implements MetaObjectHandler`
2. 在 insertFill 里自动填 createTime、updateTime
3. 在 updateFill 里自动填 updateTime
4. 加上 @Component 让 Spring 管理

**限制条件**
- 不能在业务代码里到处 setCreateTime
- 要理解这是 AOP/拦截器思想，不是魔法

**验收标准**
- 插入一条 user 记录，数据库里 create_time / update_time 自动有值
- 更新一条记录，update_time 自动变，create_time 不变
- 你能说出这个 handler 是被谁调用的、什么时候调用

**思考题**
1. 为什么 createTime 用 INSERT 填充，updateTime 用 INSERT_UPDATE 填充？
2. 逻辑删除 deleted 默认值 0 谁来填？
3. 如果我用自定义 SQL（XML 里写 insert），自动填充还生效吗？
4. 数据库的 DEFAULT CURRENT_TIMESTAMP 和 Java 自动填充，留哪个？

---

## 本阶段毕业考试

不看任何资料：
1. 给你一张新表（比如 reservation），5 分钟写出对应 Entity
2. 随机抽一个字段，让你说出 Java 类型为什么选这个
3. 让你现场写 MetaObjectHandler 并解释每一行

## 本阶段反向面试题

1. Entity / DTO / VO 三者区别？为什么不合并？
2. MyBatis-Plus 的 @TableField(fill) 原理是什么？
3. 为什么不推荐在 Entity 上加 Jackson 注解（比如 @JsonIgnore）？
4. Long 主键传给前端，JS 精度问题怎么修？
5. LocalDateTime 在 JSON 序列化时怎么格式化？

## 常见坑

- 字段名映射不上（数据库 user_name 写成 username 又没配 map-underscore-to-camel-case）
- 主键类型写成 Integer，未来数据量大了改不动
- BigDecimal 写成 Double
- Entity 上加了业务校验注解（导致 DTO 职责混乱）
- 忘了 @TableLogic，逻辑删除不生效
