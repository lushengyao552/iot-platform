# 第18课：Spring AOP——面向切面编程、日志切面、权限切面

> **本课目标**：理解 AOP（面向切面编程）的核心概念和原理，学会在项目中使用 AOP 实现操作日志、权限校验等横切关注点。项目里目前没有 AOP 代码，这课会教你从零添加。

---

## 一、什么是 AOP？为什么需要 AOP？

### 问题场景

假设你想给项目里所有 Controller 方法都加上"操作日志"功能（记录谁在什么时候调用了什么接口、参数是什么、耗时多久）。

**不用 AOP 的做法**：在每个 Controller 方法里手写日志代码：

```java
@PostMapping
public Result<BookVO> addBook(@RequestBody @Valid BookAddDTO addDTO) {
    long startTime = System.currentTimeMillis();
    log.info("开始执行 addBook, 参数: {}", addDTO);
    try {
        BookVO bookVO = bookService.addBook(addDTO);
        log.info("addBook 执行成功, 耗时: {}ms", System.currentTimeMillis() - startTime);
        return Result.success(bookVO);
    } catch (Exception e) {
        log.error("addBook 执行失败, 耗时: {}ms, 错误: {}", System.currentTimeMillis() - startTime, e.getMessage());
        throw e;
    }
}
```

问题：
1. **代码重复**：每个方法都要写一遍，几十上百个方法就要写几十上百遍
2. **职责不清**：Controller 方法应该只关心业务逻辑，不应该混着日志代码
3. **难以维护**：如果要改日志格式（比如加个请求ID），要改所有方法
4. **侵入性强**：日志代码和业务代码混在一起，可读性差

### AOP 的解决方案

AOP（Aspect-Oriented Programming，面向切面编程）把这些**横切关注点**（日志、权限、事务、监控等）从业务代码中抽离出来，统一处理。

用 AOP 实现操作日志：

```java
@Aspect  // 切面
@Component
public class OperationLogAspect {

    // 切点：所有 Controller 方法
    @Pointcut("execution(* com.example.library.controller..*.*(..))")
    public void controllerPointcut() {}

    // 环绕通知：在方法执行前后都执行
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("[操作日志] 开始执行 {}, 参数: {}", methodName, args);

        try {
            Object result = joinPoint.proceed();  // 执行原方法
            log.info("[操作日志] {} 执行成功, 耗时: {}ms", methodName, System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("[操作日志] {} 执行失败, 耗时: {}ms, 错误: {}", methodName, System.currentTimeMillis() - startTime, e.getMessage());
            throw e;
        }
    }
}
```

这样，所有 Controller 方法都会自动被日志切面拦截，**不需要修改任何业务代码**。

### AOP 的核心思想

- **OOP（面向对象编程）**：用类和对象组织代码，通过继承、封装、多态复用代码
- **AOP（面向切面编程）**：用"切面"组织横切关注点，通过"切点"定义拦截哪些方法，通过"通知"定义拦截后做什么

AOP 不是替代 OOP，而是补充 OOP。OOP 解决业务逻辑的组织问题，AOP 解决横切关注点（日志、权限、事务等）的统一处理问题。

---

## 二、AOP 的核心概念

| 概念 | 英文 | 说明 | 类比 |
|------|------|------|------|
| **切面** | Aspect | 横切关注点的模块化（日志切面、权限切面） | 一个类，里面定义了"做什么"和"在哪里做" |
| **切点** | Pointcut | 定义拦截哪些方法（匹配规则） | "所有 Controller 方法"这个规则 |
| **通知** | Advice | 在方法的什么时机执行（前/后/异常/环绕） | 方法执行前打印日志，这就是"做什么" |
| **连接点** | JoinPoint | 被拦截的具体方法（程序执行的某个点） | 具体的 `addBook()` 方法 |
| **织入** | Weaving | 把切面代码插入到目标方法的过程 | 编译时/类加载时/运行时把日志代码插到 addBook 前后 |
| **目标对象** | Target | 被代理的原始对象 | BookController 的实例 |
| **代理对象** | Proxy | AOP 框架生成的代理对象，包含原方法+切面逻辑 | 包含日志功能的 BookController 代理 |

### 五种通知类型

| 注解 | 时机 | 用途 |
|------|------|------|
| `@Before` | 方法执行前 | 权限校验、参数校验、日志记录开始 |
| `@After` | 方法执行后（无论成功还是异常） | 资源释放、日志记录结束 |
| `@AfterReturning` | 方法正常返回后 | 记录返回值、缓存更新 |
| `@AfterThrowing` | 方法抛出异常后 | 异常日志、告警 |
| `@Around` | 环绕（方法执行前后都可以控制，甚至可以不执行原方法） | 最强大，事务、性能监控、缓存、日志都可以用 |

项目里的操作日志用 `@Around` 最方便（前后都要记录，还要算耗时）。

---

## 三、Spring AOP 的底层原理：动态代理

Spring AOP 是基于**动态代理**实现的。Spring 会在运行时为目标对象生成一个代理对象，代理对象包含原方法的逻辑 + 切面的逻辑。

### 两种动态代理

| 代理方式 | 实现 | 要求 | Spring 默认选择 |
|---------|------|------|----------------|
| **JDK 动态代理** | `java.lang.reflect.Proxy`，基于接口 | 目标类必须实现接口 | 目标类有接口时 |
| **CGLIB 代理** | 基于继承，生成目标类的子类 | 目标类不能是 final，方法不能是 final/static | 目标类没有接口时 |

Spring Boot 2.x 之后默认用 CGLIB 代理（`spring.aop.proxy-target-class=true`），不管有没有接口都用 CGLIB。

### 代理的执行流程

以 `BookController.addBook()` 被日志切面拦截为例：

```
调用方（前端请求）
    │
    ▼
代理对象（Spring 生成的 BookController$$EnhancerByCGLIB）
    │
    ├─ 1. 执行 @Around 通知的前半部分（记录开始时间、打印参数）
    │
    ├─ 2. 调用目标对象的 addBook() 方法（joinPoint.proceed()）
    │     │
    │     ▼
    │   原 BookController.addBook() 执行业务逻辑
    │     │
    │     ▼
    │   返回 Result<BookVO>
    │
    ├─ 3. 执行 @Around 通知的后半部分（记录耗时、打印结果）
    │
    ▼
返回给调用方
```

调用方不知道自己调用的是代理对象，以为是在调用原始的 BookController。这就是代理模式的透明性。

### 注意：AOP 不生效的场景

因为 AOP 是基于代理的，以下情况切面**不会生效**：

1. **同类内部方法调用**：A 方法调用同类的 B 方法，B 方法不会被代理拦截（因为是通过 this 调用，不是通过代理对象调用）
   ```java
   public void methodA() {
       this.methodB();  // methodB 不会被 AOP 拦截！
   }
   ```
2. **private 方法**：CGLIB 不能代理 private 方法（子类不能重写 private）
3. **final 类/final 方法**：CGLIB 不能继承 final 类，不能重写 final 方法
4. **static 方法**：静态方法属于类，不属于对象，不能被代理

项目里用 AOP 时要注意这些坑。

---

## 四、实战：给项目加操作日志切面

### 4.1 添加依赖

`pom.xml` 里已经有了（Spring Boot starter 里默认包含）：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 4.2 自定义注解（可选，更灵活）

先定义一个 `@OperationLog` 注解，标记需要记录日志的方法：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    String value() default "";        // 操作描述
    String module() default "";       // 模块
}
```

在 Controller 方法上使用：
```java
@OperationLog(module = "图书管理", value = "新增图书")
@PostMapping
public Result<BookVO> addBook(@RequestBody @Valid BookAddDTO addDTO) { ... }
```

### 4.3 日志切面

```java
@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    // 切点1：所有带 @OperationLog 注解的方法
    @Pointcut("@annotation(com.example.library.annotation.OperationLog)")
    public void operationLogPointcut() {}

    // 切点2：所有 Controller 包下的方法（如果不想用注解，可以用这个）
    @Pointcut("execution(* com.example.library.controller..*.*(..))")
    public void controllerPointcut() {}

    // 环绕通知：拦截带 @OperationLog 注解的方法
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        // 获取注解信息
        OperationLog annotation = method.getAnnotation(OperationLog.class);
        String module = annotation.module();
        String operation = annotation.value();

        // 获取参数
        Object[] args = joinPoint.getArgs();
        String params = args.length > 0 ? toJson(args[0]) : "无";

        // 获取当前用户
        String username = "匿名用户";
        try {
            username = UserContext.getCurrentUsername();
        } catch (Exception ignored) {}

        log.info("========== 操作日志开始 ==========");
        log.info("模块: {}, 操作: {}", module, operation);
        log.info("方法: {}.{}", className, methodName);
        log.info("操作人: {}", username);
        log.info("请求参数: {}", params);
        log.info("开始时间: {}", LocalDateTime.now());

        try {
            Object result = joinPoint.proceed();  // 执行原方法

            long cost = System.currentTimeMillis() - startTime;
            log.info("执行结果: 成功, 耗时: {}ms", cost);
            log.info("========== 操作日志结束 ==========");
            return result;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("执行结果: 失败, 耗时: {}ms, 异常: {}", cost, e.getMessage());
            log.info("========== 操作日志结束 ==========");
            throw e;
        }
    }

    // 对象转 JSON（用于打印参数）
    private String toJson(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
```

### 4.4 使用

在需要记录日志的 Controller 方法上加 `@OperationLog`：

```java
@OperationLog(module = "图书管理", value = "新增图书")
@PostMapping
public Result<BookVO> addBook(@RequestBody @Valid BookAddDTO addDTO) {
    BookVO bookVO = bookService.addBook(addDTO);
    return Result.success(bookVO);
}

@OperationLog(module = "图书管理", value = "删除图书")
@DeleteMapping("/{id}")
public Result<Void> deleteBook(@PathVariable Long id) {
    bookService.deleteBook(id);
    return Result.success();
}
```

调用接口后，控制台会打印：
```
========== 操作日志开始 ==========
模块: 图书管理, 操作: 新增图书
方法: BookController.addBook
操作人: admin
请求参数: {"title":"新书","author":"作者","stock":10}
开始时间: 2026-09-02T10:00:00
执行结果: 成功, 耗时: 45ms
========== 操作日志结束 ==========
```

---

## 五、实战：用 AOP 实现权限校验

项目里目前是在 Service 层手动判断权限（如删除评论时判断是否是作者或管理员）。对于简单的角色权限校验，可以用 AOP 统一处理。

### 5.1 自定义权限注解

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();  // 需要的角色，如 {"ADMIN"}
}
```

### 5.2 权限切面

```java
@Aspect
@Component
public class PermissionAspect {

    // 拦截带 @RequireRole 注解的方法
    @Around("@annotation(requireRole)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        // 获取当前用户角色
        String currentRole;
        try {
            currentRole = UserContext.getCurrentUserRole();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 校验角色
        String[] requiredRoles = requireRole.value();
        boolean hasPermission = Arrays.asList(requiredRoles).contains(currentRole);
        if (!hasPermission) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }

        // 有权限，执行原方法
        return joinPoint.proceed();
    }
}
```

### 5.3 使用

```java
@RequireRole("ADMIN")  // 只有管理员可以删除图书
@DeleteMapping("/{id}")
public Result<Void> deleteBook(@PathVariable Long id) {
    bookService.deleteBook(id);
    return Result.success();
}
```

这样，普通用户调用删除接口时，AOP 切面会在方法执行前拦截，抛出"没有权限"异常，不需要在 Service 里手动判断。

注意：这种方式适合简单的角色校验（如只有管理员能操作）。复杂的权限校验（如"只能删除自己的评论"）还是需要在 Service 层手动判断，因为需要知道资源的归属。

---

## 六、AOP 的其他应用场景

| 场景 | 说明 | 项目里的应用 |
|------|------|-------------|
| **事务管理** | `@Transactional` 就是 AOP 实现的（Spring 内置） | 第5课讲的借阅方法加了 @Transactional |
| **缓存** | `@Cacheable`、`@CacheEvict`（Spring Cache） | 项目里手动用 RedisService，没用注解缓存 |
| **日志** | 操作日志、访问日志、异常日志 | 本课实现的 OperationLogAspect |
| **权限** | 角色权限、资源权限校验 | 本课实现的 PermissionAspect |
| **性能监控** | 记录方法执行耗时，慢查询告警 | 可以在日志切面里加耗时统计 |
| **重试** | 方法失败自动重试 | 可以用 @Around 实现简单重试 |
| **限流** | 限制方法调用频率 | 可以用 Redis + AOP 实现限流 |
| **审计** | 记录谁在什么时候修改了什么数据 | 操作日志的进阶版 |

Spring 里很多功能都是基于 AOP 实现的，理解 AOP 有助于理解 Spring 的底层机制。

---

## 七、本课必须记住的 7 件事

1. **AOP 解决什么问题**：把横切关注点（日志、权限、事务、监控）从业务代码中抽离，统一处理，避免代码重复和侵入
2. **核心概念**：切面（Aspect）、切点（Pointcut，定义拦截哪些方法）、通知（Advice，定义做什么、什么时机做）、连接点（JoinPoint，被拦截的方法）、织入（Weaving，把切面插入目标方法）
3. **五种通知**：@Before（前）、@After（后）、@AfterReturning（正常返回后）、@AfterThrowing（异常后）、@Around（环绕，最强大）
4. **底层原理**：动态代理（JDK 代理基于接口、CGLIB 代理基于继承），Spring Boot 默认用 CGLIB。代理对象包含原方法+切面逻辑
5. **AOP 不生效的坑**：同类内部方法调用（this 调用不走代理）、private/final/static 方法不能被代理
6. **切点表达式**：`execution(* com.example.library.controller..*.*(..))`（execution 语法）、`@annotation(xxx)`（注解匹配）
7. **实际应用**：操作日志（@Around 记录参数、耗时、结果）、权限校验（@Before 校验角色）、事务（@Transactional，Spring 内置）

---

## 八、本节练习

### 练习1：给项目加操作日志切面

按照本课代码，在项目里实现 OperationLogAspect：
1. 创建 @OperationLog 注解
2. 创建 OperationLogAspect 切面（@Around 记录日志）
3. 在图书管理的 Controller 方法上加 @OperationLog 注解
4. 调用接口，观察控制台是否打印操作日志

### 练习2：实现方法耗时监控

在操作日志切面基础上，增加慢方法告警：
1. 如果方法执行耗时超过 100ms，用 `log.warn` 打印告警
2. 统计每个方法的平均耗时（可以用一个 Map 存储）
3. 提供一个接口查询慢方法排行榜

> **提示**：用 `ConcurrentHashMap<String, List<Long>>` 存储每个方法的耗时记录，定期计算平均值。

### 练习3：理解 AOP 不生效的坑

在 BookController 里加一个方法 A，A 内部调用方法 B（this.methodB()），两个方法都加 @OperationLog 注解：
1. 直接调用方法 B，观察是否有日志（应该有）
2. 调用方法 A，观察方法 B 是否有日志（应该没有，因为 this 调用不走代理）
3. 理解为什么同类内部调用 AOP 不生效

---

## 九、自测题

### Q1：什么是 AOP？和 OOP 有什么区别？

<details>
<summary>点击查看答案</summary>

**AOP（Aspect-Oriented Programming，面向切面编程）**是一种编程范式，它将"横切关注点"（如日志、权限、事务、监控等与核心业务逻辑无关但多处使用的功能）从业务代码中抽离出来，通过"切面"统一处理，避免代码重复和侵入。

**和 OOP 的区别**：
- **OOP（面向对象编程）**：用类和对象组织代码，通过继承、封装、多态实现复用。关注的是业务逻辑的纵向组织（类→方法→业务流程）。
- **AOP（面向切面编程）**：用切面组织横切关注点，通过切点定义拦截位置、通知定义处理逻辑。关注的是横向的通用功能（多个类/方法都需要的日志、权限等）。

**关系**：AOP 不是替代 OOP，而是补充 OOP。OOP 解决业务逻辑的组织问题，AOP 解决横切关注点的统一处理问题。两者结合使用。

**举例**：
- OOP：BookController 类里有 addBook、updateBook、deleteBook 方法，每个方法处理图书的业务逻辑
- AOP：一个日志切面拦截所有 Controller 方法，自动记录操作日志，不需要在每个方法里写日志代码

</details>

### Q2：AOP 的核心概念有哪些？五种通知类型分别是什么？

<details>
<summary>点击查看答案</summary>

**核心概念**：
1. **切面（Aspect）**：横切关注点的模块化，一个类里定义了"做什么"（通知）和"在哪里做"（切点）
2. **切点（Pointcut）**：定义拦截哪些方法的匹配规则（如 execution 表达式、注解匹配）
3. **通知（Advice）**：在方法的什么时机执行什么逻辑
4. **连接点（JoinPoint）**：被拦截的具体方法（程序执行的某个点）
5. **织入（Weaving）**：把切面代码插入到目标方法的过程（编译时/类加载时/运行时）
6. **目标对象（Target）**：被代理的原始对象
7. **代理对象（Proxy）**：AOP 框架生成的代理对象，包含原方法+切面逻辑

**五种通知类型**：
1. **@Before**：方法执行前执行。用于权限校验、参数校验、记录开始
2. **@After**：方法执行后执行（无论成功还是异常）。用于资源释放、记录结束
3. **@AfterReturning**：方法正常返回后执行。用于记录返回值、更新缓存
4. **@AfterThrowing**：方法抛出异常后执行。用于异常日志、告警
5. **@Around**：环绕通知，方法执行前后都可以控制，甚至可以不执行原方法（joinPoint.proceed()）。最强大，用于事务、性能监控、缓存、日志

项目里操作日志用 @Around（需要记录开始时间、参数、结果、耗时，前后都要处理）。

</details>

### Q3：Spring AOP 的底层原理是什么？JDK 代理和 CGLIB 代理有什么区别？

<details>
<summary>点击查看答案</summary>

**底层原理**：Spring AOP 基于**动态代理**实现。Spring 在运行时为目标对象生成一个代理对象，代理对象在调用原方法的前后插入切面逻辑。调用方调用的是代理对象，不知道原始对象的存在。

**两种动态代理**：

| 特性 | JDK 动态代理 | CGLIB 代理 |
|------|-------------|-----------|
| 实现 | java.lang.reflect.Proxy，基于接口 | 基于继承，生成目标类的子类 |
| 要求 | 目标类必须实现接口 | 目标类不能是 final，方法不能是 final/static |
| 性能 | 生成代理快，执行稍慢 | 生成代理慢，执行快（用了 FastClass） |
| Spring 默认 | 目标类有接口时使用 | 目标类没有接口时使用（Spring Boot 2.x+ 默认全用 CGLIB） |

**Spring Boot 默认配置**：`spring.aop.proxy-target-class=true`，默认用 CGLIB 代理，不管目标类有没有接口。可以改成 false 让有接口的类用 JDK 代理。

**代理执行流程**：
1. 调用方调用代理对象的方法
2. 代理对象执行 @Around 通知的前半部分（如记录开始时间）
3. 调用目标对象的原方法（joinPoint.proceed()）
4. 原方法执行业务逻辑，返回结果
5. 代理对象执行 @Around 通知的后半部分（如记录耗时）
6. 返回结果给调用方

</details>

### Q4：什么情况下 AOP 切面不会生效？怎么解决？

<details>
<summary>点击查看答案</summary>

**AOP 不生效的场景**（因为 AOP 基于代理，只有通过代理对象调用才会被拦截）：

1. **同类内部方法调用**：A 方法通过 `this` 调用同类的 B 方法，B 方法不会被拦截。因为 this 是原始对象，不是代理对象。
   ```java
   public void methodA() {
       this.methodB();  // methodB 不会被 AOP 拦截！
   }
   ```
   **解决**：
   - 把 methodB 放到另一个类里
   - 注入自己的代理对象：`@Autowired private BookController self; self.methodB();`
   - 用 AopContext.currentProxy() 获取当前代理对象（需要开启 exposeProxy=true）

2. **private 方法**：CGLIB 基于继承，子类不能重写 private 方法，所以不能代理。
   **解决**：把方法改成 public 或 protected。

3. **final 类/final 方法**：CGLIB 不能继承 final 类，不能重写 final 方法。
   **解决**：去掉 final 修饰符。

4. **static 方法**：静态方法属于类，不属于对象，不能被代理。
   **解决**：改成实例方法。

5. **对象不是 Spring 管理的**：如果对象是自己 new 出来的（`new BookController()`），不是 Spring 容器管理的 Bean，Spring 不会为它生成代理。
   **解决**：把对象交给 Spring 管理（@Component、@Service 等），通过依赖注入使用。

项目里用 AOP 时要特别注意"同类内部调用"这个坑，这是最常见的 AOP 不生效原因。

</details>

### Q5：切点表达式有哪些写法？项目里的操作日志切面用了哪种？

<details>
<summary>点击查看答案</summary>

**常见切点表达式写法**：

1. **execution 表达式**（最常用）：匹配方法签名
   ```
   execution(返回类型 包名.类名.方法名(参数))
   ```
   - `execution(* com.example.library.controller..*.*(..))`：controller 包下所有类的所有方法
   - `execution(public * com.example.service.*.*(..))`：service 包下所有 public 方法
   - `execution(* add*(..))`：所有以 add 开头的方法
   通配符：`*` 匹配任意字符，`..` 匹配任意层级包/任意参数

2. **@annotation 注解匹配**：匹配带指定注解的方法
   ```
   @annotation(com.example.library.annotation.OperationLog)
   ```
   匹配所有带 @OperationLog 注解的方法

3. **within 类型匹配**：匹配指定类型下的所有方法
   ```
   within(com.example.library.controller.*)  // controller 包下所有类
   within(com.example.library.controller..*)  // controller 包及子包
   ```

4. **bean 名称匹配**：匹配指定名称的 Bean
   ```
   bean(*Controller)  // 所有以 Controller 结尾的 Bean
   ```

5. **逻辑组合**：用 &&、||、! 组合多个切点
   ```
   execution(* com.example.controller..*.*(..)) && @annotation(OperationLog)
   ```

**项目里的操作日志切面用了 @annotation 匹配**：
```java
@Pointcut("@annotation(com.example.library.annotation.OperationLog)")
public void operationLogPointcut() {}
```
只有加了 @OperationLog 注解的方法才会被拦截，更灵活、更可控（不需要记录日志的方法不加注解就行）。

也可以用 execution 匹配所有 Controller 方法（`execution(* com.example.library.controller..*.*(..))`），这样所有 Controller 方法都自动记录日志，不需要每个方法加注解，但可能记录太多不需要的日志。

</details>

---

## 十、面试题

### 面试题1：什么是 AOP？Spring AOP 的实现原理？

> **答题要点**：
> 1. **AOP 定义**：面向切面编程，将横切关注点（日志、权限、事务、监控等）从业务代码中抽离，通过切面统一处理，避免代码重复和侵入。是 OOP 的补充。
> 2. **核心概念**：
>    - 切面（Aspect）：横切关注点的模块化（日志切面类）
>    - 切点（Pointcut）：定义拦截哪些方法（execution 表达式、注解匹配）
>    - 通知（Advice）：在什么时机做什么（@Before/@After/@Around 等）
>    - 连接点（JoinPoint）：被拦截的具体方法
>    - 织入（Weaving）：把切面插入目标方法的过程
> 3. **实现原理**：基于动态代理，运行时生成代理对象，代理对象包含原方法+切面逻辑
>    - JDK 动态代理：基于接口，目标类必须实现接口
>    - CGLIB 代理：基于继承，生成目标类的子类，Spring Boot 默认用 CGLIB
> 4. **五种通知**：@Before（前）、@After（后）、@AfterReturning（返回后）、@AfterThrowing（异常后）、@Around（环绕，最强大）
> 5. **应用场景**：事务（@Transactional）、日志、权限、缓存、性能监控、限流、重试
> 6. **注意坑**：同类内部方法调用不生效（this 不走代理）、private/final/static 方法不能代理、非 Spring 管理的对象不代理

### 面试题2：JDK 动态代理和 CGLIB 代理的区别？Spring 怎么选择？

> **答题要点**：
> 1. **JDK 动态代理**：
>    - 基于 `java.lang.reflect.Proxy` 和 `InvocationHandler`
>    - 要求目标类必须实现接口，代理对象实现相同接口
>    - 生成代理快，执行时通过反射调用，稍慢
> 2. **CGLIB 代理**：
>    - 基于 ASM 字节码框架，生成目标类的子类，重写目标方法
>    - 不要求接口，但目标类不能是 final，方法不能是 final/static/private
>    - 生成代理慢（需要生成字节码），执行快（用 FastClass 机制，避免反射）
> 3. **Spring 的选择**：
>    - Spring 5.x / Spring Boot 2.x 之前：目标类有接口用 JDK，没有接口用 CGLIB
>    - Spring Boot 2.x+：默认 `spring.aop.proxy-target-class=true`，统一用 CGLIB（不管有没有接口）
>    - 可以通过 `spring.aop.proxy-target-class=false` 改成 JDK 优先
> 4. **实际影响**：
>    - 用 CGLIB 时，@Transactional 等 AOP 功能在没有接口的类上也能用
>    - CGLIB 不能代理 final 类和 final 方法，所以 Service 类不要加 final
>    - 同类内部方法调用 AOP 不生效（不管 JDK 还是 CGLIB），因为 this 是原始对象不是代理
> 5. **性能对比**：CGLIB 执行性能比 JDK 代理高约 10%，但生成代理慢。单例 Bean 只生成一次代理，所以执行性能更重要，CGLIB 更优。

---

## 十一、下一课预告

**第19课：Docker 部署——Dockerfile、docker-compose、容器化部署全流程**

我们会搞清楚：
- 什么是 Docker？为什么要用容器化部署？
- 核心概念：镜像、容器、Dockerfile、docker-compose
- 项目里的 docker-compose.yml 详解（MySQL、Redis、RabbitMQ 三个服务）
- 怎么写后端应用的 Dockerfile？
- 怎么写前端的 Dockerfile（Nginx 部署）？
- 完整的 docker-compose 部署（后端+前端+MySQL+Redis+RabbitMQ）
- 数据卷、网络、环境变量、健康检查
- 常用 Docker 命令
