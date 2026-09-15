# Stage 03：Mapper（数据访问层）

## 阶段目标

掌握 MyBatis-Plus 的 BaseMapper、Wrapper、分页插件。本阶段只写 Mapper 层，不写 Service/Controller，先把"数据怎么查出来"练熟。

## 前置要求

- stage02 毕业（Entity 已写好）
- 知道什么是 SQL

## 本阶段知识点

- BaseMapper<T> 内置方法
- @Mapper 注解 / @MapperScan
- QueryWrapper vs LambdaQueryWrapper（推荐后者）
- UpdateWrapper / LambdaUpdateWrapper
- 分页插件 MybatisPlusInterceptor + PaginationInnerInterceptor
- IPage / Page
- 自定义 SQL：@Select 注解 vs XML
- 批量操作 saveBatch / insertBatch
- 逻辑删除自动生效原理

---

## 题目

### 【第 3-1 题：四个 Mapper 接口】

**业务背景**
Entity 有了，现在要能对数据库做增删改查。

**我的任务**
写出 UserMapper / CategoryMapper / BookMapper / BorrowRecordMapper 四个接口，都继承 BaseMapper<对应的 Entity>。

**限制条件**
- 在启动类或配置类上加 @MapperScan
- 先不写任何自定义方法
- 跑一个 main 方法或临时接口，证明 selectById 能查到一条数据（如果没数据就先 insert 一条）

**验收标准**
- 四个接口都注册成功
- 你能说清 BaseMapper 帮你提供了哪些方法（至少说出 6 个）
- 你能说出 @Mapper 和 @MapperScan 的区别

**思考题**
1. 为什么 BaseMapper 能不用写 SQL 就提供 CRUD？
2. 接口没有实现类，Spring 是怎么给你注入 Bean 的？（提示：动态代理）
3. Mapper 接口上要不要加 @Repository？为什么？
4. insert 和 insertOrIgnore / insertBatch 的区别？

---

### 【第 3-2 题：分页插件】

**业务背景**
图书列表肯定要分页。MyBatis-Plus 自带分页，但要配插件。

**我的任务**
1. 写一个配置类 MybatisPlusConfig
2. 注册 MybatisPlusInterceptor，加 PaginationInnerInterceptor
3. 写一个测试：new Page<>(1, 10) 调 bookMapper.selectPage(page, null)，把 total 和 records 打出来

**限制条件**
- 不配这个插件，分页会怎么样？（让用户自己试一次）
- 数据库类型选 MySQL

**验收标准**
- 分页 total 正确
- 控制台能看到 MyBatis-Plus 自动拼的 LIMIT
- 你能说清为什么需要这个插件（它替你做了什么）

**思考题**
1. 为什么不分页插件，selectPage 也能返回 records，但 total 不对？
2. 深分页（page=10000, size=10）为什么慢？
3. 分页插件底层是怎么改 SQL 的？
4. overflow 参数是干嘛的？

---

### 【第 3-3 题：条件查询（LambdaQueryWrapper）】

**业务背景**
图书列表要支持按书名模糊、按分类、按作者、按状态过滤。

**我的任务**
写一个 BookMapper 的测试方法（或临时 controller），用 LambdaQueryWrapper 实现：
1. 按书名模糊查（like）
2. 按分类 ID 精确查（eq）
3. 按作者模糊查
4. 只查在架的书（status = 在架）
5. 按创建时间倒序
6. 组合以上条件（条件不为空才拼）

**限制条件**
- 不许用 QueryWrapper（字符串字段名），必须用 LambdaQueryWrapper
- 必须用 `.condition(对象 != null, ...)` 这种条件式拼接，不要写 if-else 一大堆
- 不许 select *，只查需要的列（select 方法）

**验收标准**
- 传不同参数能正确过滤
- 空参数时不报错、不加条件
- 你能解释 LambdaQueryWrapper 比 QueryWrapper 好在哪
- 你能解释 select(Book::getId, Book::getName) 这种写法为什么安全

**思考题**
1. like "%xxx%" 为什么索引失效？
2. eq 和 = 的区别？
3. orderByDesc 和 last("order by ...") 的区别？
4. 什么时候必须写自定义 SQL，Wrapper 搞不定？

---

### 【第 3-4 题：UpdateWrapper 与部分更新】

**业务背景**
改图书库存时，只想更新 available_stock，不想把整本书的其他字段都带上。

**我的任务**
用 LambdaUpdateWrapper 实现：
1. 把某本书的 available_stock +1（借出去就 -1，还回来就 +1）
2. 批量把某个分类下的书状态改成下架

**限制条件**
- 不允许先 select 再 update（会有并发问题，这里先埋伏笔，后面 stage12 再深挖）
- 用 setSql("available_stock = available_stock - 1") 这种原子写法

**验收标准**
- 库存变更后数据库里数字正确
- 你能说清为什么不用"查出来 → Java 里 -1 → 更新回去"
- 你能说清 updateById 和 UpdateWrapper 的使用场景

**思考题**
1. setSql 有 SQL 注入风险吗？
2. update 时如果 where 条件不匹配，会发生什么？
3. 为什么逻辑删除字段在 update 时会自动带 deleted=0？

---

### 【第 3-5 题：自定义 SQL（XML / 注解）】

**业务背景**
复杂联表查询 Wrapper 写不干净。比如：查借阅记录时要把 book_name、user_name 一起带出来。

**我的任务**
写一个 BorrowRecordMapper 的自定义方法：
- 按用户 ID 查借阅记录，联表 book 带出书名
- 用 @Select 注解 或 XML 二选一

**限制条件**
- 不允许在 Service 里循环查 book（那是 N+1）
- 用 JOIN 一次查出来
- 返回结果可以用 Map 或专门写 VO，先不强制

**验收标准**
- 一条 SQL 把借阅记录 + 书名查出来
- EXPLAIN 看走了索引
- 你能说清什么时候用 Wrapper，什么时候用 XML

**思考题**
1. N+1 查询是什么？怎么发现？
2. JOIN 时 on 和 where 的区别？
3. 大表 JOIN 怎么优化？
4. MyBatis 的 #{} 和 ${} 区别？（必问）

---

## 本阶段毕业考试

不看任何资料：
1. 给一个新需求"按分类 ID 分页查图书，按书名模糊，按库存大于 0 过滤，按创建时间倒序"，10 分钟写出 Mapper 方法
2. 随机让你写三种 Wrapper 条件
3. 给一条慢 SQL，让你改成走索引的写法

## 本阶段反向面试题

1. MyBatis-Plus 的 BaseMapper 是怎么在启动时注册 Bean 的？
2. LambdaQueryWrapper 是怎么在编译期拿到字段名的？
3. 分页插件的原理（InnerInterceptor 接口）？
4. 逻辑删除是物理删还是改字段？SQL 长什么样？
5. MyBatis 的一级缓存、二级缓存是什么？为什么生产上经常关掉？

## 常见坑

- 忘了配分页插件 → total=0 或不分页
- 用 QueryWrapper 写字段名打错字，编译期不报错
- select * 导致大字段拖慢查询
- in() 传空集合 → SQL 语法错
- 自定义 SQL 里写 ${} → SQL 注入
