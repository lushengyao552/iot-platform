# 第2课：Spring Boot 启动原理 + `@SpringBootApplication` 到底做了什么

> **本课目标**：彻底搞懂 `@SpringBootApplication` 这个注解的底层原理，理解 Spring Boot "约定优于配置" 的核心思想。学完这课，你应该能向面试官讲清楚 Spring Boot 的自动配置机制。

---

## 一、从项目代码开始

打开 `LibraryApplication.java`：

```java
package com.example.library;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.library.mapper")
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
```

这个类只有 3 行有效代码，但它是整个项目的入口。我们逐行拆解。

---

## 二、`@SpringBootApplication` 到底是什么？

很多人以为 `@SpringBootApplication` 是一个"魔法注解"，其实它就是一个**组合注解**。点进去看源码（简化版）：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(excludeFilters = {
    @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
    @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
})
public @interface SpringBootApplication {
    // ...
}
```

看到了吗？`@SpringBootApplication` = **三个注解的组合**：

| 注解 | 作用 |
|------|------|
| `@SpringBootConfiguration` | 标记这是一个 Spring Boot 配置类（本质就是 `@Configuration`） |
| `@EnableAutoConfiguration` | **开启自动配置**（Spring Boot 的核心） |
| `@ComponentScan` | 开启组件扫描，自动注册 Bean |

下面逐个讲。

---

## 三、第一个注解：`@SpringBootConfiguration`

### 它是什么？

`@SpringBootConfiguration` 本质上就是 `@Configuration` 的别名。点进去看：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
public @interface SpringBootConfiguration {
}
```

### `@Configuration` 又是什么？

`@Configuration` 标记一个类是**配置类**，相当于传统 Spring 开发里的 XML 配置文件。

在配置类里，你可以用 `@Bean` 注解定义对象（Bean）。比如项目里的 `RedisConfig`：

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // ... 设置序列化方式
        return template;
    }
}
```

这相当于在 XML 里写：
```xml
<bean id="redisTemplate" class="org.springframework.data.redis.core.RedisTemplate">
    <property name="connectionFactory" ref="redisConnectionFactory"/>
</bean>
```

### 为什么项目里需要它？

`LibraryApplication` 被标记为配置类后，Spring 会把它当作配置的入口。你可以直接在启动类里用 `@Bean` 定义对象（虽然项目里没这么做，都放到了单独的 Config 类里）。

### 不写会怎么样？

如果去掉 `@SpringBootConfiguration`（也就是去掉 `@SpringBootApplication`），Spring 不会把这个类当作配置类，`@Bean` 定义不会生效，自动配置也不会触发。

---

## 四、第二个注解：`@EnableAutoConfiguration`（最核心）

### 它是什么？

这是 Spring Boot **最核心的注解**，没有之一。它的作用是：

> **根据你项目里引入的依赖，自动配置 Spring 应用。**

这就是 Spring Boot "约定优于配置"（Convention over Configuration）的体现。

### 它是怎么工作的？（简化版原理）

`@EnableAutoConfiguration` 会去 classpath 下找一个文件：

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

这个文件里列了**所有自动配置类的全限定名**，比如：

```
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
...（一共 100+ 个自动配置类）
```

Spring Boot 启动时会加载这些自动配置类，但**不是全部都生效**。每个自动配置类都有条件注解，比如：

```java
@AutoConfiguration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@ConditionalOnWebApplication(type = Type.SERVLET)
public class WebMvcAutoConfiguration {
    // 自动配置 Spring MVC
}
```

`@ConditionalOnClass` 的意思是：**只有当 classpath 里存在 `Servlet` 和 `DispatcherServlet` 这两个类时，这个自动配置才生效**。

### 结合你的项目理解

你的 `pom.xml` 里引入了这些依赖：

| 依赖 | 触发的自动配置 | 效果 |
|------|--------------|------|
| `spring-boot-starter-web` | `WebMvcAutoConfiguration` | 自动配置 Spring MVC、内嵌 Tomcat、Jackson |
| `mysql-connector-j` + HikariCP | `DataSourceAutoConfiguration` | 自动配置数据源、连接池 |
| `mybatis-plus-spring-boot3-starter` | MyBatis-Plus 自动配置 | 自动配置 SqlSessionFactory、Mapper 扫描 |
| `spring-boot-starter-data-redis` | `RedisAutoConfiguration` | 自动配置 RedisTemplate、连接工厂 |
| `spring-boot-starter-amqp` | `RabbitAutoConfiguration` | 自动配置 RabbitTemplate、连接工厂 |
| `spring-boot-starter-validation` | `ValidationAutoConfiguration` | 自动配置参数校验器 |
| `spring-boot-starter-aop` | `AopAutoConfiguration` | 自动配置 AOP 代理 |

**这就是为什么你只需要在 `application.yml` 里写数据库地址，Spring Boot 就能自动连上数据库**——因为 `DataSourceAutoConfiguration` 已经帮你创建了 DataSource Bean。

### 为什么项目里需要它？

如果没有自动配置，你需要手动写大量 XML 或 Java 配置来初始化 Spring MVC、数据源、事务管理器、Redis 连接等。Spring Boot 把这些通用配置都封装好了，你只需要引入依赖 + 写少量配置。

### 不写会怎么样？

去掉 `@EnableAutoConfiguration`，所有自动配置都不生效。你需要：
- 手动配置 DispatcherServlet
- 手动配置 DataSource
- 手动配置 SqlSessionFactory
- 手动配置 RedisTemplate
- ... 基本上回到传统 SSM 开发的配置量

---

## 五、第三个注解：`@ComponentScan`

### 它是什么？

组件扫描。Spring 会自动扫描指定包下的类，把带以下注解的类创建成对象（Bean），放到 Spring 容器里：

| 注解 | 用途 | 项目里的例子 |
|------|------|-------------|
| `@Component` | 通用组件 | `JwtInterceptor`、`UserContext` |
| `@Service` | 业务层 | `BookServiceImpl`、`UserServiceImpl` |
| `@Controller` / `@RestController` | 控制层 | `BookController`、`AuthController` |
| `@Repository` | 数据访问层 | （项目里 Mapper 用 `@Mapper`，由 MyBatis 管理） |
| `@Configuration` | 配置类 | `RedisConfig`、`RabbitMQConfig`、`WebMvcConfig` |

### 扫描哪个包？

`@ComponentScan` 默认扫描**当前类所在的包及其子包**。

你的启动类在 `com.example.library` 包下，所以会扫描：
- `com.example.library.controller`
- `com.example.library.service`
- `com.example.library.service.impl`
- `com.example.library.mapper`（但 Mapper 由 `@MapperScan` 处理）
- `com.example.library.entity`（实体类不需要扫描，因为不是 Bean）
- `com.example.library.common.config`
- `com.example.library.interceptor`
- `com.example.library.util`
- ...

**这就是为什么启动类要放在最外层的包下**——如果放在 `com.example.library.controller` 下，就扫描不到 `service` 包了。

### 为什么项目里需要它？

没有组件扫描，Spring 不知道你的 `BookController`、`BookServiceImpl` 这些类的存在，不会创建它们的对象，依赖注入也无法工作。

### 不写会怎么样？

所有你自己写的 `@Service`、`@Controller`、`@Component` 都不会被注册，启动后访问接口会 404，依赖注入会报 `NoSuchBeanDefinitionException`。

---

## 六、`@MapperScan("com.example.library.mapper")`

### 它是什么？

这是 MyBatis 提供的注解，用来扫描 Mapper 接口。

注意：`@MapperScan` **不是** Spring Boot 自带的，它来自 `mybatis-spring` 包（MyBatis-Plus 的 starter 传递依赖了它）。

### 它做了什么？

1. 扫描 `com.example.library.mapper` 包下的所有接口
2. 为每个接口创建**JDK 动态代理对象**
3. 把代理对象注册到 Spring 容器中

这样，`BookServiceImpl` 里就能直接注入 `BookMapper`：

```java
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {
    // Service 层组合注入 Repository（不再继承 ServiceImpl）
    private final BookRepository bookRepository;
    // Repository 实现类 BookRepositoryImpl 继承 ServiceImpl<BookMapper, Book>，
    // 其父类 ServiceImpl 里已经注入了 baseMapper（即 BookMapper 的代理对象）
}
```

### 代理对象是怎么执行 SQL 的？

当你调用 `bookMapper.selectById(1)` 时：
1. 调用进入 JDK 动态代理的 `invoke()` 方法
2. MyBatis 根据方法名和参数，找到对应的 SQL 语句（`BaseMapper` 的方法由 MyBatis-Plus 自动生成 SQL）
3. 通过 JDBC 执行 SQL
4. 把结果集映射成 Java 对象返回

### 另一种写法：`@Mapper` 注解

除了 `@MapperScan`，你也可以在每个 Mapper 接口上加 `@Mapper` 注解：

```java
@Mapper
public interface BookMapper extends BaseMapper<Book> {
}
```

两种方式二选一即可。项目里用了 `@MapperScan`（更简洁，不用每个接口都加注解），同时 `BookMapper` 上也加了 `@Mapper`（重复了，但不影响运行）。

### 面试官可能问：`@MapperScan` 和 `@Mapper` 有什么区别？

> - `@Mapper`：加在每个 Mapper 接口上，逐个注册
> - `@MapperScan`：加在启动类或配置类上，批量扫描指定包下的所有 Mapper 接口
> - 效果一样，`@MapperScan` 更简洁，企业项目一般用 `@MapperScan`

---

## 七、`SpringApplication.run()` 做了什么？

```java
public static void main(String[] args) {
    SpringApplication.run(LibraryApplication.class, args);
}
```

这行代码背后，Spring Boot 做了大约 10 个步骤（简化版）：

```
1. 创建 SpringApplication 对象
   │
2. 推断应用类型（Servlet / Reactive / 普通）
   │  你的项目有 spring-boot-starter-web → Servlet 类型
   │
3. 加载 ApplicationContextInitializer 和 ApplicationListener
   │
4. 启动计时（StopWatch）
   │
5. 配置 Headless 模式（java.awt.headless=true）
   │
6. 获取并配置 Environment（加载 application.yml）
   │  读取 server.port、spring.datasource 等配置
   │
7. 打印 Banner（你的自定义图书管理系统 banner）
   │
8. 创建 ApplicationContext（Spring 容器）
   │
9. 准备 Context：
   │  - 注册启动类为 Bean 定义
   │  - 执行 Initializer
   │  - 刷新容器（这一步最核心）
   │     ├─ 执行 @ComponentScan，注册所有 Bean
   │     ├─ 执行 @EnableAutoConfiguration，加载自动配置类
   │     ├─ 执行 @MapperScan，创建 Mapper 代理
   │     ├─ 实例化所有单例 Bean（依赖注入）
   │     └─ 启动内嵌 Tomcat
   │
10. 执行 CommandLineRunner 和 ApplicationRunner
    │
11. 启动完成，打印"Started LibraryApplication in X seconds"
```

**第 9 步的"刷新容器"是最核心的**，你的所有 Bean 都是在这一步被创建和注入的。

---

## 八、本课必须记住的 7 件事

1. **`@SpringBootApplication` 是组合注解** = `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan`
2. **`@EnableAutoConfiguration` 是核心**：根据依赖自动配置，通过 `AutoConfiguration.imports` 文件加载自动配置类，用 `@ConditionalOnXXX` 条件注解控制是否生效
3. **`@ComponentScan` 默认扫描启动类所在包及其子包**，所以启动类要放在最外层
4. **`@MapperScan` 扫描 Mapper 接口，用 JDK 动态代理创建实现类**，你不用写 Mapper 实现
5. **`@Configuration` + `@Bean` 相当于 XML 配置**，用来手动定义 Bean
6. **Spring Boot "约定优于配置"**：引入依赖 + 写少量配置 = 自动完成大量初始化
7. **`SpringApplication.run()` 的核心是"刷新容器"**：扫描 Bean 定义 → 实例化 → 依赖注入 → 启动 Tomcat

---

## 九、本节关键代码

```java
// 启动类
@SpringBootApplication          // 组合注解：自动配置 + 组件扫描 + 配置类
@MapperScan("com.example.library.mapper")  // 扫描 Mapper 接口，生成代理
public class LibraryApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);  // 启动 Spring Boot
    }
}
```

```java
// 配置类示例：RedisConfig
@Configuration                  // 标记为配置类
public class RedisConfig {
    @Bean                       // 定义一个 Bean，方法名就是 Bean 的 id
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        return template;
    }
}
```

---

## 十、本节练习

### 练习1：去掉 `@MapperScan` 会怎样？

把 `@MapperScan("com.example.library.mapper")` 注释掉，启动项目，观察报错信息。然后在 `BookMapper` 接口上加 `@Mapper` 注解，再启动，看是否恢复正常。

> **目的**：理解 `@MapperScan` 和 `@Mapper` 的关系。

### 练习2：排除某个自动配置

修改启动类：
```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
```
启动项目，观察报错。然后恢复。

> **目的**：理解自动配置是可以手动排除的，以及 DataSource 自动配置的作用。

### 练习3：在启动类里定义一个 Bean

在 `LibraryApplication` 里加一个方法：
```java
@Bean
public String helloBean() {
    return "Hello from LibraryApplication!";
}
```
然后在任意一个 Controller 里注入它：
```java
@Autowired
private String helloBean;

@GetMapping("/hello")
public Result<String> hello() {
    return Result.success(helloBean);
}
```
访问 `http://localhost:8080/api/hello`，看是否返回了字符串。

> **目的**：理解 `@Configuration` + `@Bean` 的作用，以及启动类本身就是配置类。

---

## 十一、自测题

### Q1：`@SpringBootApplication` 组合了哪三个注解？各自的作用是什么？

<details>
<summary>点击查看答案</summary>

1. **`@SpringBootConfiguration`**：本质是 `@Configuration`，标记启动类为配置类，可以用 `@Bean` 定义对象
2. **`@EnableAutoConfiguration`**：开启自动配置，根据 classpath 里的依赖自动配置 Spring 应用（最核心）
3. **`@ComponentScan`**：组件扫描，自动扫描并注册 `@Service`、`@Controller`、`@Component` 等注解的类

</details>

### Q2：Spring Boot 的自动配置是怎么实现的？

<details>
<summary>点击查看答案</summary>

1. `@EnableAutoConfiguration` 触发自动配置
2. Spring Boot 从 classpath 的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件中加载所有自动配置类的全限定名
3. 每个自动配置类都有条件注解（如 `@ConditionalOnClass`、`@ConditionalOnMissingBean`、`@ConditionalOnProperty`）
4. 满足条件的自动配置类才会生效，创建对应的 Bean
5. 比如引入 `spring-boot-starter-web` 后，classpath 里有 `DispatcherServlet`，`WebMvcAutoConfiguration` 的条件满足，就自动配置 Spring MVC

</details>

### Q3：为什么启动类要放在最外层的包下？如果放在 `com.example.library.controller` 下会怎样？

<details>
<summary>点击查看答案</summary>

因为 `@ComponentScan` 默认扫描**当前类所在的包及其子包**。

如果启动类放在 `com.example.library.controller` 下，只会扫描 `controller` 包及其子包，扫描不到 `service`、`mapper`、`config` 等包，这些包里的 Bean 不会被注册，导致依赖注入失败、接口 404。

所以启动类必须放在**所有业务包的共同父包**下（通常是 `com.example.library`）。

</details>

### Q4：`@MapperScan` 和 `@Mapper` 有什么区别？项目里用了哪种？

<details>
<summary>点击查看答案</summary>

- **`@Mapper`**：加在每个 Mapper 接口上，逐个注册为 Bean
- **`@MapperScan`**：加在启动类或配置类上，指定要扫描的包，批量注册该包下所有 Mapper 接口

效果完全一样。项目里用了 `@MapperScan("com.example.library.mapper")`（在启动类上），同时 `BookMapper` 等接口上也加了 `@Mapper`（属于重复配置，但不影响运行）。

企业项目一般用 `@MapperScan`，更简洁。

</details>

### Q5：`@Configuration` 和 `@Component` 有什么区别？

<details>
<summary>点击查看答案</summary>

- **`@Component`**：通用组件注解，标记的类会被注册为普通 Bean
- **`@Configuration`**：配置类注解，标记的类也会被注册为 Bean，但它是"配置类"，内部可以用 `@Bean` 注解定义其他 Bean

关键区别：`@Configuration` 类里的 `@Bean` 方法会被 CGLIB 代理，保证单例语义（多次调用同一个 `@Bean` 方法返回的是同一个对象）；而 `@Component` 类里的 `@Bean` 方法不会被代理，每次调用都会创建新对象。

简单理解：`@Configuration` 是"完整版配置类"，`@Component` 是"轻量版"。定义 Bean 时优先用 `@Configuration`。

</details>

---

## 十二、面试题

### 面试题1：请解释 Spring Boot 的自动配置原理。

> **答题要点**：
> 1. `@SpringBootApplication` 中的 `@EnableAutoConfiguration` 开启自动配置
> 2. 通过 `AutoConfiguration.imports` 文件加载 100+ 个自动配置类
> 3. 每个自动配置类使用 `@ConditionalOnXXX` 条件注解（`@ConditionalOnClass`、`@ConditionalOnMissingBean`、`@ConditionalOnProperty` 等）判断是否生效
> 4. 满足条件的自动配置类会向容器注册 Bean，完成自动初始化
> 5. 比如引入 `spring-boot-starter-data-redis` 后，`RedisAutoConfiguration` 生效，自动创建 `RedisTemplate` 和连接工厂
> 6. 用户可以通过 `exclude` 属性排除不需要的自动配置，或通过自定义 `@Bean` 覆盖默认配置

### 面试题2：`@SpringBootApplication` 注解有什么作用？

> **答题要点**：
> 1. 它是一个组合注解，包含三个核心注解
> 2. `@SpringBootConfiguration`：标记为配置类（本质是 `@Configuration`）
> 3. `@EnableAutoConfiguration`：开启自动配置，根据依赖自动初始化
> 4. `@ComponentScan`：组件扫描，自动注册 `@Service`、`@Controller` 等 Bean
> 5. 此外还可以通过 `exclude` 排除特定自动配置，`scanBasePackages` 指定扫描包

### 面试题3：Spring Boot 的约定优于配置体现在哪里？

> **答题要点**：
> 1. **依赖约定**：引入 `spring-boot-starter-web` 就自动拥有 Spring MVC + 内嵌 Tomcat + Jackson，不需要手动配置
> 2. **配置约定**：默认读取 `application.yml`/`application.properties`，默认端口 8080，默认 context-path 为 `/`
> 3. **包扫描约定**：启动类所在包自动被扫描，不需要配置 `<context:component-scan>`
> 4. **自动配置约定**：根据 classpath 里的依赖自动配置 DataSource、Redis、RabbitMQ 等
> 5. **打包约定**：`spring-boot-maven-plugin` 自动打成可执行 jar，内置 Tomcat，`java -jar` 直接运行
> 6. 核心思想：减少样板配置，让开发者专注业务逻辑

---

## 十三、下一课预告

**第3课：Controller 层详解——`@RestController`、`@RequestMapping`、参数绑定**

我们会逐行拆解 `BookController.java`，搞清楚：
- `@RestController` 和 `@Controller` 的区别？为什么返回的是 JSON 而不是页面？
- `@RequestMapping`、`@GetMapping`、`@PostMapping` 怎么映射 URL？
- `@PathVariable`、`@RequestParam`、`@RequestBody` 分别从哪里取参数？
- `@Valid` 怎么做参数校验？
- `@RequiredArgsConstructor` 是什么？和 `@Autowired` 有什么关系？
