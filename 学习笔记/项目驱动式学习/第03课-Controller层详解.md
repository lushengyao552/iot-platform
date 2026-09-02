# 第3课：Controller 层详解——`@RestController`、`@RequestMapping`、参数绑定

> **本课目标**：彻底搞懂 Controller 层的每一个注解，理解 HTTP 请求是如何被映射到 Java 方法的，参数是如何从请求中提取的。学完这课，你应该能独立写出一个标准的 RESTful Controller。

---

## 一、从项目代码开始

打开 `BookController.java`，这是项目里最典型的 Controller：

```java
@Tag(name = "图书管理", description = "图书的增删改查、分页搜索等接口")
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @Operation(summary = "分页查询图书")
    @GetMapping
    public Result<IPage<BookVO>> pageBooks(BookQueryDTO queryDTO) {
        IPage<BookVO> page = bookService.pageBooks(queryDTO);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<BookVO> getBookById(@PathVariable Long id) {
        BookVO bookVO = bookService.getBookById(id);
        return Result.success(bookVO);
    }

    @PostMapping
    public Result<BookVO> addBook(@Valid @RequestBody BookAddDTO addDTO) {
        checkAdmin();
        BookVO bookVO = bookService.addBook(addDTO);
        return Result.success(bookVO);
    }

    @PutMapping("/{id}")
    public Result<BookVO> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookUpdateDTO updateDTO) {
        checkAdmin();
        BookVO bookVO = bookService.updateBook(id, updateDTO);
        return Result.success(bookVO);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteBook(@PathVariable Long id) {
        checkAdmin();
        bookService.deleteBook(id);
        return Result.success();
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
```

我们逐个注解拆解。

---

## 二、`@RestController`——最核心的注解

### 它是什么？

`@RestController` 也是一个**组合注解**，点进去看：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@ResponseBody
public @interface RestController {
}
```

`@RestController` = `@Controller` + `@ResponseBody`

### `@Controller` 是什么？

`@Controller` 标记一个类是 Spring MVC 的控制器，用来接收 HTTP 请求。它本质是 `@Component` 的别名，会被组件扫描注册为 Bean。

传统的 `@Controller` 方法返回值通常是**视图名**（比如 `"login"` → 跳转到 login.html 页面），适合服务端渲染（JSP、Thymeleaf）。

### `@ResponseBody` 是什么？

`@ResponseBody` 加在方法或类上，表示方法的返回值**不是视图名，而是直接写入 HTTP 响应体**。

Spring 会用 `HttpMessageConverter`（默认是 Jackson）把返回的 Java 对象序列化成 JSON 字符串，然后放到 HTTP 响应的 Body 里返回给前端。

### 为什么项目里用 `@RestController` 而不是 `@Controller`？

因为这是一个**前后端分离项目**：
- 前端是 React，负责页面渲染
- 后端只负责提供数据（JSON API）
- 后端不需要返回 HTML 页面，只需要返回 JSON 数据

所以每个 Controller 方法的返回值都要序列化成 JSON。用 `@RestController`（= `@Controller` + `@ResponseBody`）就相当于给类里所有方法都加了 `@ResponseBody`，不用每个方法都写。

### 不写会怎么样？

- 如果只写 `@Controller` 不写 `@ResponseBody`：方法返回值会被当作视图名，Spring 会去找 `Result` 对应的页面文件，找不到就 404
- 如果都不写：这个类不会被注册为控制器，访问 URL 会 404

### 面试官问：`@RestController` 和 `@Controller` 有什么区别？

> - `@Controller`：方法返回值默认是视图名，适合服务端渲染（JSP/Thymeleaf）
> - `@RestController` = `@Controller` + `@ResponseBody`，方法返回值直接写入 HTTP 响应体（序列化成 JSON），适合前后端分离的 RESTful API
> - 前后端分离项目用 `@RestController`，服务端渲染项目用 `@Controller`

---

## 三、`@RequestMapping`——URL 映射

### 它是什么？

`@RequestMapping` 用来把 HTTP 请求映射到 Controller 类或方法上。

加在**类上**：指定这个 Controller 的基础 URL 路径
加在**方法上**：指定这个方法的具体路径（和类上的路径拼接）

### 项目里的用法

```java
@RestController
@RequestMapping("/books")   // 类级别：基础路径 /books
public class BookController {

    @GetMapping              // 方法级别：GET /books（类路径 + 方法路径）
    public Result<IPage<BookVO>> pageBooks(...) { }

    @GetMapping("/{id}")     // 方法级别：GET /books/1
    public Result<BookVO> getBookById(@PathVariable Long id) { }

    @PostMapping             // 方法级别：POST /books
    public Result<BookVO> addBook(...) { }

    @PutMapping("/{id}")     // 方法级别：PUT /books/1
    public Result<BookVO> updateBook(...) { }

    @DeleteMapping("/{id}")  // 方法级别：DELETE /books/1
    public Result<Void> deleteBook(...) { }
}
```

加上 `context-path: /api` 后，完整 URL 是：

| 方法 | 完整 URL | 作用 |
|------|---------|------|
| GET | `/api/books` | 分页查询图书列表 |
| GET | `/api/books/1` | 查询 ID=1 的图书详情 |
| POST | `/api/books` | 新增图书 |
| PUT | `/api/books/1` | 更新 ID=1 的图书 |
| DELETE | `/api/books/1` | 删除 ID=1 的图书 |

### `@GetMapping`、`@PostMapping` 等是什么？

它们是 `@RequestMapping` 的快捷写法，限定了 HTTP 方法：

| 注解 | 等价于 | HTTP 方法 |
|------|--------|----------|
| `@GetMapping` | `@RequestMapping(method = RequestMethod.GET)` | GET（查询） |
| `@PostMapping` | `@RequestMapping(method = RequestMethod.POST)` | POST（新增） |
| `@PutMapping` | `@RequestMapping(method = RequestMethod.PUT)` | PUT（更新） |
| `@DeleteMapping` | `@RequestMapping(method = RequestMethod.DELETE)` | DELETE（删除） |
| `@PatchMapping` | `@RequestMapping(method = RequestMethod.PATCH)` | PATCH（部分更新） |

这就是 **RESTful 风格**的 API 设计：用 HTTP 方法表示操作类型，用 URL 表示资源。

### 补充基础：HTTP 方法是什么？

HTTP 协议定义了几种请求方法，每种方法有语义约定：

| 方法 | 语义 | 幂等 | 有请求体 | 项目里的例子 |
|------|------|------|---------|-------------|
| GET | 查询资源 | 是 | 否 | 查询图书列表、查询详情 |
| POST | 创建资源 | 否 | 是 | 新增图书、登录 |
| PUT | 全量更新资源 | 是 | 是 | 更新图书信息 |
| DELETE | 删除资源 | 是 | 否 | 删除图书 |
| PATCH | 部分更新资源 | 否 | 是 | （项目里没用） |

**幂等**的意思：多次执行和一次执行的效果相同。比如 GET 查询 100 次和 1 次结果一样；DELETE 删除同一条记录 100 次和 1 次效果一样（都删了）。但 POST 新增 100 次会创建 100 条记录，所以不是幂等的。

### 为什么项目里需要它？

没有 URL 映射，Spring 不知道哪个请求该交给哪个方法处理。`@RequestMapping` 建立了"URL + HTTP 方法 → Java 方法"的对应关系。

---

## 四、参数绑定——参数从哪里来？

Controller 方法的参数有几种来源，项目里用到了三种：

### 4.1 `@PathVariable`——从 URL 路径中取参数

```java
@GetMapping("/{id}")
public Result<BookVO> getBookById(@PathVariable Long id) {
```

URL 里的 `{id}` 是**路径变量**，实际请求时替换成具体值：
- 请求 `GET /api/books/1` → `id = 1`
- 请求 `GET /api/books/42` → `id = 42`

`@PathVariable` 把 URL 路径中的变量绑定到方法参数上。

**什么时候用？** 当参数是资源的标识（ID、用户名等），是 URL 的一部分时。

### 4.2 `@RequestParam`——从 URL 查询参数中取

项目里 `pageBooks` 方法没有写 `@RequestParam`，但效果一样：

```java
@GetMapping
public Result<IPage<BookVO>> pageBooks(BookQueryDTO queryDTO) {
```

当参数是一个普通对象（没有 `@RequestBody`）时，Spring 会自动从**URL 查询参数**（query string）中绑定字段。

比如请求：
```
GET /api/books?pageNum=1&pageSize=10&title=Java&author=张三
```

Spring 会自动把 `pageNum=1`、`pageSize=10`、`title=Java`、`author=张三` 绑定到 `BookQueryDTO` 对象的对应字段上。

如果是单个参数，可以显式写 `@RequestParam`：

```java
@GetMapping("/search")
public Result<List<BookVO>> search(
    @RequestParam String keyword,       // 必须传
    @RequestParam(required = false) String category,  // 可选
    @RequestParam(defaultValue = "1") Integer pageNum // 默认值
) {
```

**什么时候用？** 当参数是查询条件、筛选条件、分页参数等，放在 URL 的 `?` 后面时。

### 4.3 `@RequestBody`——从 HTTP 请求体中取（JSON）

```java
@PostMapping
public Result<BookVO> addBook(@Valid @RequestBody BookAddDTO addDTO) {
```

`@RequestBody` 表示参数来自 **HTTP 请求体（Body）**，而不是 URL。

前端发送 POST 请求时，数据放在请求体里，格式是 JSON：

```http
POST /api/books HTTP/1.1
Content-Type: application/json

{
  "isbn": "9787111213826",
  "title": "Java核心技术",
  "author": "Cay S. Horstmann",
  "price": 119.00,
  "stock": 5
}
```

Spring 用 Jackson 把请求体里的 JSON 反序列化成 `BookAddDTO` Java 对象。

**什么时候用？** 当参数较多、结构复杂（比如新增/更新一个对象），数据放在请求体里时。GET 请求一般不用 `@RequestBody`（虽然技术上可以，但不符合规范）。

### 三种参数来源对比

| 注解 | 参数来源 | 典型场景 | HTTP 方法 |
|------|---------|---------|----------|
| `@PathVariable` | URL 路径 `/books/{id}` | 资源 ID | GET、PUT、DELETE |
| `@RequestParam` | URL 查询参数 `?keyword=Java` | 筛选、分页 | GET |
| `@RequestBody` | HTTP 请求体（JSON） | 新增、更新对象 | POST、PUT |

### 补充基础：HTTP 请求的结构

```
POST /api/books HTTP/1.1          ← 请求行（方法 + 路径 + HTTP版本）
Host: localhost:8080               ← 请求头（Headers）
Content-Type: application/json
Authorization: Bearer xxx.yyy.zzz
Content-Length: 123

{                                   ← 请求体（Body），GET 请求一般没有
  "isbn": "9787111213826",
  "title": "Java核心技术"
}
```

- `@RequestParam` 从 URL 的 `?` 后面取（请求行的路径部分）
- `@PathVariable` 从 URL 路径的 `{}` 占位符取
- `@RequestBody` 从请求体取
- `@RequestHeader` 从请求头取（项目里 JWT 拦截器用 `request.getHeader()` 取）

---

## 五、`@Valid`——参数校验

```java
@PostMapping
public Result<BookVO> addBook(@Valid @RequestBody BookAddDTO addDTO) {
```

`@Valid` 开启参数校验。它会校验 `BookAddDTO` 对象上的约束注解，如果校验失败，抛出 `MethodArgumentNotValidException`，被全局异常处理器捕获。

看一下 `BookAddDTO`：

```java
@Data
public class BookAddDTO {
    @NotBlank(message = "ISBN不能为空")
    @Size(max = 20, message = "ISBN长度不能超过20")
    private String isbn;

    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名长度不能超过200")
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 100, message = "作者长度不能超过100")
    private String author;

    @DecimalMin(value = "0", message = "价格不能为负数")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;
}
```

常用校验注解：

| 注解 | 作用 | 例子 |
|------|------|------|
| `@NotNull` | 不能为 null | `@NotNull private Integer stock;` |
| `@NotBlank` | 不能为 null 且不能是空字符串（去掉空格后） | `@NotBlank private String title;` |
| `@NotEmpty` | 不能为 null 且长度 > 0（字符串、集合、数组） | `@NotEmpty private List<String> tags;` |
| `@Size(min, max)` | 长度限制 | `@Size(max = 200) private String title;` |
| `@Min` / `@Max` | 数值最小/最大值 | `@Min(0) private Integer stock;` |
| `@DecimalMin` / `@DecimalMax` | 小数最小/最大值 | `@DecimalMin("0") private BigDecimal price;` |
| `@Email` | 邮箱格式校验 | `@Email private String email;` |
| `@Pattern` | 正则表达式校验 | `@Pattern(regexp = "^1[3-9]\\d{9}$") private String phone;` |

### 为什么项目里需要它？

如果不做参数校验，前端传什么后端就存什么，可能导致：
- 空书名存进数据库
- 负数库存
- 超长字符串超出数据库字段长度，报错
- 恶意输入

参数校验是**防御性编程**的第一道防线，在 Controller 层就把非法数据挡掉。

### `@Valid` 和 `@Validated` 的区别？

| | `@Valid` | `@Validated` |
|---|---------|-------------|
| 来源 | JSR-380 标准（javax.validation / jakarta.validation） | Spring 提供 |
| 支持分组校验 | 支持（`@Valid(groups=...)`） | 支持（`@Validated(Group.class)`） |
| 支持方法级校验 | 不支持 | 支持（加在类上，校验方法参数） |
| 嵌套校验 | 支持（`@Valid` 加在字段上） | 不直接支持，需要配合 `@Valid` |

项目里 Controller 用 `@Valid`（标准用法），这是最常见的写法。

---

## 六、`@RequiredArgsConstructor`——构造器注入

```java
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor    // ← 这个注解
public class BookController {

    private final BookService bookService;   // ← final 字段
```

`@RequiredArgsConstructor` 是 **Lombok** 提供的注解，它会自动生成一个**包含所有 `final` 字段和 `@NonNull` 字段的构造器**。

上面的代码等价于：

```java
@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    // Lombok 自动生成这个构造器
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }
}
```

Spring 看到构造器只有一个参数，会自动从容器中找到 `BookService` 的 Bean 并注入进来——这就是**构造器注入**。

### 三种依赖注入方式对比

| 方式 | 写法 | 优点 | 缺点 |
|------|------|------|------|
| **字段注入** | `@Autowired private BookService bookService;` | 简洁 | 不能注入 final 字段、不利于测试、循环依赖隐藏 |
| **Setter 注入** | `@Autowired public void setBookService(...)` | 可选依赖 | 啰嗦、对象可能未完全初始化 |
| **构造器注入** | `public BookController(BookService bookService)` | 可注入 final、保证依赖不为空、利于测试、循环依赖提前暴露 | 依赖多时构造器参数多（可用 Lombok 解决） |

**Spring 官方推荐构造器注入**。项目里用 `@RequiredArgsConstructor` + `final` 字段就是构造器注入的最佳实践。

### 为什么用 `final`？

1. **不可变**：依赖注入后不允许被修改，保证安全
2. **必须初始化**：`final` 字段必须在构造器中赋值，Spring 注入时如果找不到 Bean 会直接报错，不会出现 NPE
3. **Lombok 识别**：`@RequiredArgsConstructor` 只把 `final` 字段和 `@NonNull` 字段加入构造器

### 面试官问：`@Autowired` 和 `@RequiredArgsConstructor` 有什么区别？

> - `@Autowired` 是 Spring 的注解，用于字段注入或 setter 注入
> - `@RequiredArgsConstructor` 是 Lombok 的注解，自动生成包含 final 字段的构造器，配合 Spring 的构造器注入使用
> - 本质上都是依赖注入，只是注入方式不同
> - 现代 Spring Boot 项目推荐用 `@RequiredArgsConstructor` + `final` 字段（构造器注入），因为更安全、更利于测试
> - 注意：`@RequiredArgsConstructor` 不是 Spring 的注解，它只是帮你生成构造器代码，真正的注入还是 Spring 做的

---

## 七、`Result<T>`——统一返回结果

```java
@GetMapping("/{id}")
public Result<BookVO> getBookById(@PathVariable Long id) {
    BookVO bookVO = bookService.getBookById(id);
    return Result.success(bookVO);   // ← 包装成统一格式
}
```

所有 Controller 方法都返回 `Result<T>`，这是项目的统一响应格式：

```json
{
  "code": 20000,
  "message": "操作成功",
  "data": { "id": 1, "title": "Java核心技术", ... },
  "timestamp": 1693574400000
}
```

### 为什么需要统一返回格式？

如果不统一，有的接口返回对象，有的接口返回字符串，有的接口直接抛异常，前端处理起来非常混乱。

统一格式后：
- 前端通过 `code` 判断成功/失败
- 通过 `message` 显示提示信息
- 通过 `data` 拿业务数据
- 所有接口处理逻辑一致

### `Result.success()` 和 `Result.error()`

```java
// 成功，无数据
return Result.success();

// 成功，带数据
return Result.success(bookVO);

// 失败，默认错误码
return Result.error("操作失败");

// 失败，指定错误码
return Result.error(ResultCode.BOOK_NOT_FOUND);
```

---

## 八、`checkAdmin()`——权限校验（简化版）

```java
@PostMapping
public Result<BookVO> addBook(@Valid @RequestBody BookAddDTO addDTO) {
    checkAdmin();   // ← 手动校验是否管理员
    BookVO bookVO = bookService.addBook(addDTO);
    return Result.success(bookVO);
}

private void checkAdmin() {
    if (!UserContext.isAdmin()) {
        throw new BusinessException(ResultCode.FORBIDDEN);
    }
}
```

项目里没有用 Spring Security，而是用了**简化版权限校验**：
- JWT 拦截器验证登录状态，把用户信息存入 `UserContext`
- 需要管理员权限的接口，手动调用 `checkAdmin()` 检查角色
- 不是管理员就抛出 `BusinessException(FORBIDDEN)`，返回 403

这种方式简单直接，适合小型项目。企业级项目一般用 Spring Security + 方法级注解（`@PreAuthorize`）。

---

## 九、Swagger/Knife4j 注解——接口文档

```java
@Tag(name = "图书管理", description = "图书的增删改查、分页搜索等接口")
@RestController
public class BookController {

    @Operation(summary = "分页查询图书", description = "支持按书名、作者、ISBN、分类等条件搜索")
    @GetMapping
    public Result<IPage<BookVO>> pageBooks(BookQueryDTO queryDTO) {
```

| 注解 | 作用 |
|------|------|
| `@Tag` | 接口分组，显示在 Knife4j 文档的侧边栏 |
| `@Operation` | 单个接口的说明（summary 标题、description 详细描述） |
| `@Parameter` | 参数说明 |
| `@Schema` | 实体类/DTO 字段说明（加在字段上） |

这些注解不影响业务逻辑，只是用来生成接口文档。访问 `http://localhost:8080/api/doc.html` 可以看到可视化的接口文档。

---

## 十、本课必须记住的 7 件事

1. **`@RestController` = `@Controller` + `@ResponseBody`**：返回值直接序列化成 JSON，适合前后端分离
2. **`@RequestMapping` 映射 URL**：类级别是基础路径，方法级别是具体路径；`@GetMapping`/`@PostMapping` 是限定 HTTP 方法的快捷写法
3. **三种参数绑定**：`@PathVariable`（URL路径）、`@RequestParam`（URL查询参数）、`@RequestBody`（请求体JSON）
4. **`@Valid` 开启参数校验**：配合 DTO 上的 `@NotBlank`、`@NotNull`、`@Size` 等注解，在 Controller 层挡掉非法数据
5. **`@RequiredArgsConstructor` + `final` = 构造器注入**：Lombok 自动生成构造器，Spring 自动注入，这是官方推荐的注入方式
6. **统一返回 `Result<T>`**：所有接口返回 `{code, message, data, timestamp}`，前端统一处理
7. **RESTful 风格**：GET 查询、POST 新增、PUT 更新、DELETE 删除，URL 用名词表示资源

---

## 十一、本节关键代码

```java
@Tag(name = "图书管理")
@RestController                                  // 控制器 + 返回JSON
@RequestMapping("/books")                        // 基础路径
@RequiredArgsConstructor                         // Lombok 生成构造器，注入 final 字段
public class BookController {

    private final BookService bookService;       // 构造器注入

    @GetMapping                                   // GET /books
    public Result<IPage<BookVO>> pageBooks(BookQueryDTO queryDTO) {  // 参数自动绑定查询参数
        return Result.success(bookService.pageBooks(queryDTO));
    }

    @GetMapping("/{id}")                          // GET /books/1
    public Result<BookVO> getBookById(@PathVariable Long id) {       // 从URL路径取id
        return Result.success(bookService.getBookById(id));
    }

    @PostMapping                                  // POST /books
    public Result<BookVO> addBook(@Valid @RequestBody BookAddDTO dto) {  // 从请求体取JSON + 校验
        checkAdmin();
        return Result.success(bookService.addBook(dto));
    }

    @PutMapping("/{id}")                          // PUT /books/1
    public Result<BookVO> updateBook(@PathVariable Long id,
                                       @Valid @RequestBody BookUpdateDTO dto) {
        checkAdmin();
        return Result.success(bookService.updateBook(id, dto));
    }

    @DeleteMapping("/{id}")                       // DELETE /books/1
    public Result<Void> deleteBook(@PathVariable Long id) {
        checkAdmin();
        bookService.deleteBook(id);
        return Result.success();
    }
}
```

---

## 十二、本节练习

### 练习1：给图书加一个"按出版社查询"的接口

在 `BookController` 里新增一个 GET 接口，支持按出版社模糊查询：

```
GET /api/books/publisher?name=机械工业出版社
```

要求：
- 用 `@RequestParam` 接收参数
- 返回 `Result<List<BookVO>>`
- 在 `BookService` 里加对应方法，用 `LambdaQueryWrapper` 的 `like` 查询

> **提示**：参考 `pageBooks` 方法里的 `buildQueryWrapper`，里面已经有 `wrapper.like(Book::getTitle, ...)` 的写法。

### 练习2：给 `BookAddDTO` 加校验规则

给 `BookAddDTO` 的 `isbn` 字段加一个正则校验，要求 ISBN 必须是 13 位数字：

```java
@Pattern(regexp = "^\\d{13}$", message = "ISBN必须是13位数字")
private String isbn;
```

然后用 Postman 或 Knife4j 发送一个非法 ISBN（比如 "abc"），观察返回的错误信息。

### 练习3：理解构造器注入

把 `BookController` 里的 `@RequiredArgsConstructor` 去掉，手动写出构造器，然后启动项目验证是否正常。再改回 `@RequiredArgsConstructor`。

> **目的**：理解 Lombok 帮你生成了什么代码。

---

## 十三、自测题

### Q1：`@RestController` 和 `@Controller` 有什么区别？

<details>
<summary>点击查看答案</summary>

- `@Controller`：方法返回值默认被当作视图名（如 "login" → 找 login.html），适合服务端渲染（JSP/Thymeleaf）
- `@RestController` = `@Controller` + `@ResponseBody`：方法返回值直接写入 HTTP 响应体（Jackson 序列化成 JSON），适合前后端分离的 RESTful API
- 项目是前后端分离，所以用 `@RestController`

</details>

### Q2：`@PathVariable`、`@RequestParam`、`@RequestBody` 分别从哪里取参数？各举一个项目里的例子。

<details>
<summary>点击查看答案</summary>

- **`@PathVariable`**：从 URL 路径中取。例子：`GET /api/books/1` → `@PathVariable Long id` 得到 `id=1`
- **`@RequestParam`**：从 URL 查询参数（`?` 后面）取。例子：`GET /api/books?pageNum=1&pageSize=10` → 绑定到 `BookQueryDTO` 的字段
- **`@RequestBody`**：从 HTTP 请求体取（JSON 格式）。例子：`POST /api/books`，请求体里的 JSON 反序列化成 `BookAddDTO` 对象

</details>

### Q3：为什么项目里用 `@RequiredArgsConstructor` + `final` 字段注入依赖，而不是 `@Autowired` 字段注入？

<details>
<summary>点击查看答案</summary>

`@RequiredArgsConstructor` 是 Lombok 注解，自动生成包含所有 `final` 字段的构造器，配合 Spring 的**构造器注入**。相比 `@Autowired` 字段注入：

1. **可以注入 final 字段**：保证依赖不可变，注入后不能被修改
2. **保证依赖不为空**：final 字段必须在构造器中赋值，Spring 找不到 Bean 会直接报错，不会出现运行时 NPE
3. **利于单元测试**：可以直接 `new BookController(mockBookService)`，不需要 Spring 容器
4. **循环依赖提前暴露**：构造器注入在启动时就能发现循环依赖，字段注入可能运行时才报错
5. **Spring 官方推荐**构造器注入

`@Autowired` 字段注入虽然简洁，但有上述缺点，现代项目不推荐。

</details>

### Q4：`@Valid` 注解的作用是什么？如果不加会怎么样？

<details>
<summary>点击查看答案</summary>

`@Valid` 开启参数校验，Spring 会校验参数对象上的约束注解（`@NotBlank`、`@NotNull`、`@Size` 等）。如果校验失败，抛出 `MethodArgumentNotValidException`，被全局异常处理器捕获，返回 400 错误和具体的校验失败信息。

如果不加 `@Valid`，DTO 上的校验注解不会生效，前端传什么后端就接收什么，可能导致空值、非法值存入数据库，或者在业务层/数据库层报错。

参数校验是防御性编程的第一道防线，应该在 Controller 层就挡掉非法数据。

</details>

### Q5：RESTful 风格的 API 设计中，GET、POST、PUT、DELETE 分别对应什么操作？项目里是怎么用的？

<details>
<summary>点击查看答案</summary>

- **GET**：查询资源（幂等、无请求体）。项目里：`GET /api/books` 分页查询、`GET /api/books/{id}` 查询详情
- **POST**：创建资源（非幂等、有请求体）。项目里：`POST /api/books` 新增图书、`POST /api/auth/login` 登录
- **PUT**：全量更新资源（幂等、有请求体）。项目里：`PUT /api/books/{id}` 更新图书
- **DELETE**：删除资源（幂等、无请求体）。项目里：`DELETE /api/books/{id}` 删除图书

核心思想：用 HTTP 方法表示操作类型，用 URL 路径表示资源（名词），而不是用动词（如 `/getBook`、`/deleteBook`）。

</details>

---

## 十四、面试题

### 面试题1：Spring MVC 的请求处理流程是怎样的？

> **答题要点**：
> 1. 请求到达 `DispatcherServlet`（前端控制器），它是 Spring MVC 的核心，所有请求都经过它
> 2. `DispatcherServlet` 调用 `HandlerMapping`，根据 URL + HTTP 方法找到对应的 Controller 方法（Handler）
> 3. 调用 `HandlerAdapter`，执行拦截器链（`preHandle`），然后执行 Controller 方法
> 4. Controller 方法执行业务逻辑，返回 `ModelAndView` 或直接返回对象（`@ResponseBody`）
> 5. 如果返回的是对象，`HttpMessageConverter`（Jackson）把对象序列化成 JSON，写入响应体
> 6. 执行拦截器链（`postHandle`、`afterCompletion`）
> 7. `DispatcherServlet` 返回响应
>
> 结合项目：请求 `/api/books/1` → `DispatcherServlet` → `JwtInterceptor.preHandle` 验证 Token → `BookController.getBookById` → 返回 `Result<BookVO>` → Jackson 序列化成 JSON → `JwtInterceptor.afterCompletion` 清理 ThreadLocal → 返回前端

### 面试题2：`@RequestBody` 和 `@RequestParam` 有什么区别？什么时候用哪个？

> **答题要点**：
> - `@RequestParam`：从 URL 查询参数（query string）中取，参数在 `?` 后面，如 `?name=Java&page=1`。适合参数少、结构简单的场景，主要用于 GET 请求的查询条件
> - `@RequestBody`：从 HTTP 请求体中取，参数是 JSON 格式。适合参数多、结构复杂（对象嵌套）的场景，主要用于 POST/PUT 的新增和更新
> - GET 请求一般不用 `@RequestBody`（不符合规范，虽然技术上可以）
> - `@RequestParam` 参数在 URL 里可见，有长度限制；`@RequestBody` 在请求体里，不可见，适合敏感数据和大数据量
> - 项目里：分页查询用 `BookQueryDTO`（等价于 `@RequestParam` 绑定），新增/更新用 `@RequestBody`

### 面试题3：Spring 的依赖注入有哪几种方式？推荐哪种？为什么？

> **答题要点**：
> 1. **字段注入**：`@Autowired private BookService bookService;`。简洁但不能注入 final、不利于测试、隐藏循环依赖
> 2. **Setter 注入**：`@Autowired public void setBookService(BookService bookService)`。适合可选依赖，但对象可能未完全初始化
> 3. **构造器注入**：`public BookController(BookService bookService)`。Spring 官方推荐
>
> **推荐构造器注入**，原因：
> - 可注入 final 字段，保证依赖不可变
> - 保证依赖不为空（构造器必须赋值）
> - 利于单元测试（直接 new，不需要 Spring 容器）
> - 循环依赖在启动时就暴露，而不是运行时
> - 项目里用 `@RequiredArgsConstructor`（Lombok）+ `final` 字段，就是构造器注入的最佳实践，避免了手写构造器的繁琐

---

## 十五、下一课预告

**第4课：Spring IOC/DI 核心——`@Service`、Bean 生命周期、依赖注入原理**

我们会搞清楚：
- Spring 容器（IOC 容器）到底是什么？Bean 是什么？
- `@Service`、`@Component`、`@Repository` 有什么区别？
- Bean 的生命周期（实例化 → 属性注入 → 初始化 → 销毁）
- 依赖注入的底层原理（反射、构造器）
- 单例 Bean 和多例 Bean 的区别
- 循环依赖问题（为什么构造器注入会提前暴露）
