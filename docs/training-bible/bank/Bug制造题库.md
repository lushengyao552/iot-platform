# Bug 制造题库（共享）

> 用户做完一个模块后，我给一个有 Bug 的版本，让他找。不要一次给太多，一次一个。

## 阶段 00-02 常见 Bug

1. **JDK 版本错**：用 JDK 8 跑 Spring Boot 3，启动报 ClassNotFound。让用户看报错。
2. **启动类放错包**：启动类放在 default 包，Controller 扫不到。
3. **驱动类名错**：MySQL 8 还写 com.mysql.jdbc.Driver。
4. **字段名不映射**：数据库 user_name，Java 写 username，又没开 map-underscore-to-camel-case。
5. **Entity 用 int 接 stock**：select 时 null 会 NPE。

## 阶段 03-05 常见 Bug

6. **忘了分页插件**：selectPage 返回 total=0。
7. **QueryWrapper 写字段名拼错**：编译过，运行 SQL 报错。
8. **select * 查大字段**：图书简介是 TEXT，列表页也全查出来。
9. **Controller 用 @RequestBody 接 GET 参数**：400。
10. **@RestController 写成 @Controller**：返回值被当视图名。

## 阶段 06-08 常见 Bug

11. **Service 也返回 Result**：分层污染。
12. **e.printStackTrace()**：日志里看不到栈。
13. **log.error(e.getMessage())**：丢了异常栈。
14. **兜底异常把堆栈返给前端**：信息泄露。
15. **@NotBlank 用在 Long 上**：启动报错或运行不校验。
16. **GET 参数忘了 @Validated**：注解不生效。

## 阶段 09-10 常见 Bug

17. **ThreadLocal 没 remove**：并发请求串号。
18. **拦截器里抛异常全局处理器接不到**：返回 500 白页。
19. **secret 硬编码**：安全问题。
20. **密码用 MD5**：安全问题。
21. **登录失败也提示"密码错误"**：用户枚举。
22. **/api/users/me 接受前端 userId 参数**：水平越权。
23. **401 和 403 不分**：前端无法处理。

## 阶段 11-12 常见 Bug

24. **借书不查"该用户未还"**：同一本书借两条。
25. **库存用 select → Java -1 → update**：超卖。
26. **@Transactional 加在 private 方法**：事务失效。
27. **自调用 this.borrowBook()**：事务失效。
28. **try-catch 吃了异常**：事务不回滚。
29. **还书不校验记录归属**：还别人的书。
30. **N+1**：借阅记录循环查 book。

## 阶段 13-14 常见 Bug

31. **JDK 序列化存 Redis**：乱码。
32. **缓存没删**：更新图书后详情还是旧的。
33. **缓存空值不设 TTL**：用户改了名也查不到。
34. **setnx 不带过期**：死锁。
35. **消费者自动 ACK**：处理到一半挂了，消息丢。
36. **消费者无脑 requeue**：死循环。
37. **发消息在事务里**：事务回滚消息已发。
38. **定时任务多实例重复跑**：通知发两遍。

## 阶段 15-16 常见 Bug

39. **测试不回滚**：数据库脏数据。
40. **Mock 所有依赖**：等于没测。
41. **只测正常分支**：异常分支炸了不知道。
42. **深分页用 limit 10000,10**：慢。
43. **循环里查库**：N+1。
44. **like '%java%'**：索引失效。

## 制造 Bug 的节奏

- 每个模块结束挑 1-2 个
- 先给现象，让用户自己定位
- 定位不出来按五级提示
- 修完问他"为什么会出这个 Bug"
