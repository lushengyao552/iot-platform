# Stage 00：环境与项目初始化

## 阶段目标

不看任何脚手架视频，独立把一个能跑的 Spring Boot 3.2 Maven 工程立起来，跑通第一个 HTTP 接口。理解 Maven 是什么、Spring Boot 启动类干了什么、application.yml 怎么生效、第一个 Bean 是怎么被 Spring 管理的。

## 前置要求

- JDK 17 已装
- Maven 已装（或用 IDEA 自带）
- IDEA / VS Code 任选
- MySQL 8 已装（本阶段先不连库，先把工程跑起来）

## 本阶段知识点清单

- Maven 坐标（groupId / artifactId / version）
- spring-boot-starter-parent 的作用
- starter 是什么（spring-boot-starter-web / lombok / validation / mybatis-plus / mysql-connector-j）
- @SpringBootApplication 拆开来是什么
- main 方法里 SpringApplication.run 在干嘛
- application.yml vs application.properties
- 端口、context-path 配置
- 一个最简单的 @RestController + @GetMapping
- 包结构为什么要这样放（启动类所在包是根）

---

## 题目

### 【第 0-1 题：把工程立起来】（已下发，等交码）

**业务背景**
你要从零开始做一个图书管理系统。第一步不是写业务，而是把"地基"打稳：Maven 工程能编译、Spring Boot 能启动、能通过浏览器或 Knife4j 访问到一个接口。

**我的任务**
1. 用 Maven 建一个工程，坐标自己定（建议 com.lushengyao.library）
2. 父 POM 用 spring-boot-starter-parent，版本 3.2.x
3. 至少引入：
   - spring-boot-starter-web
   - spring-boot-starter-validation
   - lombok
   - mybatis-plus-boot-starter（版本自己选 3.5.x）
   - mysql-connector-j（runtime）
   - knife4j-openapi3-jakarta-spring-boot-starter
4. 写一个启动类，类名自己定
5. application.yml 里配：
   - 端口（建议 8080）
   - 应用名 library-management
   - 暂时不要配数据库连接（下一题再配）
6. 写一个 GET 接口 /ping，返回字符串 "pong"

**限制条件**
- 不许用 Spring Initializr 一键生成（如果用了，必须解释每一项选了什么、为什么）
- 不许抄原仓库 pom.xml
- 必须能 `mvn spring-boot:run` 或在 IDEA 里直接启动成功
- 启动后控制台要有 Spring 启动 banner 和端口提示

**验收标准**
- 工程能编译、能启动
- 浏览器访问 http://localhost:8080/ping 返回 pong
- pom.xml 里依赖版本你能指着每一个说出它是干嘛的
- application.yml 里每一行配置你能说出作用

**思考题（不给答案）**
1. 为什么 Spring Boot 3.2 必须用 JDK 17？用 JDK 8 会怎样？
2. spring-boot-starter-parent 到底帮你做了什么？如果不用它会怎样？
3. @SpringBootApplication 这一个注解背后其实开了哪几件事？
4. 启动类为什么不能放在默认包（default package）里？
5. Lombok 是编译期还是运行期起作用？它对你写代码有什么约束？

---

### 【第 0-2 题：把数据库连上】（0-1 验收通过后下发）

**业务背景**
工程能跑了，但还是个空壳。下一步要让它真正连上 MySQL，为后面建表做准备。

**我的任务**
1. 在本机 MySQL 8 里建一个数据库 `library_management`（字符集 utf8mb4，排序规则 utf8mb4_unicode_ci）
2. 在 application.yml 里配数据源：
   - url（带时区、字符集、useSSL 等参数，自己想全）
   - username / password
3. 配 MyBatis-Plus 的基本项（mapper 位置、日志输出 SQL 到控制台方便调试）
4. 写一段代码证明连上了：启动时打印一次 `SELECT 1`，或者写一个临时接口 /db/check 返回数据库当前时间

**限制条件**
- 不许把密码硬编码在 yml 里直接提交（先本地用，留一个 TODO 提醒以后用环境变量或配置中心）
- 必须用 MySQL 8 的驱动类（注意 driver-class-name 写什么）
- 连接失败时错误要能看懂

**验收标准**
- 启动不报连接错误
- /db/check 能返回 MySQL 的 now()
- 控制台能看到 MyBatis-Plus 打印出来的 SQL（哪怕只是 SELECT 1）

**思考题**
1. MySQL 8 的驱动类名和 5.x 有什么不一样？
2. jdbc url 里 serverTimezone、useSSL、characterEncoding 分别解决什么问题？
3. 为什么生产环境不建议把密码写在 yml 里？你能想到几种解决方案？
4. HikariCP 是什么？Spring Boot 默认连接池是谁？
5. 如果启动时数据源配错，Spring 会在哪个阶段失败？

---

### 【第 0-3 题：包结构与第一个 Bean】（0-2 验收通过后下发）

**业务背景**
工程和数据库都好了。现在要把项目骨架搭出来，后面所有代码都按这个分层放。

**我的任务**
在你的工程下建出下面这些包（先建空包，不放代码）：
```
com.lushengyao.library
├── controller
├── service
│   └── impl
├── mapper
├── entity
├── dto
├── vo
├── config
├── common        （统一响应、异常等）
├── interceptor
├── context       （ThreadLocal / UserContext）
└── util
```
再写一个 config 包下的配置类，里面用 @Bean 声明一个自己定义的组件（比如一个 AppInfo 类，记录应用名和版本），在启动后通过日志或一个接口把它打出来。

**限制条件**
- 必须理解为什么 controller/service/mapper 要分开
- 必须知道 @Service / @Component / @Configuration 各自用在哪
- @Autowired 和 @Resource 的区别你要能讲

**验收标准**
- 启动时能看到你 @Bean 注册的对象被创建
- 你能说出为什么不能把所有类都塞到一个包里
- 你能解释 controller 为什么不直接调 mapper

**思考题**
1. @Component / @Service / @Repository / @Controller 本质上是不是同一个注解？区别在哪？
2. @Bean 和 @Component 的区别？什么时候用哪个？
3. Spring 容器里的 Bean 默认是单例还是多例？为什么？
4. 构造器注入和 @Autowired 字段注入，推荐哪个？为什么？

---

## 本阶段毕业考试

不看任何资料，新建一个空目录：
1. 15 分钟内建出一个能启动的 Spring Boot 3.2 Maven 工程
2. 配好数据源连上 MySQL
3. 写一个 GET /hello 返回你自己的名字
4. 解释 pom.xml 里每一个依赖、启动类上每一个注解、application.yml 每一行配置

讲不清楚任何一行 → 不毕业，重做对应题。

## 本阶段反向面试题

1. Spring Boot 是怎么做到"约定优于配置"的？
2. starter 里到底打包了什么？
3. 自动装配（@EnableAutoConfiguration）大概是怎么找到要装哪些 Bean 的？
4. application.yml 是在哪个阶段被读进 Spring 容器的？
5. 为什么说 Spring Boot 不是新框架，而是"组合框架"？

## 常见坑（我自己出题时要盯这些）

- JDK 版本不是 17 → 启动直接报错
- 启动类放错包 → 扫描不到 controller / mapper
- mysql-connector-j 没写 runtime → 打 fat jar 可能找不到驱动
- knife4j 版本和 Spring Boot 3 不兼容 → 4.x 才支持 jakarta
- yml 缩进错 → 配置静默失效
