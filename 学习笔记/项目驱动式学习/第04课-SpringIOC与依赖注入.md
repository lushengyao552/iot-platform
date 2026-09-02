# 第4课：Spring IOC/DI 核心——`@Service`、Bean 生命周期、依赖注入原理

> **本课目标**：理解 Spring 的核心思想——控制反转（IOC）和依赖注入（DI），搞懂 Bean 是什么、Spring 容器怎么管理对象、依赖注入的底层原理。学完这课，你应该能向面试官讲清楚 Spring IOC 的完整流程。

---

## 一、从项目代码开始

看 `BookServiceImpl.java` 的开头：

```java
@Slf4j
@Service                                    // ← 这个注解
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    private final BookCategoryService categoryService;   // ← 依赖注入
    private final RedisService redisService;              // ← 依赖注入
```

问题来了：
- `@Service` 到底做了什么？
- `categoryService` 和 `redisService` 没有 `new`，对象是从哪来的？
- 为什么 `BookController` 里能直接注入 `BookService`？

这一切的答案就是 **Spring IOC/DI**。

---

## 二、什么是 IOC（控制反转）？

### 传统方式：自己 new 对象

没有 Spring 的时候，你要这么写代码：

```java
public class BookController {
    private BookService bookService = new BookServiceImpl();  // 自己 new

    public Result<BookVO> getBookById(Long id) {
        return bookService.getBookById(id);
    }
}
```

问题：
1. `BookController` 和 `BookServiceImpl` **紧耦合**——如果要换成 `BookServiceImplV2`，必须改 `BookController` 的代码
2. `BookService` 内部如果依赖 `BookMapper`、`RedisService`，也要自己 new，层层嵌套
3. 对象的创建和销毁都要自己管理，麻烦

### Spring 方式：容器帮你创建和管理

有了 Spring：

```java
@RestController
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;   // 不 new，等 Spring 注入
}
```

`BookController` 不再自己创建 `BookService`，而是**声明"我需要一个 BookService"**，由 Spring 容器负责创建并注入进来。

这就是**控制反转（Inversion of Control, IOC）**：

> **对象的创建权和管理权从程序员手中反转给了 Spring 容器。**

你不需要 `new`，不需要管对象什么时候创建、什么时候销毁，Spring 全帮你做了。

### IOC 的好处

1. **解耦**：`BookController` 只依赖 `BookService` 接口，不依赖具体实现类。换实现类不用改 Controller
2. **可测试**：单元测试时可以注入 Mock 对象
3. **对象复用**：默认单例，整个应用共用一个对象，节省内存
4. **生命周期管理**：Spring 负责对象的创建、初始化、销毁

---

## 三、什么是 DI（依赖注入）？

依赖注入（Dependency Injection, DI）是 IOC 的**实现方式**：

> **Spring 容器在创建对象时，把它依赖的其他对象注入进来。**

比如创建 `BookServiceImpl` 时，Spring 发现它依赖 `BookCategoryService` 和 `RedisService`，就先创建这两个对象，然后通过构造器注入给 `BookServiceImpl`。

### 项目里的注入链

```
BookController
    └── 依赖 BookService (接口)
         └── 实现类 BookServiceImpl
              ├── 依赖 BookCategoryService
              ├── 依赖 RedisService
              └── 继承 ServiceImpl<BookMapper, Book>
                   └── 依赖 BookMapper (MyBatis 代理对象)
```

Spring 在启动时会把整个依赖链上的对象都创建好，按依赖顺序注入。

---

## 四、Bean 是什么？

**Bean 就是 Spring 容器管理的对象。**

在传统 Java 里，你 `new` 出来的对象叫对象。在 Spring 里，由 Spring 容器创建、管理、销毁的对象叫 **Bean**。

项目里的 Bean 包括：
- `BookController`、`AuthController`（`@RestController`）
- `BookServiceImpl`、`UserServiceImpl`（`@Service`）
- `RedisConfig`、`RabbitMQConfig`（`@Configuration`）
- `JwtInterceptor`（`@Component`）
- `BookMapper` 的代理对象（`@MapperScan` 生成）
- `RedisTemplate`、`RabbitTemplate`、`DataSource`（自动配置创建的）

### 怎么把一个类变成 Bean？

四种方式：

| 方式 | 注解/配置 | 项目里的例子 |
|------|----------|-------------|
| **组件扫描** | `@Component`、`@Service`、`@Controller`、`@Repository`、`@Configuration` | `@Service public class BookServiceImpl` |
| **Java 配置** | `@Configuration` + `@Bean` 方法 | `RedisConfig` 里的 `@Bean public RedisTemplate...` |
| **自动配置** | Spring Boot 自动配置类里的 `@Bean` | `DataSource`、`RedisTemplate`（默认） |
| **FactoryBean** | 实现 `FactoryBean` 接口 | MyBatis 的 `MapperFactoryBean`（生成 Mapper 代理） |

项目里主要用前三种。

---

## 五、`@Service`、`@Component`、`@Repository`、`@Controller` 的区别

这四个注解本质上**都是 `@Component` 的别名**，作用完全一样——把类注册为 Bean。区别只是**语义和用途**：

| 注解 | 语义 | 用在哪一层 | 项目里的例子 |
|------|------|-----------|-------------|
| `@Component` | 通用组件 | 不确定层级时 | `JwtInterceptor`、`UserContext`、`RedisService` |
| `@Service` | 业务服务 | Service 层 | `BookServiceImpl`、`UserServiceImpl` |
| `@Controller` / `@RestController` | 控制器 | Controller 层 | `BookController`、`AuthController` |
| `@Repository` | 数据仓库 | DAO/Mapper 层 | （项目里 Mapper 用 `@Mapper`，由 MyBatis 管理） |

### 为什么要分这么多？

1. **语义清晰**：看到 `@Service` 就知道是业务层，看到 `@Controller` 就知道是控制层
2. **AOP 切面**：可以按注解类型切，比如"给所有 `@Service` 的方法加事务"（Spring 的 `@Transactional` 就是这么做的）
3. **自动配置**：Spring 可以对特定注解的 Bean 做额外处理（比如 `@Repository` 的异常转换）

### 面试官问：`@Service` 和 `@Component` 有什么区别？

> 本质上没有区别，`@Service` 就是 `@Component` 的别名，都把类注册为 Bean。区别只是语义：`@Service` 表示业务层组件，`@Component` 是通用组件。从功能上讲，把 `@Service` 换成 `@Component` 项目照样能跑，但不规范。

---

## 六、Bean 的生命周期

这是面试高频考点。Spring 容器管理一个 Bean，从创建到销毁，经历这些阶段：

```
1. 实例化（Instantiation）
   │  Spring 调用构造器，创建对象（还没注入属性）
   │  对应：Bean 构造器执行
   │
2. 属性注入（Populate）
   │  Spring 把依赖的其他 Bean 注入进来（@Autowired / 构造器）
   │  对应：@RequiredArgsConstructor 的构造器参数被注入
   │
3. 初始化（Initialization）
   │  3.1 执行 Aware 接口回调（BeanNameAware、ApplicationContextAware 等）
   │  3.2 执行 BeanPostProcessor.postProcessBeforeInitialization
   │  3.3 执行 InitializingBean.afterPropertiesSet（或 @PostConstruct 方法）
   │  3.4 执行 init-method（如果配置了）
   │  3.5 执行 BeanPostProcessor.postProcessAfterInitialization
   │      （AOP 代理就是在这里创建的！）
   │
4. 使用（Ready）
   │  Bean 可以被使用了，处理业务请求
   │
5. 销毁（Destruction）
   │  5.1 执行 DisposableBean.destroy（或 @PreDestroy 方法）
   │  5.2 执行 destroy-method（如果配置了）
```

### 结合项目理解

以 `BookServiceImpl` 为例：

1. **实例化**：Spring 调用 `BookServiceImpl(BookCategoryService, RedisService)` 构造器
2. **属性注入**：构造器参数 `categoryService` 和 `redisService` 被注入（这两个 Bean 已经提前创建好了）
3. **初始化**：
   - `@Slf4j` 的 `log` 对象在编译期就生成了，不涉及生命周期
   - 如果有 `@PostConstruct` 方法会在这里执行（项目里没有）
4. **使用**：处理图书查询、新增等请求
5. **销毁**：应用关闭时，Spring 容器关闭，Bean 被销毁

### 面试必背：Bean 生命周期的完整流程

> 1. **实例化**：调用构造方法创建 Bean 实例
> 2. **属性注入**：Spring 注入依赖的属性和其他 Bean
> 3. **Aware 回调**：如果实现了 `BeanNameAware`、`ApplicationContextAware` 等接口，执行回调方法
> 4. **BeanPostProcessor 前置处理**：执行 `postProcessBeforeInitialization`
> 5. **初始化**：执行 `@PostConstruct` 方法 → `InitializingBean.afterPropertiesSet` → `init-method`
> 6. **BeanPostProcessor 后置处理**：执行 `postProcessAfterInitialization`（AOP 代理在此创建）
> 7. **使用**：Bean 就绪，可以处理请求
> 8. **销毁**：执行 `@PreDestroy` 方法 → `DisposableBean.destroy` → `destroy-method`

---

## 七、依赖注入的底层原理

### 构造器注入的过程

以 `BookController` 为例：

```java
@RestController
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
}
```

Lombok 生成的构造器：
```java
public BookController(BookService bookService) {
    this.bookService = bookService;
}
```

Spring 创建 `BookController` 的过程：

1. Spring 扫描到 `BookController`，注册为 Bean 定义（BeanDefinition）
2. 启动时，Spring 要实例化 `BookController`
3. 发现它只有一个构造器 `BookController(BookService)`
4. Spring 知道需要一个 `BookService` 类型的参数
5. Spring 去容器里找 `BookService` 类型的 Bean
   - 找到 `BookServiceImpl`（它实现了 `BookService` 接口）
   - 如果 `BookServiceImpl` 还没创建，先递归创建它（以及它的依赖）
6. 把 `BookServiceImpl` 作为参数传入构造器，创建 `BookController` 实例
7. 把创建好的 `BookController` 放入容器（单例缓存）

### 底层用了什么技术？

- **反射（Reflection）**：Spring 通过反射获取类的构造器、方法、字段，动态创建对象和调用方法
- **递归解析依赖**：创建 Bean 前先创建它依赖的 Bean，形成依赖树，按顺序创建
- **缓存**：单例 Bean 创建后放入缓存（`singletonObjects`），下次直接取

### 为什么构造器注入能发现循环依赖？

假设有循环依赖：
```java
@Service
public class A {
    private final B b;
    public A(B b) { this.b = b; }
}

@Service
public class B {
    private final A a;
    public B(A a) { this.a = a; }
}
```

创建 A 时需要 B，创建 B 时需要 A——**死锁**。Spring 在启动时就会检测到这个循环依赖并报错，因为构造器注入要求依赖在构造时就完全就绪，无法提前暴露半成品。

而字段注入可以通过"三级缓存"提前暴露半成品 Bean 来解决循环依赖（但这是 Spring 的妥协，不代表循环依赖是好事）。

---

## 八、单例 Bean 和多例 Bean

### 默认是单例

Spring 的 Bean 默认是**单例（singleton）**：整个应用中只有一个实例，所有地方共享。

项目里的 `BookController`、`BookServiceImpl`、`RedisService` 都是单例。

### 单例的线程安全问题

单例 Bean 被多个线程同时访问，**如果有成员变量，就可能有线程安全问题**。

看项目里的 `BookServiceImpl`：
```java
@Service
public class BookServiceImpl {
    private final BookCategoryService categoryService;  // 依赖的其他 Bean（也是单例，无状态）
    private final RedisService redisService;             // 依赖的其他 Bean

    private static final String BOOK_CACHE_PREFIX = "library:book:";  // 静态常量，线程安全
    private static final Set<String> ALLOWED_ORDER_FIELDS = Set.of(...);  // 静态常量
```

- `categoryService`、`redisService` 是依赖注入的其他单例 Bean，它们本身也是无状态的
- `BOOK_CACHE_PREFIX`、`ALLOWED_ORDER_FIELDS` 是静态常量，不可变
- 方法里的局部变量（如 `page`、`wrapper`、`book`）每个线程有自己的栈，互不影响

所以 `BookServiceImpl` 是**线程安全的**。

**什么时候会有线程安全问题？** 如果单例 Bean 里有**可变的成员变量**（比如 `private int count;` 用来计数），多个线程同时修改就会有问题。

**企业项目规范**：Service/Controller 层的 Bean 不要写可变成员变量，所有状态放在方法局部变量或数据库/Redis 里。

### 多例 Bean（prototype）

可以通过 `@Scope("prototype")` 设置为多例：每次获取 Bean 都创建新实例。

```java
@Component
@Scope("prototype")
public class MyPrototypeBean {
}
```

项目里没有用到多例 Bean。大多数场景用单例就够了。

### 面试官问：Spring Bean 默认是单例还是多例？单例有线程安全问题吗？

> - Spring Bean 默认是**单例（singleton）**，整个应用中只有一个实例
> - 单例 Bean 是否线程安全取决于**是否有可变的成员变量**
> - 如果 Bean 是无状态的（只有方法和不可变的依赖/常量），就是线程安全的，项目里的 Controller/Service 都是这种
> - 如果有可变成员变量（如计数器、缓存 Map），就有线程安全问题，需要加锁或用 ThreadLocal/ConcurrentHashMap
> - 企业项目规范：Service/Controller 不要写可变成员变量

---

## 九、Spring 容器的启动流程（简化版）

结合项目，Spring 容器启动时做了什么：

```
1. 读取配置
   │  - 读取 application.yml（端口、数据源、Redis、MQ 配置）
   │  - 解析 @SpringBootApplication（自动配置 + 组件扫描）
   │
2. 扫描 Bean 定义
   │  - @ComponentScan 扫描 com.example.library 包
   │  - 找到 @RestController、@Service、@Component、@Configuration 等
   │  - 注册为 BeanDefinition（Bean 的"图纸"，还没实例化）
   │  - @MapperScan 扫描 Mapper 接口，注册为 MapperFactoryBean
   │  - 自动配置类也注册了一批 BeanDefinition（DataSource、RedisTemplate 等）
   │
3. 实例化 Bean（按依赖顺序）
   │  - 先创建无依赖的 Bean（如 RedisConfig、RabbitMQConfig）
   │  - 再创建依赖它们的 Bean（如 RedisService）
   │  - 最后创建 Controller（依赖 Service）
   │  - 每个 Bean：实例化 → 属性注入 → 初始化
   │  - AOP 代理在初始化后创建
   │
4. 容器刷新完成
   │  - 所有单例 Bean 都已创建并注入
   │  - 启动内嵌 Tomcat，监听 8080 端口
   │  - 应用就绪，可以接收请求
```

---

## 十、本课必须记住的 7 件事

1. **IOC（控制反转）**：对象的创建权和管理权从程序员反转给 Spring 容器，你不需要 `new`
2. **DI（依赖注入）**：Spring 创建对象时把它依赖的其他对象注入进来，是 IOC 的实现方式
3. **Bean**：由 Spring 容器管理的对象。通过 `@Component`/`@Service`/`@Controller`/`@Configuration`+`@Bean` 注册
4. **`@Service` = `@Component` 的别名**：功能一样，只是语义不同（业务层 vs 通用组件）
5. **Bean 生命周期**：实例化 → 属性注入 → Aware 回调 → BeanPostProcessor 前置 → 初始化（@PostConstruct）→ BeanPostProcessor 后置（AOP代理）→ 使用 → 销毁（@PreDestroy）
6. **依赖注入底层**：反射 + 递归解析依赖 + 单例缓存。构造器注入是官方推荐，能提前发现循环依赖
7. **Bean 默认单例**：无状态的单例 Bean 是线程安全的；不要在 Service/Controller 里写可变成员变量

---

## 十一、本节关键代码

```java
// BookServiceImpl：典型的 Service 层 Bean
@Slf4j
@Service                                    // 注册为 Service Bean（单例）
@RequiredArgsConstructor                     // Lombok 生成构造器，用于构造器注入
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    // final 字段 = 必须通过构造器注入的依赖
    private final BookCategoryService categoryService;
    private final RedisService redisService;

    // 静态常量 = 不可变，线程安全
    private static final String BOOK_CACHE_PREFIX = "library:book:";
    private static final long CACHE_EXPIRE_MINUTES = 30;

    // 方法里的局部变量 = 每个线程独立，线程安全
    @Override
    public BookVO getBookById(Long id) {
        String cacheKey = BOOK_CACHE_PREFIX + id;           // 局部变量
        BookVO cachedBook = redisService.get(cacheKey, BookVO.class);  // 局部变量
        if (cachedBook != null) {
            return cachedBook;
        }
        Book book = getById(id);                              // 局部变量
        // ...
    }
}
```

```java
// RedisConfig：用 @Configuration + @Bean 手动注册 Bean
@Configuration                               // 配置类，本身也是 Bean
public class RedisConfig {
    @Bean                                    // 方法返回值注册为 Bean，方法名是 Bean 的 id
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // 设置序列化方式...
        return template;
    }
}
```

---

## 十二、本节练习

### 练习1：验证 Bean 是单例

在 `BookController` 和 `BookServiceImpl` 里各加一个构造器，打印一句话：

```java
public BookController() {
    System.out.println("BookController 被创建了！" + this);
}
```

启动项目，观察：
- 每个类只打印一次（单例）
- `this` 的地址每次一样

然后在 `BookController` 里加一个接口，每次请求打印 `this`：
```java
@GetMapping("/whoami")
public Result<String> whoami() {
    return Result.success("BookController: " + this + ", BookService: " + bookService);
}
```
多次访问，观察地址是否不变。

### 练习2：制造循环依赖，观察报错

创建两个类：
```java
@Service
public class ServiceA {
    private final ServiceB serviceB;
    public ServiceA(ServiceB serviceB) { this.serviceB = serviceB; }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;
    public ServiceB(ServiceA serviceA) { this.serviceA = serviceA; }
}
```
启动项目，观察报错信息（`BeanCurrentlyInCreationException`）。然后删掉这两个类。

> **目的**：理解构造器注入能提前发现循环依赖。

### 练习3：理解 @PostConstruct 和 @PreDestroy

在 `BookServiceImpl` 里加：
```java
@PostConstruct
public void init() {
    System.out.println("BookServiceImpl 初始化完成！");
}

@PreDestroy
public void destroy() {
    System.out.println("BookServiceImpl 被销毁了！");
}
```
启动项目观察 `@PostConstruct` 执行，正常关闭项目（Ctrl+C）观察 `@PreDestroy` 执行。

---

## 十三、自测题

### Q1：什么是 IOC？什么是 DI？它们的关系是什么？

<details>
<summary>点击查看答案</summary>

- **IOC（控制反转）**：是一种设计思想，指对象的创建权和管理权从程序员手中反转给了 Spring 容器。你不需要 `new` 对象，Spring 帮你创建、管理、销毁。
- **DI（依赖注入）**：是 IOC 的实现方式，指 Spring 容器在创建对象时，把它依赖的其他对象注入进来。
- **关系**：IOC 是思想/目标，DI 是实现手段。没有 DI，IOC 就无法实现——Spring 要管理对象，就必须知道对象依赖什么，然后把依赖注入进去。

</details>

### Q2：Spring 里怎么把一个类注册为 Bean？项目里用了哪些方式？

<details>
<summary>点击查看答案</summary>

四种方式：
1. **组件扫描**：`@Component`、`@Service`、`@Controller`、`@RestController`、`@Repository`、`@Configuration` 加在类上，`@ComponentScan` 自动扫描注册
2. **Java 配置**：`@Configuration` 类里用 `@Bean` 注解的方法，返回值注册为 Bean
3. **自动配置**：Spring Boot 自动配置类里的 `@Bean`，根据依赖条件自动注册
4. **FactoryBean**：实现 `FactoryBean` 接口，自定义 Bean 创建逻辑（如 MyBatis 的 Mapper 代理）

项目里：
- `BookController` 用 `@RestController`（组件扫描）
- `BookServiceImpl` 用 `@Service`（组件扫描）
- `RedisTemplate` 用 `RedisConfig` 里的 `@Bean`（Java 配置）
- `DataSource` 由 `DataSourceAutoConfiguration` 自动配置（自动配置）
- `BookMapper` 代理对象由 `@MapperScan` + `MapperFactoryBean` 创建（FactoryBean）

</details>

### Q3：Bean 的生命周期包括哪些阶段？AOP 代理在哪个阶段创建？

<details>
<summary>点击查看答案</summary>

Bean 生命周期：
1. **实例化**：调用构造器创建对象
2. **属性注入**：注入依赖的其他 Bean
3. **Aware 回调**：`BeanNameAware`、`ApplicationContextAware` 等接口回调
4. **BeanPostProcessor 前置处理**：`postProcessBeforeInitialization`
5. **初始化**：`@PostConstruct` → `InitializingBean.afterPropertiesSet` → `init-method`
6. **BeanPostProcessor 后置处理**：`postProcessAfterInitialization`
7. **使用**：Bean 就绪，处理请求
8. **销毁**：`@PreDestroy` → `DisposableBean.destroy` → `destroy-method`

**AOP 代理在第 6 步（BeanPostProcessor 后置处理）创建**。具体是 `AbstractAutoProxyCreator` 这个 BeanPostProcessor 在后置处理时，判断 Bean 是否需要 AOP 代理，如果需要就创建代理对象替换原始对象。

</details>

### Q4：Spring Bean 默认是单例还是多例？单例 Bean 线程安全吗？

<details>
<summary>点击查看答案</summary>

- Spring Bean 默认是**单例（singleton）**，整个应用中只有一个实例，所有地方共享。
- 单例 Bean 是否线程安全，取决于**是否有可变的成员变量**：
  - 如果 Bean 是无状态的（只有方法、不可变的依赖注入、静态常量），就是线程安全的。项目里的 Controller/Service 都是这种，方法里的局部变量每个线程独立。
  - 如果有可变成员变量（如 `private int count`、`private Map cache`），多个线程同时修改就有线程安全问题，需要加锁或用线程安全的集合。
- 企业项目规范：Service/Controller 层不要写可变成员变量，状态放在方法局部变量、数据库或 Redis 里。

</details>

### Q5：构造器注入、字段注入、Setter 注入有什么区别？为什么推荐构造器注入？

<details>
<summary>点击查看答案</summary>

- **字段注入**：`@Autowired private BookService bookService;`。简洁，但不能注入 final、不利于测试、循环依赖隐藏较深。
- **Setter 注入**：`@Autowired public void setBookService(...)`。适合可选依赖，但对象可能未完全初始化。
- **构造器注入**：`public BookController(BookService bookService)`。Spring 官方推荐。

推荐构造器注入的原因：
1. 可注入 `final` 字段，保证依赖不可变
2. 保证依赖不为空（构造器必须赋值，找不到 Bean 启动时报错）
3. 利于单元测试（直接 `new BookController(mockService)`，不需要 Spring 容器）
4. 循环依赖在启动时就暴露（构造器要求依赖完全就绪，无法提前暴露半成品）
5. 项目里用 `@RequiredArgsConstructor`（Lombok）+ `final` 字段，就是构造器注入的最佳实践

</details>

---

## 十四、面试题

### 面试题1：请详细描述 Spring Bean 的生命周期。

> **答题要点**（按顺序）：
> 1. **实例化**：Spring 通过反射调用构造方法创建 Bean 实例
> 2. **属性注入**：Spring 解析 Bean 的依赖，通过反射注入属性（字段注入/Setter 注入/构造器注入的参数在实例化时就传入了）
> 3. **Aware 接口回调**：如果 Bean 实现了 `BeanNameAware`、`BeanFactoryAware`、`ApplicationContextAware` 等接口，执行对应的回调方法，让 Bean 获取容器信息
> 4. **BeanPostProcessor 前置处理**：执行所有 `BeanPostProcessor` 的 `postProcessBeforeInitialization` 方法
> 5. **初始化**：先执行 `@PostConstruct` 注解的方法，再执行 `InitializingBean` 接口的 `afterPropertiesSet` 方法，最后执行配置的 `init-method`
> 6. **BeanPostProcessor 后置处理**：执行所有 `BeanPostProcessor` 的 `postProcessAfterInitialization` 方法。**AOP 代理就是在这里创建的**（`AbstractAutoProxyCreator`）
> 7. **使用**：Bean 就绪，存入单例缓存，处理业务请求
> 8. **销毁**：容器关闭时，先执行 `@PreDestroy` 方法，再执行 `DisposableBean` 接口的 `destroy` 方法，最后执行配置的 `destroy-method`

### 面试题2：Spring IOC 容器的初始化流程是怎样的？

> **答题要点**：
> 1. **配置加载**：读取 `application.yml`、注解配置、自动配置类，构建环境信息（Environment）
> 2. **Bean 定义加载**：`@ComponentScan` 扫描指定包，找到所有 `@Component` 及其子注解的类，解析成 `BeanDefinition`（Bean 的元数据，包含类名、作用域、依赖关系等），注册到 `BeanDefinitionRegistry`
> 3. **Bean 定义处理**：`BeanFactoryPostProcessor` 处理 Bean 定义（如 `PropertySourcesPlaceholderConfigurer` 替换 `${}` 占位符）
> 4. **实例化单例 Bean**：按依赖顺序实例化所有非懒加载的单例 Bean
>    - 先实例化无依赖的 Bean
>    - 再实例化依赖它们的 Bean（递归）
>    - 每个 Bean 经历完整生命周期（实例化→属性注入→初始化）
>    - AOP 代理在初始化后创建
> 5. **容器刷新完成**：所有单例 Bean 就绪，发布 `ContextRefreshedEvent`，启动内嵌 Web 服务器（Tomcat）
> 6. **应用就绪**：可以接收请求

### 面试题3：Spring 怎么解决循环依赖？为什么构造器注入的循环依赖无法解决？

> **答题要点**：
> - Spring 通过**三级缓存**解决单例 Bean 的循环依赖（字段注入/Setter 注入）：
>   1. `singletonObjects`：一级缓存，存完全初始化好的 Bean
>   2. `earlySingletonObjects`：二级缓存，存提前暴露的半成品 Bean（已实例化但未注入属性）
>   3. `singletonFactories`：三级缓存，存 Bean 的工厂对象（ObjectFactory），可以提前暴露半成品
> - 流程：创建 A 时，先把 A 的工厂放入三级缓存 → 注入 B 时发现 B 未创建 → 创建 B → B 注入 A 时从三级缓存拿到 A 的半成品（提前暴露）→ B 创建完成 → A 继续注入完成
> - **构造器注入的循环依赖无法解决**，因为构造器注入要求在实例化时就传入完整的依赖对象，而此时 A 还没实例化完成（构造器没执行完），无法提前暴露半成品。Spring 在启动时检测到构造器循环依赖会直接抛 `BeanCurrentlyInCreationException`
> - 这也是推荐构造器注入的原因之一：循环依赖在启动时就暴露，而不是运行时才出问题
> - 最佳实践：避免循环依赖，通过拆分职责、引入中间层等方式重构

---

## 十五、下一课预告

**第5课：Service 层 + 事务——`@Transactional`、事务传播、ACID**

我们会搞清楚：
- `@Transactional` 到底做了什么？Spring 事务的底层原理（AOP + 事务管理器）
- 事务的 ACID 特性，结合项目里的借阅流程理解
- 事务传播行为（REQUIRED、REQUIRES_NEW 等）
- 事务隔离级别和脏读、不可重复读、幻读
- `@Transactional` 失效的 10 种场景（面试高频）
- 项目里的借阅流程为什么需要事务
