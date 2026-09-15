# Stage 04：Service（业务层）

## 阶段目标

理解三层架构中 Service 的职责：编排 Mapper、写业务规则、抛业务异常、管事务（事务 stage12 再深入，本阶段先把分层跑通）。掌握 IService / ServiceImpl 的用法，以及为什么 Service 接口和实现分开。

## 前置要求

- stage03 毕业（Mapper 能用）

## 本阶段知识点

- IService<T> / ServiceImpl<M, T>
- 为什么 Service 要写接口
- Service 方法命名（save / get / list / page / update / remove）
- DTO → Entity → VO 的转换
- 业务异常 BizException（stage07 正式做，本阶段先用 RuntimeException 占位）
- 不要在 Controller 里写业务逻辑
- 不要在 Service 里返回 Entity（本阶段先提，stage06 系统做）

---

## 题目

### 【第 4-1 题：用户 Service 骨架】

**业务背景**
Mapper 能用了，现在把业务封装到 Service。

**我的任务**
1. 写 UserService 接口，继承 IService<User>
2. 写 UserServiceImpl 继承 ServiceImpl<UserMapper, User> 并实现 UserService
3. 加两个方法：
   - User getByUsername(String username)
   - boolean existsByUsername(String username)

**限制条件**
- 必须有接口和 Impl 两层（哪怕现在只有一个实现）
- 用 LambdaQueryWrapper 查
- 不能在 Controller 直接调 UserMapper

**验收标准**
- 方法能正常返回
- 你能说清 IService 给你提供了哪些"开箱即用"的方法（save / saveBatch / getById / list / page ...）
- 你能说清为什么 Service 要写接口

**思考题**
1. 为什么要面向接口编程？以后要换实现怎么办？
2. IService 和 BaseMapper 方法好像重复，区别在哪？
3. ServiceImpl 里的 this.baseMapper 是什么？
4. 一个 Service 里能不能注入另一个 Service？

---

### 【第 4-2 题：图书 Service】

**业务背景**
图书的增删改查业务规则比用户复杂。

**我的任务**
写 BookService：
1. addBook(Book book)：新增图书，ISBN 重复要抛异常
2. updateBook(Book book)：更新，不能改 ISBN（想清楚）
3. deleteBook(Long id)：逻辑删除
4. getBookDetail(Long id)：查详情，查不到要抛异常

**限制条件**
- 业务校验（ISBN 重复、书不存在）在 Service 做，不在 Controller
- 本阶段先 throw new RuntimeException，stage07 统一替换
- 返回 Entity 即可，stage06 再做 VO

**验收标准**
- ISBN 重复时业务层报错
- 查不存在的 id 时业务层报错
- Controller 里没有 if (book == null) 这种业务判断

**思考题**
1. 为什么 ISBN 重复校验要在 Service，不在数据库唯一索引？（提示：双保险）
2. 为什么 Controller 不写 if (xxx == null) throw?
3. Service 方法应该抛异常，还是返回 null？
4. 一个 Service 方法多长算"太长了"？

---

### 【第 4-3 题：分类 Service】

**业务背景**
分类有父子关系，删除时要检查下面有没有子分类或图书。

**我的任务**
写 CategoryService：
1. addCategory：同级下分类名不能重复
2. deleteCategory(id)：
   - 如果有子分类，禁止删
   - 如果下面还有书，禁止删（提示：调 bookMapper 统计）
3. listRootCategories：查一级分类
4. listChildren(parentId)：查二级

**限制条件**
- 禁止物理删
- 跨表校验要在 Service 层编排
- 先不做事务（但要意识到这里需要，stage12 回来补）

**验收标准**
- 有子分类时删不掉
- 有书挂在分类上时删不掉
- 你能说出这里为什么需要事务（删分类 + 校验书，中间崩了会怎样）

**思考题**
1. 先删分类还是先校验子分类？顺序重要吗？
2. 如果一个请求里校验通过了，但另一个请求同时塞了一本书进来，怎么办？（并发伏笔）
3. 树状分类查全量树，一次查出来在内存组，还是递归查数据库？

---

### 【第 4-4 题：Service 层的 DTO/VO 转换意识】

**业务背景**
从现在开始，Service 对外暴露的参数和返回值不要再直接用 Entity。

**我的任务**
1. 设计 BookQueryDTO（bookName / categoryId / author / page / size）
2. 设计 BookVO（id / name / author / categoryName / stock / createTime）
3. 写 PageVO<BookVO> pageQuery(BookQueryDTO dto)
4. Service 内部把 Entity 转 VO

**限制条件**
- Controller 层不能出现 Book Entity 的返回
- 字段拷贝用手工 setter 或 MapStruct（先手工，别一上来就 MapStruct）
- password 字段绝不能出现在 VO 里

**验收标准**
- 返回给前端的 JSON 里没有 password、没有 deleted、没有 createBy 之类
- 你能说清为什么不能直接返回 Entity
- 你能说出 Entity / DTO / VO 三者各自的职责

**思考题**
1. 手工 copy 字段 vs BeanUtils.copyProperties vs MapStruct，各有什么坑？
2. BeanUtils.copyProperties 是反射，性能怎么样？
3. VO 里要不要包含和 Entity 一模一样的所有字段？
4. DTO 是入参对象，那 GET 请求的查询参数算 DTO 吗？

---

## 本阶段毕业考试

不看任何资料：
1. 给你"分页查借阅记录"需求，写出 DTO、Service 方法、返回 VO
2. 让你现场解释 Service 接口存在的意义
3. 给你一段 Controller 里写满业务逻辑的代码，让你指出问题并重构

## 本阶段反向面试题

1. 三层架构为什么不能跨层调用？
2. Service 里应该写 SQL 吗？
3. 一个 Service 方法多长需要拆分？
4. 为什么要返回 VO 而不是 Entity？
5. IService.saveBatch 底层是什么 SQL？一条一条 insert 还是批量？

## 常见坑

- Controller 直接注入 Mapper（跨层）
- Service 里写大量 if-else 业务判断却不分方法
- 入参直接用 Entity
- 返回 Entity 把密码带出去
- 接口和实现职责混乱
