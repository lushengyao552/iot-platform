# Stage 08：参数校验

## 阶段目标

用 Jakarta Validation（@Valid / @NotNull / @NotBlank / @Size / @Email 等）在 Controller 入口统一校验，而不是在 Service 里手写 if。理解校验注解的原理、分组校验、自定义校验注解。

## 前置要求

- stage07 毕业（有全局异常处理）

## 本阶段知识点

- spring-boot-starter-validation
- @Valid / @Validated
- @NotNull / @NotBlank / @NotEmpty 区别
- @Size / @Min / @Max / @Email / @Pattern
- 嵌套校验 @Valid 在字段上
- 分组校验（新增组 / 更新组）
- 校验失败抛 MethodArgumentNotValidException / ConstraintViolationException
- 自定义注解（本阶段了解即可）

---

## 题目

### 【第 8-1 题：注册 DTO 加校验】

**业务背景**
注册接口现在接收 username/password，前端可能传空、传太短、传邮箱格式错。

**我的任务**
1. 写 RegisterDTO：
   - username：必填，长度 3-20
   - password：必填，长度 6-30，至少含字母和数字（用 @Pattern）
   - nickname：可选，最长 30
   - email：可选，格式正确
2. Controller 参数前加 @Valid
3. 全局异常处理器接住 MethodArgumentNotValidException，把第一个错误信息拼到 Result.message

**限制条件**
- 不允许在 Service 里手写 if (username == null)
- @NotBlank 不能用在 Integer 上
- 错误信息要中文，且能让前端直接展示

**验收标准**
- 空 username 返回 400 业务码 + 具体错误
- 密码 "123" 返回"密码长度必须在 6-30 之间"
- 邮箱格式错返回具体提示
- 你能说清 @NotNull / @NotBlank / @NotEmpty 的区别

**思考题**
1. @NotBlank 为什么只能用在 String？
2. @Valid 和 @Validated 区别？
3. 校验是在 Controller 进入前还是后？
4. GET 请求的查询参数怎么校验？

---

### 【第 8-2 题：新增 vs 更新分组校验】

**业务背景**
新增图书时 ISBN 必填，更新图书时 id 必填、ISBN 可不变。同一个 BookDTO 两种场景要求不同。

**我的任务**
1. 写分组：Create / Update（空接口标记）
2. BookDTO：
   - id：Update 组必填，Create 组不用
   - name：Create / Update 都必填
   - isbn：Create 必填，Update 不必填
3. Controller：
   - POST 用 @Validated(Create.class)
   - PUT 用 @Validated(Update.class)

**限制条件**
- 不允许写两个 DTO（CreateBookDTO / UpdateBookDTO），用分组优雅解决
- 嵌套对象里也要校验

**验收标准**
- 新增不传 id 能过
- 更新不传 id 报错
- 新增和更新共用同一个 DTO
- 你能说清分组校验的设计动机

**思考题**
1. 分组和写两个 DTO 各有什么优劣？
2. 默认组 Default 是什么意思？
3. 分组能继承吗？
4. 什么时候该用分组，什么时候该拆 DTO？

---

### 【第 8-3 题：对象内嵌套校验与集合校验】

**业务背景**
批量新增图书时，传一个 List<BookDTO>。每个元素都要校验。

**我的任务**
1. 写 POST /api/admin/books/batch
2. 入参是 List<BookDTO>，加 @Valid
3. 校验整个集合不为空，且每个元素都合法

**限制条件**
- 集合本身不能用 @NotEmpty，要在外面再包一层 DTO 或用 @Size
- 错误信息要告诉前端是第几个元素错了

**验收标准**
- 传空集合报错
- 传 10 本里第 3 本名字为空，错误信息能定位
- 你能说清为什么 List 直接加 @Valid 在 Spring MVC 里不生效（要在 Controller 参数上 @Valid + 元素字段上 @Valid）

**思考题**
1. 为什么 @Valid 加在 List<BookDTO> 上不够？
2. 自定义一个 @ValidList 注解怎么做？
3. 级联校验什么时候停？

---

### 【第 8-4 题：自定义校验注解（选做）】

**业务背景**
要校验"图书 ISBN 格式"（13 位数字），现成注解不够。

**我的任务**
写一个 @Isbn 注解：
- 用 @Pattern 也能做，但自己写一遍 ConstraintValidator
- 注解标注在 BookDTO.isbn 字段上

**限制条件**
- 理解即可，不追求复杂
- 错误信息要能通过 message 属性自定义

**验收标准**
- 13 位数字过
- 12 位报错
- 你能说清 ConstraintValidator 的两个泛型参数

**思考题**
1. 自定义注解要标哪些元注解？
2. 校验注解能拿到当前对象吗（跨字段校验）？
3. 跨字段校验（比如两次密码一致）怎么做？

---

## 本阶段毕业考试

不看任何资料：
1. 给你一个新 DTO（比如修改密码：oldPassword / newPassword / confirmPassword），加上所有校验，包括两次密码一致
2. 让你现场解释 @Valid 和 @Validated 区别
3. 给一段满是手写 if 校验的 Service 代码，让你改成注解校验

## 本阶段反向面试题

1. Hibernate Validator 和 Spring Validation 关系？
2. 校验是 AOP 实现的吗？
3. 分组校验的原理？
4. 为什么 @NotNull 不能用在基本类型上？
5. 校验失败抛的两个异常（MethodArgumentNotValidException / ConstraintViolationException）分别在什么场景抛？

## 常见坑

- @NotBlank 用在 Long 上
- GET 参数不加 @Validated，校验不生效
- 异常没接住，直接返回 500
- 分组忘了在 Controller 指定，全部走默认组
- 嵌套对象字段漏加 @Valid
