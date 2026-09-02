# 第6课：MyBatis-Plus 入门——`BaseMapper`、`ServiceImpl`、`LambdaQueryWrapper`

> **本课目标**：搞懂 MyBatis-Plus 是怎么让你"不写 SQL 就能 CRUD"的，掌握 `BaseMapper`、`ServiceImpl`、`LambdaQueryWrapper` 的用法。学完这课，你应该能独立用 MyBatis-Plus 写出单表的增删改查和动态条件查询。

---

## 一、从项目代码开始

看 `BookMapper.java`：

```java
@Mapper
public interface BookMapper extends BaseMapper<Book> {

    @Update("UPDATE book SET stock = stock - #{count} WHERE id = #{bookId} AND stock >= #{count} AND deleted = 0")
    int decreaseStock(@Param("bookId") Long bookId, @Param("count") Integer count);

    @Update("UPDATE book SET stock = stock + #{count} WHERE id = #{bookId} AND deleted = 0")
    int increaseStock(@Param("bookId") Long bookId, @Param("count") Integer count);
}
```

**问题**：这个接口只有两个自定义方法，但 `BookServiceImpl` 里调用了 `getById`、`save`、`updateById`、`removeById`、`page`、`count`、`list` 等一堆方法——这些方法从哪来的？

答案：**`BaseMapper<Book>` 提供了这些方法**。MyBatis-Plus 在运行时为这些方法自动生成 SQL，你不需要写。

---

## 二、MyBatis 和 MyBatis-Plus 的关系

### MyBatis 是什么？

MyBatis 是一个**半 ORM** 持久层框架。它需要你：
1. 写 Mapper 接口
2. 写 XML 文件或注解，定义 SQL 语句
3. MyBatis 帮你把 SQL 执行结果映射成 Java 对象

比如用 MyBatis 查询图书，你要写：
```java
public interface BookMapper {
    @Select("SELECT * FROM book WHERE id = #{id} AND deleted = 0")
    Book selectById(Long id);
}
```

每个方法都要自己写 SQL，单表 CRUD 非常重复。

### MyBatis-Plus 是什么？

MyBatis-Plus（简称 MP）是 MyBatis 的**增强工具**，在 MyBatis 的基础上只做增强不做改变。

它提供了：
- `BaseMapper`：单表 CRUD 的通用方法，自动生成 SQL，不用写
- `ServiceImpl`：Service 层的通用实现，封装了常用业务方法
- `LambdaQueryWrapper`：用 Lambda 表达式构建动态查询条件，不用写 XML
- 分页插件、逻辑删除、自动填充、主键策略等企业级功能

### 项目里的关系

```
MyBatis-Plus
├── 封装了 MyBatis（底层还是 MyBatis 执行 SQL）
├── 提供 BaseMapper（单表 CRUD 自动生成 SQL）
├── 提供 ServiceImpl（Service 层通用实现）
├── 提供 LambdaQueryWrapper（动态条件构建）
└── 你只需要写复杂的自定义 SQL（如多表 JOIN、子查询）
```

项目里 90% 的单表操作都用 MyBatis-Plus 的通用方法，只有 `decreaseStock`、`increaseStock` 这种需要原子操作的自定义 SQL 才自己写。

---

## 三、`BaseMapper`——单表 CRUD 的通用方法

`BookMapper extends BaseMapper<Book>`，泛型 `Book` 是对应的实体类。`BaseMapper` 提供了以下方法：

### 3.1 插入（Insert）

| 方法 | 生成的 SQL | 说明 |
|------|-----------|------|
| `insert(entity)` | `INSERT INTO book (字段...) VALUES (值...)` | 插入一条记录，null 字段不插入 |

项目里的用法：
```java
Book book = new Book();
book.setTitle("Java核心技术");
book.setAuthor("Cay S. Horstmann");
book.setStock(5);
bookMapper.insert(book);  // 自动生成 INSERT SQL
```

### 3.2 删除（Delete）

| 方法 | 生成的 SQL | 说明 |
|------|-----------|------|
| `deleteById(id)` | `UPDATE book SET deleted=1 WHERE id = ?`（逻辑删除） | 根据 ID 删除 |
| `deleteBatchIds(ids)` | `UPDATE book SET deleted=1 WHERE id IN (?,?)` | 批量删除 |
| `delete(wrapper)` | `UPDATE book SET deleted=1 WHERE 条件` | 按条件删除 |

注意：项目配置了**逻辑删除**（`logic-delete-field: deleted`），所以 `deleteById` 生成的不是 `DELETE`，而是 `UPDATE ... SET deleted=1`。查询时会自动加 `WHERE deleted=0`。

### 3.3 修改（Update）

| 方法 | 生成的 SQL | 说明 |
|------|-----------|------|
| `updateById(entity)` | `UPDATE book SET 字段=值 WHERE id = ?` | 根据 ID 更新，null 字段不更新 |
| `update(entity, wrapper)` | `UPDATE book SET 字段=值 WHERE 条件` | 按条件更新 |

项目里的用法：
```java
Book book = new Book();
book.setId(1L);
book.setTitle("新书名");
bookMapper.updateById(book);  // 只更新 title 字段，其他字段不变
```

### 3.4 查询（Select）

| 方法 | 生成的 SQL | 说明 |
|------|-----------|------|
| `selectById(id)` | `SELECT * FROM book WHERE id = ? AND deleted=0` | 根据 ID 查询 |
| `selectBatchIds(ids)` | `SELECT * FROM book WHERE id IN (?,?) AND deleted=0` | 批量查询 |
| `selectOne(wrapper)` | `SELECT * FROM book WHERE 条件 LIMIT 1` | 按条件查一条 |
| `selectList(wrapper)` | `SELECT * FROM book WHERE 条件` | 按条件查列表 |
| `selectCount(wrapper)` | `SELECT COUNT(*) FROM book WHERE 条件` | 统计数量 |
| `selectPage(page, wrapper)` | `SELECT * FROM book WHERE 条件 LIMIT ?,?` + COUNT | 分页查询 |
| `selectMaps(wrapper)` | `SELECT * FROM book WHERE 条件` | 返回 List<Map> |

项目里的用法：
```java
Book book = bookMapper.selectById(1L);  // 自动生成 SELECT SQL
Integer count = bookMapper.selectCount(wrapper);  // 自动生成 COUNT SQL
```

### 关键：泛型和实体类映射

`BaseMapper<Book>` 怎么知道 `Book` 对应哪张表、字段怎么映射？

通过 `Book` 实体类上的注解：

```java
@Data
@TableName("book")                    // ← 对应数据库表名
public class Book implements Serializable {

    @TableId(type = IdType.AUTO)     // ← 主键，自增
    private Long id;

    private String isbn;              // ← 普通字段，自动映射（驼峰转下划线）
    private String title;
    private String author;
    private Long categoryId;          // ← categoryId 映射到 category_id 字段
    private Integer stock;

    @TableField(fill = FieldFill.INSERT)   // ← 插入时自动填充
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)  // ← 插入和更新时自动填充
    private LocalDateTime updateTime;

    @TableLogic                          // ← 逻辑删除字段
    private Integer deleted;
}
```

MyBatis-Plus 通过这些注解，在运行时自动生成对应的 SQL。

---

## 四、`ServiceImpl`——Service 层的通用实现

看 `BookServiceImpl` 的类声明：

```java
@Service
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {
```

`ServiceImpl<BookMapper, Book>` 是 MyBatis-Plus 提供的 Service 层通用实现类。两个泛型：
- 第一个 `BookMapper`：对应的 Mapper 接口
- 第二个 `Book`：对应的实体类

继承 `ServiceImpl` 后，`BookServiceImpl` 自动拥有了以下方法（内部调用 `baseMapper` 的方法）：

| 方法 | 内部调用 | 说明 |
|------|---------|------|
| `getById(id)` | `baseMapper.selectById(id)` | 根据 ID 查询 |
| `getOne(wrapper)` | `baseMapper.selectOne(wrapper)` | 按条件查一条 |
| `list(wrapper)` | `baseMapper.selectList(wrapper)` | 按条件查列表 |
| `count(wrapper)` | `baseMapper.selectCount(wrapper)` | 统计数量 |
| `save(entity)` | `baseMapper.insert(entity)` | 新增 |
| `saveBatch(list)` | 批量 insert | 批量新增 |
| `updateById(entity)` | `baseMapper.updateById(entity)` | 根据 ID 更新 |
| `removeById(id)` | `baseMapper.deleteById(id)` | 根据 ID 删除 |
| `page(page, wrapper)` | `baseMapper.selectPage(page, wrapper)` | 分页查询 |
| `getBaseMapper()` | - | 获取底层的 Mapper |

项目里的用法：
```java
// BookServiceImpl 里直接调用父类方法
Book book = getById(1L);           // 等价于 baseMapper.selectById(1L)
boolean success = save(book);       // 等价于 baseMapper.insert(book) > 0
boolean updated = updateById(book); // 等价于 baseMapper.updateById(book) > 0
IPage<Book> page = page(pageObj, wrapper);  // 分页查询
```

### 为什么要继承 `ServiceImpl`？

1. **减少重复代码**：每个 Service 都要写的 `getById`、`save`、`updateById` 等，父类已经封装好了
2. **统一规范**：所有 Service 都有一致的方法名和行为
3. **可以直接用 `baseMapper`**：父类里已经注入了 `BookMapper`，子类里直接用 `baseMapper.xxx()`

### `BookService` 接口

```java
public interface BookService extends IService<Book> {
    // 自定义业务方法
    IPage<BookVO> pageBooks(BookQueryDTO queryDTO);
    BookVO getBookById(Long id);
    BookVO addBook(BookAddDTO addDTO);
    // ...
}
```

`BookService extends IService<Book>`，`IService` 是 `ServiceImpl` 实现的接口。这样 Controller 里注入的是 `BookService` 接口，既可以调用通用方法（`getById`、`save`），也可以调用自定义方法（`pageBooks`）。

---

## 五、`LambdaQueryWrapper`——动态查询条件构建

看 `BookServiceImpl` 里的 `buildQueryWrapper` 方法：

```java
private LambdaQueryWrapper<Book> buildQueryWrapper(BookQueryDTO queryDTO) {
    LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();

    // 书名模糊查询
    if (StringUtils.hasText(queryDTO.getTitle())) {
        wrapper.like(Book::getTitle, queryDTO.getTitle());
    }

    // 作者模糊查询
    if (StringUtils.hasText(queryDTO.getAuthor())) {
        wrapper.like(Book::getAuthor, queryDTO.getAuthor());
    }

    // ISBN 精确查询
    if (StringUtils.hasText(queryDTO.getIsbn())) {
        wrapper.eq(Book::getIsbn, queryDTO.getIsbn());
    }

    // 分类ID
    if (queryDTO.getCategoryId() != null) {
        wrapper.eq(Book::getCategoryId, queryDTO.getCategoryId());
    }

    // 只查询有库存的图书
    if (Boolean.TRUE.equals(queryDTO.getOnlyAvailable())) {
        wrapper.gt(Book::getStock, 0);
    }

    // 排序
    wrapper.orderByDesc(Book::getCreateTime);

    return wrapper;
}
```

### 什么是 `LambdaQueryWrapper`？

`LambdaQueryWrapper` 是 MyBatis-Plus 提供的**查询条件构建器**，用 **Lambda 表达式**引用实体类的字段，动态拼接 WHERE 条件。

传统 MyBatis 写动态 SQL 要在 XML 里用 `<if>` 标签：
```xml
<select id="selectBooks" resultType="Book">
    SELECT * FROM book WHERE deleted=0
    <if test="title != null and title != ''">
        AND title LIKE CONCAT('%', #{title}, '%')
    </if>
    <if test="author != null and author != ''">
        AND author LIKE CONCAT('%', #{author}, '%')
    </if>
</select>
```

用 `LambdaQueryWrapper` 直接在 Java 代码里构建，不用写 XML：
```java
LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
if (StringUtils.hasText(title)) {
    wrapper.like(Book::getTitle, title);  // 自动生成 AND title LIKE '%Java%'
}
```

### 常用条件方法

| 方法 | 生成的 SQL 条件 | 说明 |
|------|---------------|------|
| `eq(Book::getTitle, "Java")` | `title = 'Java'` | 等于 |
| `ne(Book::getTitle, "Java")` | `title <> 'Java'` | 不等于 |
| `gt(Book::getStock, 0)` | `stock > 0` | 大于 |
| `ge(Book::getStock, 0)` | `stock >= 0` | 大于等于 |
| `lt(Book::getPrice, 100)` | `price < 100` | 小于 |
| `le(Book::getPrice, 100)` | `price <= 100` | 小于等于 |
| `like(Book::getTitle, "Java")` | `title LIKE '%Java%'` | 模糊匹配（两边加 %） |
| `likeLeft(Book::getTitle, "Java")` | `title LIKE '%Java'` | 左模糊 |
| `likeRight(Book::getTitle, "Java")` | `title LIKE 'Java%'` | 右模糊 |
| `in(Book::getId, 1,2,3)` | `id IN (1,2,3)` | 在集合中 |
| `notIn(Book::getId, 1,2)` | `id NOT IN (1,2)` | 不在集合中 |
| `between(Book::getPrice, 50, 100)` | `price BETWEEN 50 AND 100` | 范围 |
| `isNull(Book::getDescription)` | `description IS NULL` | 为空 |
| `isNotNull(Book::getDescription)` | `description IS NOT NULL` | 不为空 |
| `orderByDesc(Book::getCreateTime)` | `ORDER BY create_time DESC` | 降序 |
| `orderByAsc(Book::getPrice)` | `ORDER BY price ASC` | 升序 |
| `last("LIMIT 10")` | 追加到 SQL 最后 | 自定义追加 |

### 条件拼接的逻辑

多个条件默认用 `AND` 连接：

```java
wrapper.eq(Book::getCategoryId, 1L)
       .gt(Book::getStock, 0)
       .like(Book::getTitle, "Java");
```

生成的 SQL：
```sql
WHERE category_id = 1 AND stock > 0 AND title LIKE '%Java%' AND deleted = 0
```

如果需要 `OR`：
```java
wrapper.eq(Book::getCategoryId, 1L)
       .or()
       .eq(Book::getCategoryId, 2L);
```
生成：`WHERE category_id = 1 OR category_id = 2`

### 为什么用 Lambda 表达式引用字段？

`Book::getTitle` 是 Java 8 的方法引用（Lambda 的一种）。MyBatis-Plus 通过反射解析这个方法引用，拿到对应的字段名 `title`，然后转换成数据库列名 `title`（驼峰转下划线）。

好处：
1. **编译期检查**：字段名写错了编译不通过，不会出现运行时 SQL 错误
2. **不用写字符串**：`wrapper.like("title", "Java")` 这种字符串写法容易拼错，而且重构字段名时不会自动更新
3. **类型安全**：MyBatis-Plus 知道 `Book::getTitle` 返回的是 String，做类型校验

---

## 六、分页查询——`IPage` 和分页插件

看 `BookServiceImpl` 的 `pageBooks` 方法：

```java
@Override
public IPage<BookVO> pageBooks(BookQueryDTO queryDTO) {
    // 1. 构建分页对象（当前页、每页条数）
    Page<Book> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

    // 2. 构建动态查询条件
    LambdaQueryWrapper<Book> wrapper = buildQueryWrapper(queryDTO);

    // 3. 执行分页查询
    IPage<Book> bookPage = page(page, wrapper);

    // 4. 转换为 VO（补充分类名称）
    return bookPage.convert(this::toVO);
}
```

### `Page` 对象

`Page<T>` 是 MyBatis-Plus 的分页对象，构造参数：
- 第一个：当前页码（从 1 开始）
- 第二个：每页条数

```java
Page<Book> page = new Page<>(1, 10);  // 第1页，每页10条
```

### `IPage` 接口

`page(page, wrapper)` 返回 `IPage<Book>`，包含分页结果：

| 方法/字段 | 含义 |
|----------|------|
| `getRecords()` | 当前页的数据列表（List<Book>） |
| `getTotal()` | 总记录数 |
| `getCurrent()` | 当前页码 |
| `getSize()` | 每页条数 |
| `getPages()` | 总页数 |

返回给前端的 JSON 大概是：
```json
{
  "records": [ { "id": 1, "title": "Java核心技术", ... }, ... ],
  "total": 50,
  "size": 10,
  "current": 1,
  "pages": 5
}
```

### 分页插件怎么配置的？

看 `MyBatisPlusConfig.java`：

```java
@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件，指定数据库类型为 MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

分页插件是一个 MyBatis 拦截器，它会在 SQL 执行前：
1. 先执行 `SELECT COUNT(*) FROM book WHERE 条件` 获取总记录数
2. 再自动给原 SQL 追加 `LIMIT offset, size` 实现分页

比如原 SQL 是 `SELECT * FROM book WHERE deleted=0`，分页插件会改成：
```sql
-- 先查总数
SELECT COUNT(*) FROM book WHERE deleted=0;
-- 再查当前页数据（第1页，每页10条 → LIMIT 0,10）
SELECT * FROM book WHERE deleted=0 LIMIT 0,10;
```

### `convert` 方法——结果转换

```java
return bookPage.convert(this::toVO);
```

`IPage.convert()` 是 MyBatis-Plus 提供的便捷方法，把分页结果里的每条记录用指定函数转换：
- `bookPage` 是 `IPage<Book>`
- `this::toVO` 是 `Book → BookVO` 的转换函数
- 返回 `IPage<BookVO>`

等价于：
```java
IPage<BookVO> voPage = new Page<>();
voPage.setRecords(bookPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
voPage.setTotal(bookPage.getTotal());
voPage.setCurrent(bookPage.getCurrent());
voPage.setSize(bookPage.getSize());
voPage.setPages(bookPage.getPages());
return voPage;
```

---

## 七、逻辑删除——`@TableLogic`

看 `Book` 实体类：

```java
@TableLogic
private Integer deleted;
```

`application.yml` 里的配置：
```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted      # 逻辑删除字段名
      logic-delete-value: 1            # 已删除的值
      logic-not-delete-value: 0        # 未删除的值
```

### 什么是逻辑删除？

逻辑删除不是真的 `DELETE` 数据，而是用一个字段标记"已删除"：
- 查询时自动加 `WHERE deleted = 0`（只查未删除的）
- 删除时执行 `UPDATE ... SET deleted = 1`（标记为已删除）
- 数据还在数据库里，可以恢复

### 对 SQL 的影响

| 操作 | 没有逻辑删除 | 有逻辑删除 |
|------|------------|-----------|
| `selectById(1)` | `SELECT * FROM book WHERE id=1` | `SELECT * FROM book WHERE id=1 AND deleted=0` |
| `deleteById(1)` | `DELETE FROM book WHERE id=1` | `UPDATE book SET deleted=1 WHERE id=1` |
| `selectCount()` | `SELECT COUNT(*) FROM book` | `SELECT COUNT(*) FROM book WHERE deleted=0` |

### 项目里的自定义 SQL 要注意

`BookMapper` 里的自定义 SQL 必须手动加 `deleted = 0`：

```java
@Update("UPDATE book SET stock = stock - #{count} WHERE id = #{bookId} AND stock >= #{count} AND deleted = 0")
int decreaseStock(@Param("bookId") Long bookId, @Param("count") Integer count);
```

因为 MyBatis-Plus 只对 `BaseMapper` 的通用方法自动加逻辑删除条件，**自定义 SQL 不会自动加**，必须手动写 `AND deleted = 0`。

### 为什么用逻辑删除？

1. **数据可恢复**：误删了可以恢复，不用找备份
2. **保留历史数据**：借阅记录关联了图书，如果物理删除图书，借阅记录就查不到图书信息了
3. **审计需求**：知道谁删了什么、什么时候删的
4. **避免物理删除的性能问题**：大表 DELETE 可能锁表、产生大量 binlog

---

## 八、自定义 SQL——`@Update`、`@Select`、`@Param`

项目里 `BookMapper` 有两个自定义方法：

```java
@Update("UPDATE book SET stock = stock - #{count} WHERE id = #{bookId} AND stock >= #{count} AND deleted = 0")
int decreaseStock(@Param("bookId") Long bookId, @Param("count") Integer count);
```

### 为什么需要自定义 SQL？

`BaseMapper` 的通用方法只能做简单的单表 CRUD。但有些操作需要特殊 SQL：
- **原子扣减库存**：`UPDATE book SET stock = stock - 1 WHERE id = ? AND stock >= 1`
  - 不能先查再改（并发下会超卖）
  - 必须用一条原子 SQL 完成"判断库存 + 扣减"
- **多表 JOIN 查询**：关联查询图书和分类名称
- **复杂子查询**：统计每个用户的借阅数量

### `@Param` 注解的作用

```java
int decreaseStock(@Param("bookId") Long bookId, @Param("count") Integer count);
```

`@Param("bookId")` 把方法参数 `bookId` 命名为 `bookId`，在 SQL 里用 `#{bookId}` 引用。

如果不写 `@Param`，MyBatis 默认用参数名（Java 8 编译加 `-parameters` 参数时）或 `arg0`、`arg1` 引用。写 `@Param` 更明确、更安全。

### `#{}` 和 `${}` 的区别

| 语法 | 原理 | 安全性 | 用途 |
|------|------|--------|------|
| `#{bookId}` | 预编译（PreparedStatement），参数用 `?` 占位 | **安全，防 SQL 注入** | 大多数情况，传参数值 |
| `${bookId}` | 字符串拼接，直接替换到 SQL 里 | **不安全，有 SQL 注入风险** | 动态表名、列名、ORDER BY 字段 |

项目里都用 `#{}`，这是正确的做法。

### 返回值 `int` 是什么？

`int decreaseStock(...)` 返回的是**受影响的行数**：
- 返回 1：扣减成功（找到了符合条件的记录并更新了）
- 返回 0：扣减失败（库存不足或图书不存在/已删除）

项目里就是通过判断 `affectedRows == 0` 来判断库存是否不足：
```java
int affectedRows = bookMapper.decreaseStock(bookId, 1);
if (affectedRows == 0) {
    throw new BusinessException(ResultCode.BOOK_OUT_OF_STOCK);
}
```

---

## 九、自动填充——`@TableField(fill = ...)`

看 `Book` 实体类：

```java
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createTime;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updateTime;
```

### 什么是自动填充？

`createTime` 和 `updateTime` 这两个字段，你不需要在代码里手动 `setCreateTime(now)`，MyBatis-Plus 会在插入/更新时自动填充。

看 `MyMetaObjectHandler.java`：

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 插入时自动填充 createTime 和 updateTime
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时自动填充 updateTime
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

### `FieldFill` 枚举

| 值 | 含义 |
|----|------|
| `FieldFill.DEFAULT` | 默认不填充 |
| `FieldFill.INSERT` | 插入时填充 |
| `FieldFill.UPDATE` | 更新时填充 |
| `FieldFill.INSERT_UPDATE` | 插入和更新时都填充 |

项目里：
- `createTime`：插入时填充（`INSERT`），更新时不变
- `updateTime`：插入和更新时都填充（`INSERT_UPDATE`），每次更新自动刷新

### 为什么用自动填充？

1. **减少重复代码**：每个实体的 `createTime`、`updateTime` 都要手动 set，很麻烦
2. **统一规范**：所有表的时间字段填充逻辑一致
3. **避免遗漏**：不会出现"忘了 setCreateTime 导致为 null"的 bug

---

## 十、本课必须记住的 7 件事

1. **MyBatis-Plus 是 MyBatis 的增强工具**，提供 `BaseMapper`（单表 CRUD 自动生成 SQL）、`ServiceImpl`（Service 层通用实现）、`LambdaQueryWrapper`（动态条件构建）
2. **`BaseMapper<Book>` 提供 17+ 个通用方法**：insert、deleteById、updateById、selectById、selectList、selectCount、selectPage 等，不用写 SQL
3. **`ServiceImpl<Mapper, Entity>` 封装了 Service 层通用方法**：getById、save、updateById、removeById、page、count、list 等，内部调用 baseMapper
4. **`LambdaQueryWrapper` 用 Lambda 表达式构建动态条件**：eq（等于）、like（模糊）、gt（大于）、in、orderByDesc 等，多个条件默认 AND 连接
5. **分页查询用 `Page` + `IPage`**：`new Page<>(pageNum, pageSize)` 构建分页对象，`page(page, wrapper)` 执行查询，返回包含 records、total、current、size、pages 的分页结果
6. **逻辑删除 `@TableLogic`**：删除时执行 UPDATE 标记 deleted=1，查询时自动加 WHERE deleted=0；自定义 SQL 必须手动加 deleted=0
7. **自定义 SQL 用 `@Select`/`@Update` 注解 + `@Param` 参数**：`#{}` 是预编译（防注入），`${}` 是字符串拼接（有注入风险）；返回 int 是受影响行数

---

## 十一、本节关键代码

```java
// Mapper 层：继承 BaseMapper，自动拥有单表 CRUD
@Mapper
public interface BookMapper extends BaseMapper<Book> {
    // 只有复杂操作才写自定义 SQL
    @Update("UPDATE book SET stock = stock - #{count} WHERE id = #{bookId} AND stock >= #{count} AND deleted = 0")
    int decreaseStock(@Param("bookId") Long bookId, @Param("count") Integer count);
}

// Service 层：继承 ServiceImpl，自动拥有通用业务方法
@Service
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    // 分页查询 + 动态条件
    @Override
    public IPage<BookVO> pageBooks(BookQueryDTO queryDTO) {
        Page<Book> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Book> wrapper = buildQueryWrapper(queryDTO);
        IPage<Book> bookPage = page(page, wrapper);  // 父类方法，内部调 baseMapper.selectPage
        return bookPage.convert(this::toVO);          // 结果转换
    }

    // 动态条件构建
    private LambdaQueryWrapper<Book> buildQueryWrapper(BookQueryDTO queryDTO) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getTitle())) {
            wrapper.like(Book::getTitle, queryDTO.getTitle());   // LIKE '%Java%'
        }
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Book::getCategoryId, queryDTO.getCategoryId());  // = 1
        }
        if (Boolean.TRUE.equals(queryDTO.getOnlyAvailable())) {
            wrapper.gt(Book::getStock, 0);   // > 0
        }
        wrapper.orderByDesc(Book::getCreateTime);  // ORDER BY create_time DESC
        return wrapper;
    }

    // 直接用父类方法
    @Override
    public BookVO getBookById(Long id) {
        Book book = getById(id);  // 等价于 baseMapper.selectById(id)
        // ...
    }
}

// 实体类：注解驱动的表映射
@Data
@TableName("book")                    // 表名
public class Book {
    @TableId(type = IdType.AUTO)     // 主键自增
    private Long id;

    private String title;             // 普通字段，自动驼峰转下划线

    @TableField(fill = FieldFill.INSERT)       // 插入时自动填充
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE) // 插入和更新时自动填充
    private LocalDateTime updateTime;

    @TableLogic                          // 逻辑删除字段
    private Integer deleted;
}
```

---

## 十二、本节练习

### 练习1：用 LambdaQueryWrapper 实现多条件查询

在 `BookService` 里加一个方法：`List<BookVO> searchBooks(String keyword, Long categoryId, Integer minStock)`，要求：
- keyword 不为空时，按书名 OR 作者模糊查询
- categoryId 不为空时，按分类精确查询
- minStock 不为空时，查询库存 >= minStock 的图书
- 按价格升序排序

> **提示**：OR 条件用 `wrapper.and(w -> w.like(...).or().like(...))`。

### 练习2：理解分页插件的 SQL

在 `application.yml` 里已经配置了 `log-impl: org.apache.ibatis.logging.stdout.StdOutImpl`，MyBatis 会把执行的 SQL 打印到控制台。

调用分页查询接口（`GET /api/books?pageNum=1&pageSize=5&title=Java`），观察控制台打印的 SQL：
- 是不是先执行了 `SELECT COUNT(*)`？
- 然后执行了 `SELECT ... LIMIT 0,5`？
- WHERE 条件里是不是自动加了 `deleted=0`？
- LIKE 条件是不是 `%Java%`？

### 练习3：验证逻辑删除

调用删除接口（`DELETE /api/books/1`），然后查数据库：
- 图书记录还在吗？（还在，逻辑删除）
- `deleted` 字段变成了什么？（变成 1）
- 再调用查询接口，还能查到这本书吗？（查不到，因为自动加了 deleted=0）

---

## 十三、自测题

### Q1：MyBatis 和 MyBatis-Plus 是什么关系？项目里为什么大部分 SQL 不用写？

<details>
<summary>点击查看答案</summary>

MyBatis-Plus 是 MyBatis 的增强工具，在 MyBatis 基础上只做增强不做改变，底层还是 MyBatis 执行 SQL。

项目里大部分 SQL 不用写，是因为：
1. Mapper 接口继承了 `BaseMapper<Book>`，MyBatis-Plus 提供了 insert、deleteById、updateById、selectById、selectList、selectCount、selectPage 等 17+ 个通用方法
2. 这些方法在运行时根据实体类上的注解（`@TableName`、`@TableId`、`@TableField`、`@TableLogic`）自动生成对应的 SQL
3. Service 层继承 `ServiceImpl<BookMapper, Book>`，又封装了 getById、save、updateById、page 等通用业务方法
4. 只有复杂操作（如原子扣减库存、多表 JOIN）才需要自己写自定义 SQL

</details>

### Q2：`BaseMapper` 提供了哪些常用方法？`selectById` 和 `selectOne` 有什么区别？

<details>
<summary>点击查看答案</summary>

`BaseMapper` 常用方法：
- 插入：`insert(entity)`
- 删除：`deleteById(id)`、`deleteBatchIds(ids)`、`delete(wrapper)`
- 修改：`updateById(entity)`、`update(entity, wrapper)`
- 查询：`selectById(id)`、`selectBatchIds(ids)`、`selectOne(wrapper)`、`selectList(wrapper)`、`selectCount(wrapper)`、`selectPage(page, wrapper)`、`selectMaps(wrapper)`

`selectById` 和 `selectOne` 的区别：
- `selectById(id)`：根据主键 ID 查询，参数是 ID 值，SQL 是 `WHERE id = ?`
- `selectOne(wrapper)`：根据查询条件查询一条，参数是 `Wrapper`（可以是任意条件），SQL 是 `WHERE 条件 LIMIT 1`。如果查询结果有多条会报错

</details>

### Q3：`LambdaQueryWrapper` 怎么用？`eq`、`like`、`gt`、`in`、`orderByDesc` 分别生成什么 SQL？

<details>
<summary>点击查看答案</summary>

`LambdaQueryWrapper` 是 MyBatis-Plus 的查询条件构建器，用 Lambda 表达式（`Book::getTitle`）引用实体字段，动态拼接 WHERE 条件。

常用方法生成的 SQL：
- `eq(Book::getTitle, "Java")` → `title = 'Java'`
- `like(Book::getTitle, "Java")` → `title LIKE '%Java%'`
- `gt(Book::getStock, 0)` → `stock > 0`
- `ge(Book::getStock, 0)` → `stock >= 0`
- `in(Book::getId, 1,2,3)` → `id IN (1,2,3)`
- `between(Book::getPrice, 50, 100)` → `price BETWEEN 50 AND 100`
- `orderByDesc(Book::getCreateTime)` → `ORDER BY create_time DESC`
- `orderByAsc(Book::getPrice)` → `ORDER BY price ASC`

多个条件默认用 AND 连接，需要 OR 时用 `.or()`。用 Lambda 引用字段的好处是编译期检查、类型安全、重构时自动更新。

</details>

### Q4：逻辑删除是怎么工作的？自定义 SQL 要注意什么？

<details>
<summary>点击查看答案</summary>

逻辑删除通过 `@TableLogic` 注解标记字段，配合 `application.yml` 里的配置：
- `logic-delete-field: deleted`：逻辑删除字段名
- `logic-delete-value: 1`：已删除的值
- `logic-not-delete-value: 0`：未删除的值

工作原理：
- 查询时（selectById、selectList 等通用方法），自动在 WHERE 里加 `deleted = 0`
- 删除时（deleteById 等），不执行 DELETE，而是执行 `UPDATE ... SET deleted = 1`
- 数据还在数据库里，可以恢复

自定义 SQL 要注意：MyBatis-Plus 只对 BaseMapper 的通用方法自动加逻辑删除条件，**自定义 SQL（@Select、@Update 注解里的 SQL）不会自动加**，必须手动写 `AND deleted = 0`。比如项目里的 `decreaseStock` 就手动加了 `AND deleted = 0`。

</details>

### Q5：分页查询是怎么实现的？`IPage` 对象包含哪些信息？

<details>
<summary>点击查看答案</summary>

分页查询通过 MyBatis-Plus 的分页插件（`PaginationInnerInterceptor`）实现，配置在 `MyBatisPlusConfig` 里。

使用方式：
1. 构建 `Page<T>` 对象：`new Page<>(pageNum, pageSize)`（当前页、每页条数）
2. 构建查询条件 `LambdaQueryWrapper`
3. 调用 `page(page, wrapper)` 方法（ServiceImpl 的方法，内部调 baseMapper.selectPage）
4. 返回 `IPage<T>` 分页结果

分页插件的工作原理：在 SQL 执行前，先执行 `SELECT COUNT(*)` 获取总记录数，再自动给原 SQL 追加 `LIMIT offset, size` 查询当前页数据。

`IPage` 包含的信息：
- `getRecords()`：当前页的数据列表
- `getTotal()`：总记录数
- `getCurrent()`：当前页码
- `getSize()`：每页条数
- `getPages()`：总页数

</details>

---

## 十四、面试题

### 面试题1：MyBatis-Plus 的 `BaseMapper` 和 `ServiceImpl` 分别提供了什么？为什么能不写 SQL 就实现 CRUD？

> **答题要点**：
> 1. `BaseMapper<T>` 是 MyBatis-Plus 提供的 Mapper 层通用接口，继承它后自动拥有 insert、deleteById、updateById、selectById、selectList、selectCount、selectPage 等 17+ 个单表 CRUD 方法
> 2. `ServiceImpl<Mapper, Entity>` 是 Service 层通用实现类，继承它后自动拥有 getById、getOne、list、save、saveBatch、updateById、removeById、page、count 等通用业务方法，内部封装了对 baseMapper 的调用
> 3. 能不写 SQL 就实现 CRUD 的原因：MyBatis-Plus 在运行时通过反射解析实体类上的注解（`@TableName` 表名、`@TableId` 主键、`@TableField` 字段映射、`@TableLogic` 逻辑删除），根据方法名和参数动态生成对应的 SQL 语句，然后交给 MyBatis 执行
> 4. 比如 `selectById(1L)` 会生成 `SELECT 字段列表 FROM 表名 WHERE id = 1 AND deleted = 0`（如果配置了逻辑删除）
> 5. 只有复杂操作（多表 JOIN、原子更新、子查询）才需要写自定义 SQL

### 面试题2：`LambdaQueryWrapper` 和传统 XML 动态 SQL 相比有什么优势？

> **答题要点**：
> 1. **不用写 XML**：直接在 Java 代码里构建查询条件，减少 XML 文件的维护成本
> 2. **编译期检查**：用 Lambda 表达式（`Book::getTitle`）引用字段，字段名写错了编译不通过；XML 里的字符串字段名写错了运行时才报错
> 3. **类型安全**：MyBatis-Plus 知道字段的类型，做参数类型校验
> 4. **重构友好**：重构字段名时，IDE 会自动更新 Lambda 引用；XML 里的字符串不会自动更新
> 5. **动态条件更直观**：用 Java 的 if 判断是否拼接条件，比 XML 的 `<if test="...">` 更易读、更易调试
> 6. **链式调用**：`wrapper.eq().like().gt().orderByDesc()` 链式调用，代码简洁
> 7. 缺点：特别复杂的多表 JOIN 查询、动态列名、动态排序等场景，XML 更灵活；简单单表查询用 LambdaQueryWrapper，复杂查询用 XML 或注解 SQL

### 面试题3：MyBatis-Plus 的分页插件是怎么实现的？

> **答题要点**：
> 1. 分页插件（`PaginationInnerInterceptor`）是一个 MyBatis 拦截器（`Interceptor`），在 SQL 执行前拦截
> 2. 配置方式：在 `MybatisPlusInterceptor` 里添加 `PaginationInnerInterceptor`，指定数据库类型（如 MySQL）
> 3. 执行流程：
>    - 调用 `selectPage(page, wrapper)` 时，MyBatis-Plus 把分页对象（当前页、每页条数）放到 ThreadLocal 里
>    - 分页插件拦截到 SQL 执行，先判断是否有分页参数
>    - 如果有，先执行 `SELECT COUNT(*) FROM (原SQL) tmp` 获取总记录数，设置到 IPage 的 total 字段
>    - 然后根据数据库类型生成分页 SQL，MySQL 是追加 `LIMIT offset, size`（offset = (current-1)*size）
>    - 执行分页后的 SQL，把结果设置到 IPage 的 records 字段
> 4. 不同数据库的分页语法不同：MySQL 用 `LIMIT`，Oracle 用 `ROWNUM`，SQL Server 用 `OFFSET FETCH`，插件根据 DbType 自动适配
> 5. `IPage` 返回结果包含：records（当前页数据）、total（总记录数）、current（当前页）、size（每页条数）、pages（总页数）

---

## 十五、下一课预告

**第7课：Entity / DTO / VO 三层对象设计——为什么要分这么多类？**

我们会搞清楚：
- 项目里为什么有 `Book`（Entity）、`BookAddDTO`（DTO）、`BookVO`（VO）三个类？分别用在哪一层？
- Entity 和数据库表怎么映射？为什么不能直接把 Entity 返回给前端？
- DTO 是什么？为什么接收前端参数要用 DTO 而不是 Entity？
- VO 是什么？为什么返回给前端要用 VO 而不是 Entity？
- `BeanUtils.copyProperties` 怎么做对象拷贝？有什么坑？
- 项目里的 `BookVO` 为什么比 `Book` 多了 `categoryName`、`available` 字段？
- 三层对象的设计原则和企业项目规范
