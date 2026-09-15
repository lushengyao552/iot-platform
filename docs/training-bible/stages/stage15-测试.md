# Stage 15：测试

## 阶段目标

用 JUnit 5 + Mockito 写单元测试，至少覆盖核心 Service。理解 Mock、Stub、断言、测试金字塔、什么时候测什么。

## 前置要求

- stage12 毕业
- 项目已有 Spring Boot Test 依赖

## 本阶段知识点

- JUnit 5：@Test / @BeforeEach / @DisplayName / Assertions
- Mockito：@Mock / @InjectMocks / when().thenReturn() / verify()
- 单元测试 vs 集成测试
- 测试金字塔
- 测试不连数据库（Mock Mapper）
- 边界条件、异常分支、正常分支
- 覆盖率（JaCoCo，了解即可）

---

## 题目

### 【第 15-1 题：Service 单元测试（纯 Mock）】

**业务背景**
之前改代码都靠手测。现在写自动化测试。

**我的任务**
给 BookService 写单测：
1. testAddBook_IsbnDuplicate_ThrowException
2. testAddBook_Success
3. testGetBook_NotFound_ThrowException
4. testDeleteBook_HasChildren_ThrowException

**限制条件**
- 不启动 Spring 容器（纯 JUnit + Mockito）
- BookMapper 用 @Mock mock 掉
- BookService 用 @InjectMocks
- 测试方法名要表达"场景_预期"

**验收标准**
- 4 个测试方法全绿
- 故意把业务改错（比如 ISBN 不查重），测试变红
- 你能说清单元测试为什么不连数据库
- 你能说清 @Mock 和 @InjectMocks 区别

**思考题**
1. 为什么单元测试要把 Mapper mock 掉？
2. when().thenReturn() 在干什么？
3. 测试覆盖率 100% 就够了吗？
4. 一个测试方法测多个断言可以吗？

---

### 【第 15-2 题：异常分支测试】

**业务背景**
正常分支好测，异常分支容易漏。

**我的任务**
1. 用 assertThrows 测业务异常
2. 测借书失败的几个分支：
   - 书不存在
   - 库存为 0
   - 重复借
3. 测还书失败：
   - 记录不存在
   - 不是本人
   - 已还过

**限制条件**
- 每个异常分支至少一个测试
- verify() 检查某方法是否被调用、调用几次

**验收标准**
- 异常断言通过
- 你能说清 verify(bookMapper, times(1)).updateById(any()) 的作用
- 你能说出为什么异常分支必须测

**思考题**
1. assertThrows 怎么拿到异常对象并断言其 code？
2. 怎么测"事务回滚"？
3. Mock 的方法真的执行了吗？

---

### 【第 15-3 题：集成测试（Spring Boot Test）】

**业务背景**
纯 Mock 测试覆盖不到和数据库的交互。写少量集成测试。

**我的任务**
1. @SpringBootTest
2. 用 H2 或测试用 MySQL 库
3. 测 UserService.register：
   - 先 insert 一个用户，验证用户名重复报错
   - 验证密码被 BCrypt 加密
4. 用 @Transactional 让测试跑完自动回滚

**限制条件**
- 集成测试要少而精，只测关键路径
- 不要把所有接口都写成集成测试
- 测试完自动回滚，不要污染数据库

**验收标准**
- 测试在真数据库（或 H2）跑通
- 跑完数据库不留脏数据
- 你能说清单测和集成测的比例

**思考题**
1. 为什么 @SpringBootTest 启动慢？
2. MockMvc 是什么？怎么测 Controller？
3. 测试数据库和开发数据库怎么隔离？
4. 什么时候用 Testcontainers？

---

### 【第 15-4 题：测试思维】

**业务背景**
写测试不是为了凑覆盖率，是为了改代码不慌。

**我的任务**
1. 选一个你之前写过的需求变更（比如"一个用户最多借 5 本"）
2. 先写测试（最多借 5 本成功、第 6 本失败）
3. 再去改代码让测试过
4. 体会 TDD 红-绿-重构

**限制条件**
- 这次不强求 TDD，但要体会
- 测试要能在你改坏代码时立刻报警

**验收标准**
- 你能现场说出"为什么重构要先有测试"
- 你能区分单元测试、集成测试、端到端测试
- 你能说出哪些代码值得测，哪些不值得

**思考题**
1. 为什么不追求 100% 覆盖率？
2. 测试代码也要维护，怎么平衡？
3. 前端测试和后端测试有什么不同？
4. 回归测试怎么做？

---

## 本阶段毕业考试

不看任何资料：
1. 给你一个 BorrowService.borrowBook 方法，让你现场写 5 个测试用例
2. 解释 Mock 是什么、为什么不连数据库
3. 让你把一个测试从"全 mock"改成"集成测试"

## 本阶段反向面试题

1. TDD 是什么？
2. BDD 是什么？
3. 测试金字塔？
4. Mock 和 Stub 区别？
5. 为什么 private 方法不好测？
6. 测试驱动开发的好处和成本？

## 常见坑

- 测了 Controller 没测 Service 业务
- Mock 了所有东西，等于没测
- 集成测试不回滚，污染数据库
- 一个测试方法几百行，测了所有事
- 断言太弱（只 assertNotNull）
