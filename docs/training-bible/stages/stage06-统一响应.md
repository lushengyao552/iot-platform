# Stage 06：统一响应结果

## 阶段目标

所有接口返回统一格式 `{ code, message, data }`。理解为什么要统一、怎么用ResponseBodyAdvice 或直接在 Controller 返回。本阶段先做"直接包一层"的简单版本，再讲进阶方案。

## 前置要求

- stage05 毕业

## 本阶段知识点

- 统一响应体 Result<T>
- 错误码枚举 ResultCode
- 成功 / 失败两种构造
- HTTP 状态码 vs 业务 code 的区别
- 为什么不用 ResponseEntity 包一层
- 进阶：@RestControllerAdvice + ResponseBodyAdvice（本阶段只讲，不强求实现）

---

## 题目

### 【第 6-1 题：设计 Result 类】

**业务背景**
现在每个 Controller 直接返回 VO，前端要自己判断 HTTP 状态码。企业项目里通常返回统一格式。

**我的任务**
1. 在 common 包下写一个 `Result<T>`：
   - code（int）
   - message（String）
   - data（T）
   - 时间戳 timestamp（可选）
2. 写静态方法：
   - success(T data)
   - success()
   - failed(int code, String message)
   - failed(ResultCode code)
3. 写 ResultCode 枚举，至少包含：
   - SUCCESS(20000)
   - PARAM_ERROR(40000)
   - UNAUTHORIZED(40100)
   - FORBIDDEN(40300)
   - NOT_FOUND(40400)
   - INTERNAL_ERROR(50000)

**限制条件**
- code 用业务码，不要用 HTTP 状态码当 code
- 枚举要有 code 和 message 两个字段
- Result 要支持泛型

**验收标准**
- 你能说清 HTTP 200 + code=50000 和 HTTP 500 的区别
- 你能说清为什么要统一响应
- Result 能被 Jackson 正确序列化

**思考题**
1. 为什么业务 code 不用 HTTP 状态码？
2. Result 里要不要加 traceId？什么时候加？
3. 成功时 code 用 0 还是 200 还是 20000？公司里常见约定？
4. data 为 null 时要不要返回这个字段？

---

### 【第 6-2 题：改造所有 Controller】

**业务背景**
Result 设计好了，现在把所有接口的返回值包一层。

**我的任务**
把 stage05 写的所有 Controller 方法返回值改成 `Result<XxxVO>`。

**限制条件**
- Service 层不要返回 Result（Service 只返回业务数据或抛异常）
- 不要在每个方法里手写 `new Result(...)`，用静态方法
- 改造完跑一遍 Knife4j，确认返回格式统一

**验收标准**
- 所有接口返回都是 `{ code, message, data }`
- Service 层看不到 Result
- 你能说清为什么 Result 应该在 Controller 边界包

**思考题**
1. Service 返回 Result 有什么坏处？
2. 如果想"自动包一层"，不用每个方法写 Result.success，怎么做？（提示：ResponseBodyAdvice）
3. 文件下载接口要不要包 Result？（提示：不要）

---

### 【第 6-3 题：错误码规范】

**业务背景**
错误码不能想到什么写什么，要有规范。

**我的任务**
1. 把 ResultCode 扩展成分段规范：
   - 2xxxx 成功
   - 4xxxx 客户端错误（40xxx 参数、41xxx 认证、42xxx 权限、43xxx 业务规则）
   - 5xxxx 服务端错误
2. 至少为用户、图书、借阅三个模块各预留几个业务错误码
3. 写一份错误码说明文档（README 或 md）

**限制条件**
- 不允许直接 throw new RuntimeException("xxx")，必须用业务异常
- 错误码一旦分配不许复用

**验收标准**
- 你能说出"用户名已存在"该用哪个码段
- 你能说出为什么 404 用业务码 40400 而不是 HTTP 404
- 错误码表能让新同学一眼看懂

**思考题**
1. 错误码和 message 是什么关系？message 能不能前端直接展示？
2. 国际化怎么做？错误码固定，message 按语言取？
3. 一个异常可能有多个错误码吗？

---

## 本阶段毕业考试

不看任何资料：
1. 给一个新需求"批量导入图书"，设计成功和失败时 Result 的结构
2. 解释为什么 Service 不能返回 Result
3. 现场给你一个不统一的响应，让你改成统一格式

## 本阶段反向面试题

1. 为什么要统一响应？直接抛 HTTP 状态码不行吗？
2. ResponseBodyAdvice 是什么？怎么用它自动包装？
3. 文件下载、SSE、流式接口为什么不能套统一响应？
4. code 和 message 应该由谁返回，前端怎么用？
5. 分页接口的 data 里放 total / records 怎么设计？

## 常见坑

- Service 里也返回 Result（污染分层）
- code 直接用 HTTP 状态码
- 错误码重复
- 分页对象直接当 data，不包装 total
