# 第7课：Entity / DTO / VO 三层对象设计——为什么要分这么多类？

> **本课目标**：理解项目里为什么有 `Book`（Entity）、`BookAddDTO`（DTO）、`BookVO`（VO）三个类，分别用在哪一层，以及 `BeanUtils.copyProperties` 怎么做对象拷贝。学完这课，你应该能向面试官讲清楚三层对象的设计原则和企业项目规范。

---

## 一、从项目代码开始

看 `BookServiceImpl` 的 `addBook` 方法：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public BookVO addBook(BookAddDTO addDTO) {          // ① 入参是 DTO
    // 1. 校验 ISBN 是否已存在
    LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Book::getIsbn, addDTO.getIsbn());
    if (count(wrapper) > 0) {
        throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "ISBN 已存在");
    }

    // 2. 校验分类是否存在
    if (addDTO.getCategoryId() != null) {
        BookCategory category = categoryService.getById(addDTO.getCategoryId());
        if (category == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
    }

    // 3. 构建实体并保存
    Book book = new Book();                           // ② 中间用 Entity
    BeanUtils.copyProperties(addDTO, book);           //    DTO → Entity 拷贝
    book.setTotalStock(addDTO.getStock());
    save(book);                                        //    Entity 存数据库

    log.info("新增图书成功: bookId={}, title={}", book.getId(), book.getTitle());
    return toVO(book);                                 // ③ 返回是 VO（Entity → VO）
}
```

一个方法里出现了三种对象：
- **入参** `BookAddDTO`：从前端接收的数据
- **中间** `Book`（Entity）：和数据库表对应的实体，用于持久化
- **返回** `BookVO`：返回给前端的数据

为什么不能只用一个 `Book` 类？下面详细讲。

---

## 二、Entity（实体类）——和数据库表一一对应

### 是什么？

Entity 是和数据库表一一对应的 Java 类，表的每一行对应一个 Entity 对象，表的每一列对应 Entity 的一个字段。

项目里的 Entity：
- `Book` → 对应 `book` 表
- `User` → 对应 `sys_user` 表
- `BookCategory` → 对应 `book_category` 表
- `BorrowRecord` → 对应 `borrow_record` 表

### 长什么样？

```java
@Data
@TableName("book")                    // 对应数据库表名
public class Book implements Serializable {

    @TableId(type = IdType.AUTO)     // 主键，自增
    private Long id;

    private String isbn;              // 普通字段，和表列一一对应
    private String title;
    private String author;
    private Long categoryId;
    private Integer stock;
    private Integer totalStock;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;           // 逻辑删除字段，前端不应该看到
}
```

### 用在哪一层？

Entity 用在 **Service 层和 Mapper 层**，是和数据库交互的对象：
- Mapper 方法的参数和返回值是 Entity（`selectById` 返回 `Book`）
- Service 内部业务逻辑用 Entity
- `save(book)`、`updateById(book)` 这些 MyBatis-Plus 方法操作的是 Entity

### 为什么不能直接把 Entity 返回给前端？

1. **敏感字段泄露**：`User` 实体里有 `password` 字段（虽然是加密的），直接返回给前端不安全
2. **内部字段暴露**：`deleted`（逻辑删除）、`updateTime` 等内部字段不应该暴露给前端
3. **字段不够**：前端需要的 `categoryName`（分类名称）在 `Book` 实体里没有，只有 `categoryId`
4. **字段多余**：前端不需要 `totalStock`（总藏书量），但 Entity 里有
5. **耦合**：前端和数据库表结构直接耦合，表结构一变前端就崩了

---

## 三、DTO（数据传输对象）——接收前端请求参数

### 是什么？

DTO（Data Transfer Object，数据传输对象）是**从前端接收请求参数**的对象，用于 Controller 层的入参。

项目里的 DTO：
- `BookAddDTO`：新增图书的请求参数
- `BookUpdateDTO`：更新图书的请求参数
- `BookQueryDTO`：分页查询图书的请求参数
- `LoginDTO`：登录请求（用户名+密码）
- `RegisterDTO`：注册请求

### 长什么样？

```java
@Data
@Schema(description = "新增图书请求")
public class BookAddDTO {

    @NotBlank(message = "ISBN不能为空")        // 参数校验注解
    @Size(max = 20, message = "ISBN长度不能超过20")
    private String isbn;

    @NotBlank(message = "书名不能为空")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "作者不能为空")
    private String author;

    private String publisher;
    private LocalDate publishDate;
    private Long categoryId;

    @DecimalMin(value = "0.00", message = "价格不能为负数")
    private BigDecimal price;

    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能为负数")
    private Integer stock;

    private String description;
    private String coverUrl;
}
```

### 用在哪一层？

DTO 用在 **Controller 层的入参**：

```java
@PostMapping
public Result<BookVO> addBook(@Valid @RequestBody BookAddDTO addDTO) {
    // addDTO 是从前端 JSON 请求体反序列化来的
    BookVO bookVO = bookService.addBook(addDTO);
    return Result.success(bookVO);
}
```

### 为什么接收前端参数要用 DTO 而不是 Entity？

1. **字段不同**：新增图书时不需要 `id`、`createTime`、`updateTime`、`deleted` 这些字段，但 Entity 里有。用 DTO 可以精确控制前端能传哪些字段
2. **校验规则不同**：新增时 `title` 必填，更新时 `title` 可选。如果用同一个 Entity，校验规则没法区分（除非用分组校验，更复杂）
3. **防止越权**：前端如果传了 `id=1`、`deleted=1` 这些不该由前端控制的字段，用 Entity 接收就会被恶意修改。用 DTO 只暴露允许前端修改的字段
4. **解耦**：前端请求结构和数据库表结构解耦，表结构变了不影响前端接口

### 项目里为什么有 `BookAddDTO` 和 `BookUpdateDTO` 两个？

因为新增和更新的校验规则不同：
- 新增时：`isbn`、`title`、`author`、`stock` 都是必填
- 更新时：所有字段都是可选（只更新传了的字段），而且 `isbn` 一般不允许修改

如果用同一个 DTO，就没法区分"新增时必填"和"更新时可选"。所以分开写更清晰。

---

## 四、VO（视图对象）——返回给前端的数据

### 是什么？

VO（View Object，视图对象）是**返回给前端展示用**的对象，Controller 方法的返回值包装在 `Result<VO>` 里。

项目里的 VO：
- `BookVO`：返回给前端的图书信息
- `UserVO`：返回给前端的用户信息（不含密码）
- `LoginVO`：登录成功返回的信息（Token + 用户信息）
- `BorrowRecordVO`：借阅记录 VO（含用户名、书名等关联信息）
- `CategoryVO`：分类 VO

### 长什么样？

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "图书信息")
public class BookVO {

    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private LocalDate publishDate;
    private Long categoryId;

    private String categoryName;     // ← Entity 里没有，是关联查询出来的分类名称

    private BigDecimal price;
    private Integer stock;
    private Integer totalStock;

    private Boolean available;       // ← Entity 里没有，是计算出来的"是否可借"

    private String description;
    private String coverUrl;
    private LocalDateTime createTime;
    // 注意：没有 deleted 字段（不暴露给前端）
}
```

### 用在哪一层？

VO 用在 **Controller 层的返回值**：

```java
@GetMapping("/{id}")
public Result<BookVO> getBookById(@PathVariable Long id) {
    BookVO bookVO = bookService.getBookById(id);
    return Result.success(bookVO);   // 返回 VO 给前端
}
```

### 为什么返回给前端要用 VO 而不是 Entity？

1. **隐藏敏感字段**：`UserVO` 里没有 `password` 字段，`BookVO` 里没有 `deleted` 字段
2. **补充关联信息**：`BookVO` 里有 `categoryName`（分类名称），但 `Book` 实体里只有 `categoryId`。前端展示需要分类名称，不能让前端再发一次请求查分类
3. **计算派生字段**：`BookVO` 里有 `available`（是否可借），是根据 `stock > 0` 计算出来的，数据库里没有这个字段
4. **精确控制返回字段**：只返回前端需要的字段，不返回多余的内部字段
5. **解耦**：前端展示结构和数据库表结构解耦

---

## 五、三层对象对比总结

| | Entity（实体） | DTO（数据传输对象） | VO（视图对象） |
|---|---|---|---|
| **全称** | Entity / DO（Data Object） | Data Transfer Object | View Object |
| **作用** | 和数据库表一一对应，持久化用 | 接收前端请求参数 | 返回给前端展示 |
| **用在哪层** | Service 层、Mapper 层 | Controller 层入参 | Controller 层返回值 |
| **字段来源** | 数据库表列 | 前端请求 JSON | 前端展示需要（Entity 字段 + 关联查询 + 计算字段） |
| **注解** | `@TableName`、`@TableId`、`@TableField`、`@TableLogic` | `@NotBlank`、`@NotNull` 等校验注解 | `@Schema`（接口文档）、`@Builder` |
| **项目例子** | `Book`、`User`、`BorrowRecord` | `BookAddDTO`、`LoginDTO`、`BookQueryDTO` | `BookVO`、`LoginVO`、`BorrowRecordVO` |
| **能不能跨层用** | 不能直接返回前端 | 不能直接存数据库 | 不能直接存数据库 |

### 数据流向图

```
前端发送 JSON 请求
    │
    ▼
Controller 接收 → DTO（@RequestBody 反序列化）
    │
    ▼
Service 层：DTO → Entity（BeanUtils.copyProperties）
    │           业务逻辑处理
    ▼
Mapper 层：Entity → 数据库（INSERT/UPDATE/SELECT）
    │
    ▼
数据库返回 → Entity
    │
    ▼
Service 层：Entity → VO（toVO 方法，补充关联信息、计算字段）
    │
    ▼
Controller 返回 → Result<VO>（Jackson 序列化成 JSON）
    │
    ▼
前端收到 JSON 响应
```

---

## 六、`BeanUtils.copyProperties`——对象拷贝

### 是什么？

`BeanUtils.copyProperties(source, target)` 是 Spring 提供的工具方法，把源对象（source）的**同名字段**拷贝到目标对象（target）。

项目里的用法：

```java
// DTO → Entity
Book book = new Book();
BeanUtils.copyProperties(addDTO, book);   // 把 addDTO 的同名字段拷贝到 book

// Entity → VO
BookVO vo = new BookVO();
BeanUtils.copyProperties(book, vo);        // 把 book 的同名字段拷贝到 vo
```

### 拷贝规则

1. **同名字段才拷贝**：源对象和目标对象都有 `title` 字段，就拷贝；源对象有 `deleted` 但目标对象没有，就不拷贝
2. **类型要兼容**：源字段是 `String`，目标字段是 `Long`，拷贝不了（会跳过）
3. **null 值也会拷贝**：源对象的 `title` 是 null，目标对象的 `title` 也会被设为 null（这是个坑！）
4. **浅拷贝**：只拷贝字段的引用，不拷贝对象内部的嵌套对象（引用类型字段共享同一个对象）

### 项目里的 `toVO` 方法

```java
@Override
public BookVO toVO(Book book) {
    if (book == null) {
        return null;
    }
    BookVO vo = new BookVO();
    BeanUtils.copyProperties(book, vo);   // 先拷贝同名字段

    // 再补充 Entity 里没有的字段
    if (book.getCategoryId() != null) {
        BookCategory category = categoryService.getById(book.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());   // 关联查询分类名称
        }
    }

    vo.setAvailable(book.getStock() != null && book.getStock() > 0);  // 计算字段

    return vo;
}
```

这是标准的 VO 转换模式：**先 `copyProperties` 拷贝同名字段，再手动补充关联字段和计算字段**。

### `BeanUtils.copyProperties` 的坑

1. **null 值覆盖**：源对象字段为 null 时，会把目标对象的同名字段也设为 null。更新时如果只想更新非空字段，不能直接用 `copyProperties`
   - 项目里 `updateBook` 方法的处理：`BeanUtils.copyProperties(updateDTO, book, "stock")`，第三个参数是"忽略的字段"，跳过 stock 字段的拷贝，手动处理

2. **性能问题**：`BeanUtils.copyProperties` 用反射实现，性能比手动 getter/setter 差。但在普通业务场景下，性能差异可以忽略不计。只有在超高并发场景（如秒杀）才需要考虑用 MapStruct 等编译期生成代码的工具

3. **类型不匹配静默跳过**：源字段是 `Integer`，目标字段是 `String`，不会报错，只是跳过这个字段。排查问题时容易忽略

4. **两个 `BeanUtils`**：Spring 的 `org.springframework.beans.BeanUtils` 和 Apache 的 `org.apache.commons.beanutils.BeanUtils` 都叫 `BeanUtils`，但方法参数顺序相反！
   - Spring：`copyProperties(source, target)`
   - Apache：`copyProperties(target, source)`
   - 项目里用的是 Spring 的（`import org.springframework.beans.BeanUtils`）

---

## 七、项目里的对象设计有什么值得学的？

### 优点

1. **分层清晰**：Entity、DTO、VO 各司其职，没有混用
2. **DTO 细分**：新增和更新分开（`BookAddDTO`、`BookUpdateDTO`），校验规则不同
3. **VO 补充关联信息**：`BookVO` 有 `categoryName`，`BorrowRecordVO` 有 `username`、`bookTitle`，前端不需要二次查询
4. **敏感字段不暴露**：VO 里没有 `password`、`deleted` 等字段
5. **统一转换方法**：每个 Service 都有 `toVO` 方法，转换逻辑集中管理

### 可以改进的地方（Code Review 视角）

1. **`toVO` 里的 N+1 查询问题**：`BookVO.toVO` 里每次都查 `categoryService.getById(categoryId)`，如果是列表查询（10 条记录），就会发 10 次分类查询（N+1 问题）。应该批量查询分类后再设置，或者用 JOIN 查询一次性查出
2. **`BeanUtils.copyProperties` 的 null 覆盖**：更新时如果前端传了 null 字段，会把数据库里的有效值覆盖为 null。应该用更智能的拷贝工具（如只拷贝非 null 字段）
3. **缺少 DO/BO 分层**：大型项目会进一步分 DO（数据库对象）、BO（业务对象）、VO（视图对象）。但这个项目规模不大，Entity 兼做 BO 是可以接受的

---

## 八、本课必须记住的 7 件事

1. **三层对象各司其职**：Entity（数据库持久化）、DTO（接收前端请求）、VO（返回前端展示），不能混用
2. **Entity 不能直接返回前端**：会泄露敏感字段（password、deleted）、缺少关联信息（categoryName）、前后端耦合
3. **DTO 不能直接存数据库**：字段不完整（没有 id、createTime）、校验规则和 Entity 不同、防止越权修改
4. **VO 比 Entity 多了关联字段和计算字段**：如 `categoryName`（关联查询）、`available`（计算得出），同时去掉了敏感字段
5. **数据流向**：前端 JSON → DTO → Entity → 数据库 → Entity → VO → 前端 JSON
6. **`BeanUtils.copyProperties(source, target)`**：Spring 提供的对象拷贝工具，同名字段拷贝，用反射实现，null 值会覆盖目标值
7. **新增和更新用不同的 DTO**：因为校验规则不同（新增必填多，更新全可选），项目里 `BookAddDTO` 和 `BookUpdateDTO` 分开是正确的

---

## 九、本节关键代码

```java
// Controller 层：入参 DTO，返回 VO
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;

    @PostMapping
    public Result<BookVO> addBook(@Valid @RequestBody BookAddDTO addDTO) {  // 入参 DTO
        BookVO bookVO = bookService.addBook(addDTO);
        return Result.success(bookVO);                                          // 返回 VO
    }
}

// Service 层：DTO → Entity → 数据库 → Entity → VO
@Service
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookVO addBook(BookAddDTO addDTO) {        // 接收 DTO
        // 校验...

        Book book = new Book();
        BeanUtils.copyProperties(addDTO, book);        // DTO → Entity 拷贝
        book.setTotalStock(addDTO.getStock());
        save(book);                                      // Entity 存数据库

        return toVO(book);                               // Entity → VO
    }

    @Override
    public BookVO toVO(Book book) {                     // Entity → VO 转换
        if (book == null) return null;
        BookVO vo = new BookVO();
        BeanUtils.copyProperties(book, vo);              // 先拷贝同名字段

        // 再补充关联字段和计算字段
        if (book.getCategoryId() != null) {
            BookCategory category = categoryService.getById(book.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());   // 关联查询
            }
        }
        vo.setAvailable(book.getStock() != null && book.getStock() > 0);  // 计算字段
        return vo;
    }
}
```

---

## 十、本节练习

### 练习1：给 User 模块加三层对象

项目里 `User` 实体有 `password` 字段。请：
1. 创建 `UserAddDTO`（新增用户请求，包含 username、password、nickname、email、phone，加校验注解）
2. 创建 `UserVO`（返回给前端的用户信息，包含 id、username、nickname、email、phone、role、status、createTime，**不包含 password**）
3. 在 `UserService` 里写 `toVO(User user)` 方法，把 Entity 转成 VO
4. 在 `UserController` 里把返回值从 `User` 改成 `UserVO`

### 练习2：理解 BeanUtils.copyProperties 的 null 覆盖问题

写一个测试方法：
```java
Book source = new Book();
source.setTitle(null);  // 源对象 title 为 null
source.setAuthor("张三");

Book target = new Book();
target.setTitle("原书名");
target.setAuthor("原作者");

BeanUtils.copyProperties(source, target);
System.out.println(target.getTitle());   // 输出什么？
System.out.println(target.getAuthor());  // 输出什么？
```

运行后观察结果，理解 null 值覆盖问题。然后思考：更新图书时，如果前端只传了 `author` 没传 `title`，直接用 `copyProperties` 会有什么问题？应该怎么解决？

### 练习3：给 BookVO 加一个字段

给 `BookVO` 加一个 `categoryDescription`（分类描述）字段，在 `toVO` 方法里从 `BookCategory` 实体中获取并设置。然后调用查询接口，观察返回的 JSON 里是否有这个新字段。

---

## 十一、自测题

### Q1：Entity、DTO、VO 分别是什么？分别用在哪一层？

<details>
<summary>点击查看答案</summary>

- **Entity（实体/DO）**：和数据库表一一对应的 Java 类，用于持久化。用在 Service 层和 Mapper 层，是和数据库交互的对象。项目例子：`Book`、`User`、`BorrowRecord`。
- **DTO（Data Transfer Object，数据传输对象）**：接收前端请求参数的对象。用在 Controller 层的入参。项目例子：`BookAddDTO`、`LoginDTO`、`BookQueryDTO`。
- **VO（View Object，视图对象）**：返回给前端展示用的对象。用在 Controller 层的返回值。项目例子：`BookVO`、`LoginVO`、`BorrowRecordVO`。

数据流向：前端 JSON → DTO → Entity → 数据库 → Entity → VO → 前端 JSON。

</details>

### Q2：为什么不能直接把 Entity 返回给前端？至少说 3 个原因。

<details>
<summary>点击查看答案</summary>

1. **敏感字段泄露**：`User` 实体有 `password` 字段，`Book` 有 `deleted`（逻辑删除）字段，直接返回不安全
2. **缺少关联信息**：前端需要 `categoryName`（分类名称），但 `Book` 实体只有 `categoryId`，直接返回前端还得再发请求查分类
3. **缺少计算字段**：前端需要 `available`（是否可借），这是根据 `stock > 0` 计算的，数据库里没有
4. **字段多余**：`totalStock`、`updateTime` 等内部字段前端不需要
5. **前后端耦合**：前端和数据库表结构直接绑定，表结构一变前端接口就变了，不利于维护

</details>

### Q3：为什么接收前端参数要用 DTO 而不是 Entity？

<details>
<summary>点击查看答案</summary>

1. **字段不同**：新增图书时不需要 `id`、`createTime`、`updateTime`、`deleted` 这些字段，用 Entity 接收会把这些不该由前端控制的字段也暴露出来
2. **校验规则不同**：新增时 `title` 必填，更新时 `title` 可选，同一个 Entity 没法区分两种校验规则（除非用分组校验，更复杂）
3. **防止越权**：前端如果恶意传了 `id=1`、`deleted=1`、`role=ADMIN` 这些字段，用 Entity 接收就会被恶意修改。用 DTO 只暴露允许前端修改的字段
4. **解耦**：前端请求结构和数据库表结构解耦，表结构变了不影响前端接口

</details>

### Q4：`BeanUtils.copyProperties(source, target)` 是怎么工作的？有什么坑？

<details>
<summary>点击查看答案</summary>

`BeanUtils.copyProperties` 是 Spring 提供的对象拷贝工具，通过**反射**把源对象（source）的**同名字段**拷贝到目标对象（target）。

工作规则：
1. 同名字段才拷贝（源和目标都有 `title` 才拷贝）
2. 类型要兼容（不兼容的字段静默跳过）
3. null 值也会拷贝（源字段为 null 会覆盖目标字段的值）
4. 浅拷贝（引用类型字段共享同一个对象）

常见坑：
1. **null 值覆盖**：更新时前端只传了部分字段，没传的字段为 null，会把数据库里的有效值覆盖为 null。解决：用 `copyProperties(source, target, "忽略字段")` 或只拷贝非 null 字段的工具
2. **性能问题**：反射比手动 getter/setter 慢，超高并发场景考虑 MapStruct
3. **类型不匹配静默跳过**：不报错，排查问题时容易忽略
4. **两个 BeanUtils 混淆**：Spring 的参数顺序是 `(source, target)`，Apache 的是 `(target, source)`，搞反了就拷贝错了

</details>

### Q5：项目里的 `BookVO` 为什么比 `Book` 多了 `categoryName` 和 `available` 字段？

<details>
<summary>点击查看答案</summary>

- **`categoryName`（分类名称）**：`Book` 实体里只有 `categoryId`（分类ID），但前端展示图书列表时需要显示分类名称（如"计算机科学"），而不是分类ID。如果不把分类名称放在 VO 里，前端拿到图书列表后还得为每本书再发一次请求查分类名称（N+1 查询问题）。所以在 `toVO` 方法里根据 `categoryId` 查出分类名称，一起返回给前端。
- **`available`（是否可借）**：这是一个派生字段，根据 `stock > 0` 计算得出。数据库里只存了 `stock`（库存数量），没有 `available` 字段。前端展示时需要知道这本书"是否可借"，如果只返回 stock，前端还得自己判断 `stock > 0`。在后端计算好 `available` 直接返回，前端更方便，也统一了判断逻辑。

这两个字段都是 VO 特有的，Entity 里没有，体现了 VO 的作用：**面向前端展示，而不是面向数据库存储**。

</details>

---

## 十二、面试题

### 面试题1：你们项目里 Entity、DTO、VO 是怎么分层的？为什么这么设计？

> **答题要点**：
> 1. **Entity（DO）**：和数据库表一一对应，用 `@TableName`、`@TableId` 注解，用于 Service 层和 Mapper 层的持久化操作。项目里有 `Book`、`User`、`BorrowRecord`、`BookCategory`
> 2. **DTO**：接收前端请求参数，用 `@Valid` + `@NotBlank` 等校验注解，用于 Controller 层入参。项目里按操作细分：`BookAddDTO`（新增）、`BookUpdateDTO`（更新）、`BookQueryDTO`（查询）、`LoginDTO`、`RegisterDTO`
> 3. **VO**：返回给前端展示，用于 Controller 层返回值。项目里有 `BookVO`、`UserVO`、`LoginVO`、`BorrowRecordVO`
>
> **为什么这么设计**：
> - 解耦：前端请求/响应结构和数据库表结构解耦，表结构变更不影响接口
> - 安全：VO 隐藏敏感字段（password、deleted），DTO 防止越权修改内部字段
> - 精确：DTO 按操作细分（新增/更新/查询），校验规则不同；VO 补充关联信息（categoryName）和计算字段（available），前端不需要二次查询
> - 可维护：每层对象职责单一，修改数据库表不影响前端，修改前端接口不影响数据库

### 面试题2：`BeanUtils.copyProperties` 和 MapStruct 有什么区别？你们项目用的哪个？为什么？

> **答题要点**：
> 1. **`BeanUtils.copyProperties`**：Spring/Apache 提供的工具类，运行时通过**反射**实现对象拷贝。优点是简单易用，不需要额外配置；缺点是性能稍差（反射）、null 值会覆盖目标字段、类型不匹配静默跳过
> 2. **MapStruct**：编译期代码生成工具，通过注解在编译时生成 getter/setter 拷贝代码，运行时就是普通方法调用。优点是性能好（无反射）、类型安全、支持复杂映射；缺点是需要额外依赖和配置，学习成本稍高
> 3. **项目用的 `BeanUtils.copyProperties`**（Spring 的），因为项目规模不大、并发量不高，反射的性能差异可以忽略，而且 `BeanUtils` 简单直接，团队成员都熟悉
> 4. **如果是超高并发场景（如秒杀、大数据量导出）**，会考虑用 MapStruct，因为编译期生成的代码性能更好，而且可以避免 null 覆盖、类型不匹配等问题
> 5. **null 覆盖问题的处理**：项目里更新时用 `copyProperties(source, target, "stock")` 忽略特定字段，或者手动 set 非空字段，避免 null 覆盖有效值

### 面试题3：新增和更新为什么要用不同的 DTO？能不能用同一个？

> **答题要点**：
> 1. **校验规则不同**：新增时多个字段必填（如 isbn、title、author、stock），更新时所有字段可选（只更新传了的字段）。如果用同一个 DTO，新增时的 `@NotBlank` 注解在更新时也会生效，导致更新时必须传所有字段，不符合部分更新的需求
> 2. **字段不同**：新增时不需要 id（由数据库自增生成），更新时必须有 id（指定更新哪条记录）。如果用同一个 DTO，id 字段的校验规则不好定义
> 3. **语义清晰**：`BookAddDTO` 一看就知道是新增请求，`BookUpdateDTO` 一看就知道是更新请求，代码可读性好
> 4. **能不能用同一个？技术上可以**，用分组校验（`@NotBlank(groups = AddGroup.class)`）+ `@Validated(AddGroup.class)` 可以区分新增和更新的校验规则。但分组校验更复杂、可读性差，对于字段差异较大的场景，分开写更清晰
> 5. **项目里的做法**：`BookAddDTO` 和 `BookUpdateDTO` 分开，`BookAddDTO` 有 `@NotBlank` 等必填校验，`BookUpdateDTO` 所有字段可选，这是企业项目的常见做法

---

## 十三、下一课预告

**第8课：统一返回结果 + 全局异常处理——`Result`、`@RestControllerAdvice`、`@ExceptionHandler`**

我们会搞清楚：
- 为什么所有接口都返回 `Result<T>`？统一返回格式的好处是什么？
- `ResultCode` 枚举怎么设计？项目里有哪些状态码？
- `@RestControllerAdvice` 是什么？全局异常处理器的底层原理（AOP）
- `@ExceptionHandler` 怎么匹配异常？具体异常和通用异常的优先级
- 项目里处理了哪些异常？`BusinessException`、参数校验异常、系统异常分别怎么处理
- 为什么业务异常要继承 `RuntimeException`？受检异常和运行时异常的区别
- 全局异常处理和 `@Transactional` 回滚的关系
