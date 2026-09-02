# 第20课：Knife4j 接口文档 + 阶段三总结

> **本课目标**：理解 Knife4j（Swagger）接口文档的作用和使用，学会给接口加文档注解，最后总结阶段三的学习成果，规划后续学习路线。

---

## 一、什么是 Knife4j？为什么需要接口文档？

### 问题场景

前后端分离项目中，前端开发需要知道后端有哪些接口、每个接口的 URL、请求方法、参数、返回值是什么。

如果没有接口文档：
- 前端每次都要问后端："这个接口参数是什么？返回什么？"
- 后端改了接口，前端不知道，联调时才发现报错
- 新人入职，不知道有哪些接口，只能看代码
- 接口测试要用 Postman 手动输入，容易出错

### Knife4j 的解决方案

Knife4j 是一个增强版的 Swagger 接口文档工具，它能：
1. **自动生成接口文档**：根据代码里的注解，自动生成 HTML 格式的接口文档
2. **在线调试**：在文档页面直接发请求测试接口（类似 Postman，但不需要手动输入参数）
3. **接口分组**：按模块分组展示接口
4. **导出文档**：支持导出 Markdown、Word、PDF 等格式
5. **离线文档**：可以生成静态文档，不需要启动服务也能查看

项目里用的是 Knife4j 4.4.0（Spring Boot 3.x 兼容版本）。

### Swagger 和 Knife4j 的关系

- **Swagger**：是一套接口文档规范和工具，包括 Swagger Annotations（注解）、Swagger UI（文档页面）
- **Knife4j**：是基于 Swagger 的增强版，提供更好看的 UI、更多功能（在线调试、导出、离线文档等），是国内常用的接口文档工具

简单理解：Knife4j = Swagger 注解 + 更好看的 UI + 更多功能。

---

## 二、项目里的 Knife4j 配置

### 2.1 pom.xml 依赖

```xml
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    <version>4.4.0</version>
</dependency>
```

注意：Spring Boot 3.x 用 `knife4j-openapi3-jakarta-spring-boot-starter`（jakarta 命名空间），Spring Boot 2.x 用 `knife4j-openapi3-spring-boot-starter`（javax 命名空间）。

### 2.2 application.yml 配置

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
  api-docs:
    path: /v3/api-docs
    enabled: true
  group-configs:
    - group: 'default'
      paths-to-match: '/**'
      packages-to-scan: com.example.library.controller

knife4j:
  enable: true
  setting:
    language: zh_cn
    enable-version: true
    enable-search: true
```

### 2.3 访问接口文档

启动项目后，浏览器访问：
- **Knife4j 文档**：http://localhost:8080/api/doc.html
- **Swagger UI**：http://localhost:8080/api/swagger-ui.html
- **OpenAPI JSON**：http://localhost:8080/api/v3/api-docs

注意：项目的 context-path 是 `/api`，所以文档地址是 `/api/doc.html`。

---

## 三、常用注解详解

Knife4j 基于 Swagger 注解，常用注解分两类：Controller 层注解和 Model 层注解。

### 3.1 Controller 层注解

| 注解 | 作用 | 示例 |
|------|------|------|
| `@Tag` | 给 Controller 分组/加描述 | `@Tag(name = "图书管理", description = "图书的增删改查")` |
| `@Operation` | 给接口方法加描述 | `@Operation(summary = "新增图书", description = "管理员新增一本图书")` |
| `@Parameter` | 给方法参数加描述 | `@Parameter(description = "图书ID", required = true) @PathVariable Long id` |
| `@Parameters` | 多个参数的容器 | （一般不用，直接用多个 @Parameter） |
| `@ApiResponse` | 描述响应 | `@ApiResponse(responseCode = "200", description = "成功")` |
| `@ApiResponses` | 多个响应的容器 | |

### 3.2 Model 层注解（DTO/VO/Entity）

| 注解 | 作用 | 示例 |
|------|------|------|
| `@Schema` | 给 Model 类加描述 | `@Schema(description = "图书新增请求")` |
| `@Schema`（字段上） | 给字段加描述、示例值、是否必填 | `@Schema(description = "书名", example = "Java核心技术", requiredMode = REQUIRED)` |

### 3.3 项目里的实际用法

看 `BookController`：

```java
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Tag(name = "图书管理", description = "图书的增删改查、分页查询、借阅")
public class BookController {

    private final BookService bookService;

    @Operation(summary = "分页查询图书", description = "支持按书名/作者模糊查询、按分类筛选，分页返回")
    @GetMapping
    public Result<IPage<BookVO>> pageBooks(
            @Parameter(description = "页码，从1开始", example = "1")
            @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "搜索关键词（书名/作者）", example = "Java")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID", example = "1")
            @RequestParam(required = false) Long categoryId) {
        IPage<BookVO> page = bookService.pageBooks(pageNum, pageSize, keyword, categoryId);
        return Result.success(page);
    }

    @Operation(summary = "查询图书详情", description = "根据ID查询图书详情，包含分类名称、是否可借等信息")
    @GetMapping("/{id}")
    public Result<BookVO> getBookById(
            @Parameter(description = "图书ID", required = true, example = "1")
            @PathVariable Long id) {
        BookVO bookVO = bookService.getBookById(id);
        return Result.success(bookVO);
    }

    @Operation(summary = "新增图书", description = "管理员新增一本图书，需要ADMIN角色")
    @PostMapping
    public Result<BookVO> addBook(@RequestBody @Valid BookAddDTO addDTO) {
        BookVO bookVO = bookService.addBook(addDTO);
        return Result.success(bookVO);
    }
}
```

看 `BookAddDTO`：

```java
@Data
@Schema(description = "图书新增请求")
public class BookAddDTO {

    @Schema(description = "书名", example = "Java核心技术", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "书名不能为空")
    @Size(max = 100, message = "书名长度不能超过100")
    private String title;

    @Schema(description = "作者", example = "Cay S. Horstmann", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "作者不能为空")
    @Size(max = 50, message = "作者长度不能超过50")
    private String author;

    @Schema(description = "ISBN", example = "9787111547426")
    @Size(max = 20, message = "ISBN长度不能超过20")
    private String isbn;

    @Schema(description = "分类ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @Schema(description = "价格（元）", example = "119.00")
    @DecimalMin(value = "0.00", message = "价格不能为负数")
    private BigDecimal price;

    @Schema(description = "库存数量", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Schema(description = "图书简介", example = "本书是Java领域的经典畅销书")
    @Size(max = 500, message = "简介长度不能超过500")
    private String description;
}
```

### 3.4 注解和校验注解的配合

注意项目里的 DTO 同时有两类注解：
- **文档注解**（`@Schema`）：给 Knife4j 文档用，描述字段、示例值、是否必填
- **校验注解**（`@NotBlank`、`@Size`、`@Min`）：给 Spring Validation 用，运行时校验参数

两类注解各司其职，互不干扰。`@Schema(requiredMode = REQUIRED)` 只是文档上标记必填，实际校验靠 `@NotBlank`/`@NotNull`。

---

## 四、接口文档的使用

### 4.1 文档页面结构

打开 http://localhost:8080/api/doc.html，你会看到：

1. **左侧导航**：按 Controller 分组（图书管理、用户认证、借阅管理等），每个分组下是接口列表
2. **接口详情**：点击某个接口，显示：
   - 接口描述（summary + description）
   - 请求 URL 和方法
   - 请求参数（名称、类型、是否必填、描述、示例值）
   - 请求体（JSON Schema + 示例）
   - 响应体（JSON Schema + 示例）
3. **在线调试**：点击"发送请求"按钮，可以直接在页面里发请求测试
   - 自动填充参数（根据示例值）
   - 可以修改参数
   - 显示响应结果（状态码、响应体、响应头）
   - 可以设置请求头（如 Authorization Token）

### 4.2 在线调试带 Token 的接口

需要登录的接口，在调试时需要加 Token：
1. 先调用登录接口，拿到 Token
2. 在文档页面右上角"文档管理" → "全局参数设置" → 添加请求头 `Authorization: Bearer <token>`
3. 或者在每个接口的调试页面手动加请求头
4. 然后发送请求，就会自动带 Token 了

### 4.3 导出文档

Knife4j 支持导出多种格式：
- **Markdown**：适合放到 Git 仓库里
- **Word**：适合发给非技术人员
- **PDF**：适合打印
- **OpenAPI JSON**：标准格式，可以导入其他工具

在文档页面右上角"文档管理" → "离线文档" / "导出 Markdown"。

---

## 五、写好接口文档的最佳实践

### 5.1 必须加注解的地方

1. **每个 Controller 类**：加 `@Tag(name, description)`，说明这个 Controller 是做什么的
2. **每个接口方法**：加 `@Operation(summary, description)`，summary 是简短标题，description 是详细说明
3. **每个方法参数**：加 `@Parameter(description, example, required)`，特别是路径参数和查询参数
4. **每个 DTO/VO 类**：加 `@Schema(description)`
5. **每个 DTO/VO 字段**：加 `@Schema(description, example, requiredMode)`，特别是必填字段和有特殊格式的字段

### 5.2 示例值很重要

`example` 是接口文档里最有价值的信息之一。好的示例值：
- 真实可信：`"Java核心技术"` 比 `"string"` 好
- 符合格式：ISBN 用真实的 ISBN 格式，日期用 `2026-09-02`
- 有业务含义：价格用 `119.00`，库存用 `10`

不好的示例值（Swagger 默认生成的）：`"string"`、`0`、`"2026-01-01T00:00:00Z"`，这些对前端没有帮助。

### 5.3 文档和代码同步

接口文档最大的问题是**文档和代码不同步**。Knife4j 的优势是文档由代码注解生成，代码改了文档自动更新（只要注解也改了）。

注意：
- 改了接口参数，一定要同步改 `@Parameter` 和 `@Schema` 注解
- 改了返回值，一定要同步改 VO 的 `@Schema` 注解
- 新增接口，一定要加 `@Operation` 注解
- 不要让文档里出现没有描述的接口（"新增接口"这种默认描述等于没写）

### 5.4 生产环境关闭文档

接口文档包含接口细节，生产环境不应该对外暴露：

```yaml
# application-prod.yml（生产环境配置）
springdoc:
  swagger-ui:
    enabled: false  # 关闭 Swagger UI
  api-docs:
    enabled: false  # 关闭 API 文档
knife4j:
  enable: false     # 关闭 Knife4j
```

或者通过 Nginx 限制只有内网能访问 `/doc.html`。

---

## 六、阶段三总结

到这里，阶段三（第17-20课）全部完成。回顾一下你学了什么：

### 阶段三：能扩展功能

| 课程 | 内容 | 你应该能做到 |
|------|------|-------------|
| 第17课 | 新增功能全链路实战 | 独立为项目新增一个功能（从数据库到后端到前端） |
| 第18课 | Spring AOP | 给项目加日志切面、权限切面，理解 AOP 原理和动态代理 |
| 第19课 | Docker 部署 | 写 Dockerfile、docker-compose，把项目容器化部署 |
| 第20课 | Knife4j 接口文档 | 给接口加文档注解，使用在线调试，导出文档 |

### 你现在的能力

完成阶段一、二、三后，你应该能：

1. **完全看懂项目**：理解每一行代码的作用、每个注解的原理、每个技术的使用场景
2. **修改项目**：能修改现有功能（改参数、加字段、调逻辑）
3. **扩展功能**：能独立新增一个完整功能（从数据库设计到后端代码到前端页面）
4. **部署项目**：能用 Docker 容器化部署整个项目
5. **写文档**：能给接口写规范的文档注解

### 五阶段学习路线总览

| 阶段 | 目标 | 课程 | 状态 |
|------|------|------|------|
| **阶段一：看懂项目** | 完全看懂项目所有代码 | 第1-10课 | ✅ 已完成 |
| **阶段二：核心技术深入** | 深入理解 Redis、MQ、前端、校验、分页 | 第11-16课 | ✅ 已完成 |
| **阶段三：能扩展功能** | 独立新增功能、AOP、Docker、文档 | 第17-20课 | ✅ 已完成 |
| **阶段四：能独立开发 + 面试** | Code Review、SQL优化、Redis进阶、MQ进阶、面试高频 | 第21-25课 | ⏳ 待生成 |
| **阶段五：AI 应用开发** | Spring AI、RAG、Tool Calling、Agent | 第26课+ | ⏳ 待生成 |

### 后续学习建议

**阶段四（能独立开发 + 面试）**重点：
- 对项目做 Code Review，找出设计问题、安全问题、性能问题
- SQL 优化（索引、慢查询、EXPLAIN）
- Redis 进阶（缓存击穿解决方案、分布式锁 Redisson、缓存一致性方案）
- RabbitMQ 进阶（消息幂等性、延迟插件、死信队列实战）
- 面试高频题（Spring Boot、MySQL、Redis、MQ、项目介绍）

**阶段五（AI 应用开发）**重点：
- Spring AI 框架入门
- LLM API 调用（OpenAI / 豆包 / 通义千问）
- RAG（检索增强生成）：把项目数据接入大模型
- Tool Calling：让大模型调用项目的 API
- MCP（Model Context Protocol）
- Agent：智能体开发

**你的最终目标**：Java 后端 + AI 应用开发工程师。先把 Java 后端基础打牢（阶段一到四），再学 AI 应用（阶段五），不要跳过基础直接学 Agent。

---

## 七、本课必须记住的 5 件事

1. **Knife4j 作用**：自动生成接口文档 + 在线调试，基于 Swagger 注解，代码改了文档自动更新
2. **常用注解**：Controller 层用 @Tag（类描述）、@Operation（方法描述）、@Parameter（参数描述）；Model 层用 @Schema（类和字段描述，一定要加 example 示例值）
3. **文档注解 vs 校验注解**：@Schema 是给文档看的，@NotBlank/@Size 是给运行时校验看的，两类注解配合使用
4. **最佳实践**：每个接口都加描述、示例值要真实可信、改代码同步改注解、生产环境关闭文档
5. **阶段三完成**：你现在能看懂、修改、扩展、部署项目了。接下来是阶段四（独立开发+面试）和阶段五（AI应用）

---

## 八、本节练习

### 练习1：给所有 Controller 加文档注解

检查项目里所有 Controller，确保：
1. 每个 Controller 类都有 @Tag 注解（name + description）
2. 每个接口方法都有 @Operation 注解（summary + description）
3. 每个方法参数都有 @Parameter 注解（description + example）
4. 每个 DTO/VO 类和字段都有 @Schema 注解

启动项目，访问 http://localhost:8080/api/doc.html，检查文档是否完整、美观。

### 练习2：用 Knife4j 在线调试所有接口

1. 启动项目，打开 Knife4j 文档
2. 调用登录接口，拿到 Token
3. 在全局参数里设置 Authorization 请求头
4. 逐个调试图书管理、借阅管理、用户管理的所有接口
5. 验证每个接口的参数、返回值是否和文档一致

### 练习3：导出接口文档

1. 在 Knife4j 文档页面导出 Markdown 格式的接口文档
2. 把导出的文档放到项目 docs/ 目录下
3. 检查导出的文档是否完整、格式是否正确

---

## 九、自测题

### Q1：Knife4j 和 Swagger 是什么关系？为什么需要接口文档？

<details>
<summary>点击查看答案</summary>

**关系**：
- **Swagger**：是一套接口文档规范和工具，包括 Swagger Annotations（注解规范）、Swagger UI（文档页面）。是业界标准。
- **Knife4j**：是基于 Swagger 的增强版工具，提供更好看的中文 UI、更多功能（在线调试、导出 Markdown/Word/PDF、离线文档、接口分组、全局参数等）。是国内常用的接口文档工具。
- 简单理解：Knife4j = Swagger 注解 + 增强 UI + 更多功能。Knife4j 兼容 Swagger 注解。

**为什么需要接口文档**：
1. **前后端协作**：前端需要知道后端有哪些接口、URL、请求方法、参数、返回值，没有文档只能靠问，效率低
2. **接口测试**：可以在文档页面在线调试，不需要 Postman 手动输入参数
3. **新人上手**：新人通过接口文档快速了解系统有哪些功能
4. **文档同步**：由代码注解自动生成，代码改了文档自动更新（只要注解也改了），避免文档和代码不一致
5. **对外交付**：可以导出文档发给第三方或前端团队

项目里用 Knife4j 4.4.0，Spring Boot 3.x 用 jakarta 版本的 starter。访问地址 http://localhost:8080/api/doc.html。

</details>

### Q2：常用的 Knife4j/Swagger 注解有哪些？分别用在什么地方？

<details>
<summary>点击查看答案</summary>

**Controller 层注解**：
1. **@Tag**：用在 Controller 类上，给接口分组加名称和描述
   - `@Tag(name = "图书管理", description = "图书的增删改查")`
2. **@Operation**：用在接口方法上，给接口加标题和详细描述
   - `@Operation(summary = "新增图书", description = "管理员新增一本图书")`
3. **@Parameter**：用在方法参数上，给参数加描述、示例值、是否必填
   - `@Parameter(description = "图书ID", example = "1", required = true) @PathVariable Long id`
4. **@ApiResponse / @ApiResponses**：描述响应状态码和含义（一般用默认的就行）

**Model 层注解（DTO/VO/Entity）**：
1. **@Schema（类上）**：给 Model 类加描述
   - `@Schema(description = "图书新增请求")`
2. **@Schema（字段上）**：给字段加描述、示例值、是否必填
   - `@Schema(description = "书名", example = "Java核心技术", requiredMode = REQUIRED)`

**注意**：
- 文档注解（@Schema、@Parameter）和校验注解（@NotBlank、@Size）是两类不同的注解，各司其职：文档注解给 Knife4j 看，校验注解给 Spring Validation 看
- example 示例值很重要，要真实可信，不要用默认的 "string"、"0"
- requiredMode = REQUIRED 只是文档上标记必填，实际校验靠 @NotBlank/@NotNull

</details>

### Q3：接口文档最大的问题是什么？怎么保证文档和代码同步？

<details>
<summary>点击查看答案</summary>

**最大的问题：文档和代码不同步**。

手写的接口文档（如 Word、Markdown）很容易和代码不同步：代码改了但文档没改，前端按旧文档调用就报错。这是接口文档最常见的问题。

**Knife4j 的解决方案：文档由代码注解自动生成**。
- 文档不是手写的，是根据代码里的注解（@Tag、@Operation、@Schema 等）自动生成的
- 代码改了，只要注解也同步改了，文档自动更新
- 不需要手动维护文档，减少了不同步的可能性

**但 Knife4j 也不能完全保证同步**，因为注解也是代码的一部分，改代码时可能忘了改注解。保证同步的最佳实践：

1. **改接口必改注解**：改了参数、返回值、URL，必须同步改 @Parameter、@Schema 注解，形成习惯
2. **Code Review 检查**：代码审查时检查接口文档注解是否完整、是否和代码一致
3. **CI 检查**：可以在 CI/CD 里加检查，确保所有接口都有 @Operation 注解
4. **定期核对**：定期用 Knife4j 在线调试，验证文档和实际接口行为是否一致
5. **不要用默认描述**：Swagger 默认生成的 "新增接口"、"string" 等于没写文档，必须手动写有意义的描述和示例值

**生产环境注意**：接口文档包含接口细节，生产环境应该关闭（springdoc.swagger-ui.enabled=false）或限制内网访问，避免泄露接口信息。

</details>

### Q4：完成阶段一到三后，你应该具备什么能力？后续学习路线是什么？

<details>
<summary>点击查看答案</summary>

**完成阶段一到三后应该具备的能力**：

1. **完全看懂项目**：理解每一行代码、每个注解、每个技术的作用和原理
2. **修改项目**：能修改现有功能（加字段、调逻辑、改配置）
3. **扩展功能**：能独立新增一个完整功能（数据库设计 → Entity/DTO/VO → Mapper/XML → Service → Controller → 前端 API → 前端页面 → 测试）
4. **部署项目**：能用 Docker 容器化部署（Dockerfile、docker-compose、数据卷、网络、环境变量）
5. **工程化能力**：AOP 切面、参数校验、统一异常、接口文档、缓存、消息队列

**后续学习路线（五阶段总览）**：

| 阶段 | 目标 | 重点内容 |
|------|------|---------|
| 阶段一：看懂项目 | 完全看懂项目代码 | 启动流程、Controller、IOC、Service、MyBatis-Plus、DTO/VO、异常、JWT、MySQL |
| 阶段二：核心技术深入 | 深入理解中间件和前端 | Redis缓存、RabbitMQ、React、前后端联调、参数校验、分页 |
| 阶段三：能扩展功能 | 独立开发和部署 | 新增功能全链路、AOP、Docker、Knife4j |
| **阶段四：能独立开发+面试** | Code Review和面试准备 | Code Review找问题、SQL优化、Redis进阶、MQ进阶、面试高频题 |
| **阶段五：AI应用开发** | Java+AI | Spring AI、LLM API、RAG、Tool Calling、MCP、Agent |

**学习建议**：
- 先把 Java 后端基础打牢（阶段一到四），再学 AI 应用（阶段五）
- 不要跳过基础直接学 Agent，AI 应用开发需要扎实的后端基础
- 你的最终目标是 Java 后端 + AI 应用开发工程师
- 阶段四的 Code Review 和面试题很重要，能帮你找到项目里的不足并准备面试

</details>

---

## 十、面试题

### 面试题1：你们项目的接口文档用的什么？怎么保证文档和代码一致？

> **答题要点**：
> 1. **工具**：用 Knife4j（基于 Swagger/OpenAPI 3 的增强版接口文档工具），版本 4.4.0，Spring Boot 3.x 用 jakarta 版本 starter。
> 2. **访问地址**：http://localhost:8080/api/doc.html，支持在线调试、导出 Markdown/Word、接口分组、全局参数设置。
> 3. **注解**：
>    - Controller 层：@Tag（类描述）、@Operation（方法 summary+description）、@Parameter（参数描述+example）
>    - Model 层：@Schema（类和字段描述，字段加 example 示例值和 requiredMode）
>    - 文档注解和校验注解（@NotBlank/@Size）配合使用，文档注解给 Knife4j 看，校验注解给运行时看
> 4. **保证一致性**：
>    - 文档由代码注解自动生成，不是手写，代码改了注解改了文档自动更新
>    - 团队规范：改接口必须同步改注解，Code Review 时检查
>    - 定期用 Knife4j 在线调试验证文档和实际行为一致
>    - 所有接口必须有 @Operation 和 @Schema，不允许默认描述
> 5. **生产环境**：生产环境关闭文档（springdoc.swagger-ui.enabled=false）或限制内网访问，避免泄露接口信息
> 6. **其他功能**：支持离线文档导出、接口分组（按 Controller）、全局 Token 设置（调试需要登录的接口）

---

## 全部课程完成情况

到这里，**阶段一（第1-10课）、阶段二（第11-16课）、阶段三（第17-20课）共 20 课**已全部生成并写入项目目录。

你现在已经具备了看懂、修改、扩展、部署这个项目的能力。

如果需要继续生成**阶段四（第21-25课：Code Review、SQL优化、Redis进阶、MQ进阶、面试高频）**和**阶段五（AI应用开发）**，告诉我即可。

建议：先把这 20 课认真学完，每课的练习都动手做一遍，真正吃透项目，再继续后面的课程。不要只看不动手。
