# 第10课：MySQL 表设计与索引——结合项目 4 张表讲数据库设计

> **本课目标**：结合项目里的 4 张表，搞懂数据库表设计、索引原理、字段类型选择、范式等核心知识。学完这课，你应该能独立设计一个中小型项目的数据库表结构，并能看懂 EXPLAIN 做基本的 SQL 优化。

---

## 一、从项目代码开始

看 `db/schema.sql` 里的 4 张表：

```sql
-- 1. 用户表
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN-管理员，USER-普通用户',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_email (email),
    KEY idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 图书分类表
CREATE TABLE book_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书分类表';

-- 3. 图书表
CREATE TABLE book (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    isbn          VARCHAR(20)   NOT NULL,
    title         VARCHAR(200)  NOT NULL,
    author        VARCHAR(100)  NOT NULL,
    publisher     VARCHAR(100)  DEFAULT NULL,
    publish_date  DATE          DEFAULT NULL,
    category_id   BIGINT        DEFAULT NULL,
    price         DECIMAL(10,2) DEFAULT NULL,
    stock         INT           NOT NULL DEFAULT 0,
    total_stock   INT           NOT NULL DEFAULT 0,
    description   TEXT          DEFAULT NULL,
    cover_url     VARCHAR(500)  DEFAULT NULL,
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_isbn (isbn),
    KEY idx_title (title),
    KEY idx_author (author),
    KEY idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- 4. 借阅记录表
CREATE TABLE borrow_record (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL,
    book_id     BIGINT        NOT NULL,
    borrow_date DATE          NOT NULL,
    due_date    DATE          NOT NULL,
    return_date DATE          DEFAULT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'BORROWED',
    fine        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_book_id (book_id),
    KEY idx_status (status),
    KEY idx_borrow_date (borrow_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';
```

4 张表，覆盖了用户、分类、图书、借阅记录四个核心业务实体。下面逐个知识点讲解。

---

## 二、表设计思路——4 张表的关系

### ER 图

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   sys_user   │       │ borrow_record│       │     book     │
│   (用户表)    │       │  (借阅记录表)  │       │   (图书表)    │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)      │◄──┐   │ id (PK)      │   ┌──►│ id (PK)      │
│ username(UK) │   │   │ user_id (IDX)│───┘   │ isbn (UK)    │
│ password     │   └───│ book_id (IDX)│       │ title (IDX)  │
│ role         │       │ borrow_date   │       │ author (IDX)  │
│ status       │       │ status (IDX)  │       │ category_id   │
└──────────────┘       │ fine          │       │ stock         │
                       └──────────────┘       └──────┬───────┘
                                                       │
                                              ┌────────▼───────┐
                                              │ book_category  │
                                              │  (图书分类表)   │
                                              ├────────────────┤
                                              │ id (PK)        │
                                              │ name (UK)       │
                                              │ sort            │
                                              └────────────────┘
```

### 关系说明

| 关系 | 类型 | 说明 |
|------|------|------|
| 用户 → 借阅记录 | 一对多 | 一个用户可以借多本书，一条借阅记录属于一个用户 |
| 图书 → 借阅记录 | 一对多 | 一本书可以被多次借阅（不同时间），一条借阅记录对应一本书 |
| 分类 → 图书 | 一对多 | 一个分类下有多本图书，一本图书属于一个分类 |
| 用户 → 图书 | 多对多 | 通过借阅记录表实现多对多关系（中间表） |

### 为什么没有物理外键？

注意：项目里的 `user_id`、`book_id`、`category_id` 都是**逻辑外键**（通过字段关联），没有建数据库的物理外键约束（`FOREIGN KEY`）。

企业项目一般不用物理外键，原因：
1. **性能**：外键约束会在每次 INSERT/UPDATE/DELETE 时检查关联表，有性能开销
2. **分库分表**：物理外键在分库分表场景下无法使用
3. **灵活性**：数据迁移、批量操作时外键约束会碍事
4. **应用层保证**：关联一致性由应用层（Service 层）保证，如删除分类前检查是否有图书引用

项目里就是这种做法：`category_id` 只是一个普通字段加了索引，没有物理外键约束。

---

## 三、字段类型选择

### 整数类型

| 类型 | 字节 | 范围 | 项目里的用法 |
|------|------|------|-------------|
| `TINYINT` | 1 | -128 ~ 127 | `status`（0/1）、`deleted`（0/1）——状态标志位 |
| `INT` | 4 | -21亿 ~ 21亿 | `stock`、`total_stock`、`sort`——数量、排序 |
| `BIGINT` | 8 | -922亿亿 ~ 922亿亿 | `id`（主键）——防止数据量超过 INT 上限 |

**为什么主键用 BIGINT 而不是 INT？**
- INT 上限约 21 亿，看起来很大，但分布式系统、高并发业务可能超过
- BIGINT 上限 922 亿亿，永远用不完
- 现在硬盘和内存都便宜，多 4 个字节不是问题
- 企业项目规范：主键一律用 BIGINT

**为什么 status、deleted 用 TINYINT 而不是 BOOLEAN？**
- MySQL 没有真正的 BOOLEAN 类型，`BOOLEAN` 是 `TINYINT(1)` 的别名
- 用 TINYINT 更明确，0/1 表示两种状态，未来扩展（如 status=2 表示"待审核"）也方便

### 字符串类型

| 类型 | 特点 | 项目里的用法 |
|------|------|-------------|
| `VARCHAR(n)` | 变长字符串，最多 n 字符，实际占用按实际长度 | `username`(50)、`title`(200)、`isbn`(20)、`cover_url`(500) |
| `TEXT` | 长文本，最大 64KB，不能有默认值 | `description`（图书简介，可能很长） |
| `CHAR(n)` | 定长字符串，固定 n 字符 | （项目没用） |

**VARCHAR 长度怎么定？**
- 根据业务实际需要定，不要太大也不要太小
- `username` 50：用户名一般不会超过 50 字符
- `title` 200：书名可能较长，200 足够
- `isbn` 20：ISBN 标准是 13 位，留余量 20
- `cover_url` 500：URL 可能较长，500 足够

**什么时候用 TEXT？**
- 内容可能超过 VARCHAR 上限（MySQL 5.0.3+ VARCHAR 最大 65535 字节，但 InnoDB 单行最大 65535 字节）
- 不需要默认值、不需要建索引（TEXT 建索引需要指定前缀长度）
- 项目里 `book.description`（图书简介）用 TEXT，因为简介可能很长

### 小数类型

| 类型 | 特点 | 项目里的用法 |
|------|------|-------------|
| `DECIMAL(p,s)` | 精确小数，p 总位数，s 小数位数 | `price`(10,2)、`fine`(10,2)——金额 |
| `FLOAT` / `DOUBLE` | 近似小数，有精度损失 | （项目没用） |

**为什么金额用 DECIMAL 而不是 FLOAT/DOUBLE？**
- FLOAT/DOUBLE 是二进制浮点数，存在精度损失（如 0.1 + 0.2 ≠ 0.3）
- DECIMAL 是定点数，精确存储，不会有精度损失
- 金额、价格、罚款这些对精度要求高的字段必须用 DECIMAL
- `DECIMAL(10,2)` 表示最多 10 位数字，其中 2 位小数，范围 -99999999.99 ~ 99999999.99

### 日期时间类型

| 类型 | 格式 | 项目里的用法 |
|------|------|-------------|
| `DATETIME` | `YYYY-MM-DD HH:MM:SS` | `create_time`、`update_time`——创建/更新时间 |
| `DATE` | `YYYY-MM-DD` | `borrow_date`、`due_date`、`return_date`、`publish_date`——只需要日期 |
| `TIMESTAMP` | 时间戳，范围 1970-2038 | （项目没用） |

**DATETIME vs TIMESTAMP？**
- DATETIME 范围大（1000-9999年），占 8 字节，不受时区影响
- TIMESTAMP 范围小（1970-2038年），占 4 字节，受时区影响
- 项目用 DATETIME，范围大、不受时区影响，更安全

**为什么借阅日期用 DATE 而不是 DATETIME？**
- 借阅日期只需要到天，不需要具体到时分秒
- DATE 占 3 字节，DATETIME 占 8 字节，DATE 更省空间
- 业务上借阅就是按天计算的（应还日期 = 借阅日期 + 30天）

### 通用字段设计

项目里每张表都有这 5 个字段，这是企业项目的规范做法：

| 字段 | 类型 | 作用 |
|------|------|------|
| `id` | BIGINT AUTO_INCREMENT | 主键，自增 |
| `create_time` | DATETIME DEFAULT CURRENT_TIMESTAMP | 创建时间，插入时自动设为当前时间 |
| `update_time` | DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间，每次更新自动刷新 |
| `deleted` | TINYINT DEFAULT 0 | 逻辑删除，0-未删除，1-已删除 |
| （部分表有）`status` | TINYINT / VARCHAR | 业务状态 |

`DEFAULT CURRENT_TIMESTAMP` 和 `ON UPDATE CURRENT_TIMESTAMP` 是 MySQL 的特性，让数据库自动维护时间字段，不需要应用层手动设置（虽然项目里 MyBatis-Plus 的自动填充也做了这件事，双重保险）。

---

## 四、索引原理

### 什么是索引？

索引是数据库中用于**加速查询**的数据结构，类似于书的目录。没有索引时，查询需要扫描整张表（全表扫描）；有了索引，可以通过索引快速定位到数据行。

MySQL InnoDB 引擎的索引用的是 **B+ 树**数据结构。

### 索引的类型

| 类型 | 语法 | 作用 | 项目里的例子 |
|------|------|------|-------------|
| **主键索引** | `PRIMARY KEY (id)` | 主键自带索引，唯一且非空，聚簇索引 | 每张表的 `id` |
| **唯一索引** | `UNIQUE KEY uk_name (name)` | 值唯一，允许 NULL，保证数据唯一性 | `sys_user.username`、`book.isbn`、`book_category.name` |
| **普通索引** | `KEY idx_title (title)` | 加速查询，没有唯一性约束 | `book.title`、`book.author`、`borrow_record.user_id` |
| **联合索引** | `KEY idx_a_b (a, b)` | 多个字段组成的索引，遵循最左前缀原则 | （项目里没有，下面讲） |

### 项目里的索引分析

**sys_user 表**：
```sql
PRIMARY KEY (id),                    -- 主键
UNIQUE KEY uk_username (username),   -- 用户名唯一，登录时按用户名查用户
KEY idx_email (email),               -- 按邮箱查询
KEY idx_phone (phone)                -- 按手机号查询
```
- `username` 建唯一索引：登录时 `WHERE username = ?` 走索引，同时保证用户名不重复
- `email`、`phone` 建普通索引：如果有按邮箱/手机号查询的需求，加速查询

**book 表**：
```sql
PRIMARY KEY (id),
UNIQUE KEY uk_isbn (isbn),           -- ISBN 唯一，新增时校验重复
KEY idx_title (title),                -- 按书名模糊查询
KEY idx_author (author),              -- 按作者查询
KEY idx_category_id (category_id)     -- 按分类查询（分类列表页筛选）
```
- `isbn` 唯一索引：ISBN 是图书的标准编号，不重复
- `title`、`author` 普通索引：图书搜索的核心字段
- `category_id` 普通索引：按分类筛选图书，这是常见查询

**borrow_record 表**：
```sql
PRIMARY KEY (id),
KEY idx_user_id (user_id),            -- 查"我的借阅记录"
KEY idx_book_id (book_id),            -- 查某本书的借阅历史
KEY idx_status (status),              -- 按状态筛选（借阅中/已归还/逾期）
KEY idx_borrow_date (borrow_date)     -- 按借阅日期范围查询
```
- `user_id` 索引：`WHERE user_id = ?` 查某个用户的借阅记录，这是高频查询
- `book_id` 索引：查某本书被谁借过
- `status` 索引：按状态筛选，如"只看借阅中的"
- `borrow_date` 索引：按日期范围统计

### 索引的代价

索引不是越多越好，建索引有代价：
1. **占用存储空间**：每个索引都是一棵 B+ 树，占磁盘空间
2. **降低写入性能**：INSERT/UPDATE/DELETE 时，除了更新数据，还要更新所有相关索引
3. **优化器选择困难**：索引太多，MySQL 查询优化器可能选错索引

**建索引的原则**：
- 高频查询字段建索引（WHERE、JOIN、ORDER BY、GROUP BY 后面的字段）
- 区分度低的字段不建索引（如性别、status 只有几个值，索引效果差）
- 频繁更新的字段谨慎建索引（更新成本高）
- 单表索引数量控制在 5 个以内

---

## 五、联合索引与最左前缀原则

### 什么是联合索引？

联合索引是多个字段组成的索引：
```sql
KEY idx_user_status (user_id, status)
```

这个索引先按 `user_id` 排序，`user_id` 相同的再按 `status` 排序。

### 最左前缀原则

联合索引遵循**最左前缀原则**：查询时必须从索引的最左列开始，才能命中索引。

对于 `idx_user_status (user_id, status)`：

| 查询条件 | 是否命中索引 | 原因 |
|---------|------------|------|
| `WHERE user_id = 1` | ✅ 命中 | 用了最左列 user_id |
| `WHERE user_id = 1 AND status = 'BORROWED'` | ✅ 命中 | 用了 user_id 和 status，完全匹配 |
| `WHERE status = 'BORROWED'` | ❌ 不命中 | 没用最左列 user_id，跳过了最左列 |
| `WHERE user_id = 1 OR status = 'BORROWED'` | ❌ 不命中 | OR 条件，索引失效 |

### 项目里为什么没有联合索引？

项目里都是单列索引，没有联合索引。但实际业务中，有些查询可以用联合索引优化：

比如"查询某个用户借阅中的记录"：
```sql
SELECT * FROM borrow_record WHERE user_id = 1 AND status = 'BORROWED';
```
现在有 `idx_user_id` 和 `idx_status` 两个单列索引，MySQL 只会选其中一个（一般选区分度高的 user_id），然后回表过滤 status。

如果建联合索引 `idx_user_status (user_id, status)`，可以直接在索引里完成过滤，不需要回表，性能更好。

这是项目可以优化的点之一。作为学习项目，单列索引足够理解概念；生产项目应该根据实际查询模式建联合索引。

### 覆盖索引

如果查询的所有字段都在索引里，不需要回表查询数据行，这叫**覆盖索引**，性能最好。

比如：
```sql
SELECT user_id, status FROM borrow_record WHERE user_id = 1;
```
如果有联合索引 `idx_user_status (user_id, status)`，查询的 user_id 和 status 都在索引里，直接从索引返回，不需要回表，这就是覆盖索引。

---

## 六、逻辑删除

### 什么是逻辑删除？

逻辑删除不是真的 `DELETE` 数据，而是用一个字段标记"已删除"：
- 查询时自动加 `WHERE deleted = 0`
- 删除时执行 `UPDATE ... SET deleted = 1`
- 数据还在数据库里

项目里每张表都有 `deleted TINYINT DEFAULT 0` 字段，配合 MyBatis-Plus 的配置：
```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 为什么用逻辑删除？

1. **数据可恢复**：误删了可以恢复（把 deleted 改回 0），不需要找数据库备份
2. **保留历史关联**：借阅记录关联了图书和用户，如果物理删除图书，借阅记录就查不到图书信息了。逻辑删除后图书还在，借阅记录仍能关联查询
3. **审计需求**：知道谁删了什么、什么时候删的（如果有操作人字段）
4. **避免物理删除的性能问题**：大表 DELETE 可能锁表、产生大量 binlog、碎片

### 逻辑删除的注意事项

1. **唯一索引冲突**：如果 username 建了唯一索引，删除一个用户后（deleted=1），再新增同名用户会报唯一索引冲突。解决：唯一索引改成 `(username, deleted)` 联合唯一索引，或者用删除时间戳标记
2. **自定义 SQL 要手动加 deleted=0**：MyBatis-Plus 只对 BaseMapper 通用方法自动加逻辑删除条件，自定义 SQL（@Select/@Update）必须手动加 `AND deleted = 0`
3. **查询量增大**：逻辑删除后数据不会真正减少，表越来越大，查询性能下降。可以定期归档真正删除历史数据
4. **统计要注意**：`COUNT(*)` 会包含已删除的数据，MyBatis-Plus 的 `selectCount` 会自动加 deleted=0，但手写 SQL 要注意

---

## 七、数据库三大范式

### 第一范式（1NF）：原子性

每个字段的值都是不可再分的原子值。

反例（不符合 1NF）：
```sql
CREATE TABLE book (
    id BIGINT,
    info VARCHAR(500)  -- "Java核心技术,Cay S. Horstmann,机械工业出版社" 多个值塞一个字段
);
```

正例（符合 1NF）：项目里的设计，title、author、publisher 各是一个字段。

### 第二范式（2NF）：消除部分依赖

在满足 1NF 的基础上，非主键字段必须完全依赖于整个主键，不能只依赖主键的一部分（主要针对联合主键）。

反例（不符合 2NF）：
```sql
-- 联合主键 (user_id, book_id)，但 book_title 只依赖 book_id，不依赖 user_id
CREATE TABLE borrow_record (
    user_id BIGINT,
    book_id BIGINT,
    book_title VARCHAR(200),  -- 只依赖 book_id，部分依赖
    borrow_date DATE,
    PRIMARY KEY (user_id, book_id)
);
```

正例（符合 2NF）：项目里借阅记录只存 `book_id`，不存 `book_title`，图书信息从 book 表关联查询。

### 第三范式（3NF）：消除传递依赖

在满足 2NF 的基础上，非主键字段必须直接依赖于主键，不能依赖于其他非主键字段（传递依赖）。

反例（不符合 3NF）：
```sql
CREATE TABLE book (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200),
    category_id BIGINT,
    category_name VARCHAR(50)  -- 依赖 category_id，而 category_id 依赖 id，传递依赖
);
```

正例（符合 3NF）：项目里 book 表只存 `category_id`，分类名称从 book_category 表关联查询。

### 项目符合第几范式？

项目的 4 张表设计符合**第三范式（3NF）**：
- 每个字段都是原子值（1NF）
- 非主键字段完全依赖主键（2NF）
- 非主键字段直接依赖主键，没有传递依赖（3NF）

分类名称、用户名、书名这些关联信息都不冗余存储，通过 ID 关联查询。

### 反范式化（适度冗余）

严格遵循 3NF 会导致查询时需要大量 JOIN，性能下降。实际项目中会**适度反范式化**，在从表冗余一些主表的字段，减少 JOIN。

项目里 `BorrowRecordVO` 在 Service 层查询时补充了 `username`、`bookTitle`（不是数据库冗余，是查询时组装的），这是一种折中。

如果借阅记录量很大，每次查询都 JOIN 用户表和图书表性能差，可以考虑在 borrow_record 表冗余 `username`、`book_title` 字段（反范式化），但要注意数据一致性（用户改了用户名，借阅记录里的冗余字段要不要更新）。

---

## 八、EXPLAIN 与 SQL 优化

### 怎么用 EXPLAIN？

在 SELECT 语句前加 `EXPLAIN`，MySQL 会显示执行计划而不是执行 SQL：

```sql
EXPLAIN SELECT * FROM book WHERE title LIKE '%Java%';
```

### 关键输出字段

| 字段 | 含义 | 关注点 |
|------|------|--------|
| `type` | 访问类型 | 性能从好到差：system > const > eq_ref > ref > range > index > ALL |
| `key` | 实际使用的索引 | NULL 表示没走索引 |
| `rows` | 预估扫描的行数 | 越小越好 |
| `Extra` | 额外信息 | `Using index`（覆盖索引，好）、`Using where`（回表过滤）、`Using filesort`（文件排序，差）、`Using temporary`（临时表，差） |

### type 字段详解

| type | 含义 | 场景 |
|------|------|------|
| `system` | 系统表，只有一行 | 极少 |
| `const` | 主键或唯一索引等值查询，最多一行 | `WHERE id = 1` |
| `eq_ref` | 联表查询时，用主键或唯一索引关联 | JOIN 时用主键关联 |
| `ref` | 非唯一索引等值查询 | `WHERE category_id = 1` |
| `range` | 索引范围查询 | `WHERE id IN (1,2,3)`、`WHERE price > 50` |
| `index` | 全索引扫描 | 扫描整个索引树，比全表扫描快（索引比数据小） |
| `ALL` | 全表扫描 | 没有索引，性能最差，要避免 |

### 项目里的 SQL 优化分析

**查询1：按 ID 查图书**
```sql
EXPLAIN SELECT * FROM book WHERE id = 1;
-- type=const, key=PRIMARY, rows=1
-- 主键等值查询，性能最好
```

**查询2：按分类查图书**
```sql
EXPLAIN SELECT * FROM book WHERE category_id = 1;
-- type=ref, key=idx_category_id, rows=N
-- 非唯一索引等值查询，性能好
```

**查询3：按书名模糊查询**
```sql
EXPLAIN SELECT * FROM book WHERE title LIKE '%Java%';
-- type=ALL, key=NULL, rows=全表
-- LIKE 以 % 开头，索引失效，全表扫描！
```

**问题**：`title LIKE '%Java%'` 左边有 `%`，索引失效，全表扫描。这是项目里的一个性能问题。

**解决方案**：
1. 如果是前缀匹配（`LIKE 'Java%'`），索引有效
2. 如果是中间匹配（`LIKE '%Java%'`），用全文索引（FULLTEXT）或搜索引擎（Elasticsearch）
3. 数据量小时全表扫描也可以接受，项目是学习项目，数据量小，问题不大

**查询4：分页查询**
```sql
EXPLAIN SELECT * FROM book WHERE deleted = 0 ORDER BY create_time DESC LIMIT 0, 10;
-- 如果 create_time 没有索引，type=ALL, Extra=Using filesort
-- 项目里 create_time 没有建索引，深分页时性能差
```

**优化**：给 `create_time` 建索引，或者用主键排序（主键默认有索引）。

### SQL 优化常见手段

1. **建索引**：高频查询字段、JOIN 字段、ORDER BY/GROUP BY 字段建索引
2. **避免索引失效**：
   - 不要在索引字段上用函数（`WHERE YEAR(create_time) = 2024` → 改成范围查询）
   - 不要用 `!=`、`<>`、`OR`（可能失效）
   - `LIKE` 不要以 `%` 开头
   - 字符串不加引号（隐式类型转换导致索引失效）
3. **用覆盖索引**：查询字段都在索引里，避免回表
4. **避免 SELECT ***：只查需要的字段，减少数据传输和回表
5. **LIMIT 深分页优化**：`LIMIT 100000, 10` 慢，用子查询或游标分页
6. **JOIN 优化**：小表驱动大表，JOIN 字段建索引，避免超过 3 张表 JOIN

---

## 九、本课必须记住的 7 件事

1. **4 张表关系**：用户 1:N 借阅记录 N:1 图书 N:1 分类，借阅记录是用户和图书的中间表（多对多）
2. **字段类型选择**：主键 BIGINT、状态 TINYINT、金额 DECIMAL(10,2)、时间 DATETIME/DATE、字符串 VARCHAR、长文本 TEXT
3. **索引类型**：主键索引（PRIMARY KEY）、唯一索引（UNIQUE KEY）、普通索引（KEY）、联合索引；索引加速查询但降低写入性能、占空间
4. **联合索引最左前缀原则**：查询必须从索引最左列开始才能命中，跳过最左列索引失效
5. **逻辑删除**：用 deleted 字段标记删除而非物理 DELETE，数据可恢复、保留历史关联，但要注意唯一索引冲突和自定义 SQL 手动加 deleted=0
6. **三大范式**：1NF 原子性、2NF 消除部分依赖、3NF 消除传递依赖；项目符合 3NF，实际项目会适度反范式化减少 JOIN
7. **EXPLAIN 优化**：看 type（避免 ALL）、key（是否走索引）、rows（扫描行数）、Extra（避免 Using filesort/temporary）；常见索引失效场景：函数、!=、OR、LIKE %xx

---

## 十、本节关键代码

```sql
-- 图书表：典型的企业项目表设计
CREATE TABLE book (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键，BIGINT防溢出',
    isbn          VARCHAR(20)   NOT NULL COMMENT 'ISBN，唯一索引',
    title         VARCHAR(200)  NOT NULL COMMENT '书名，普通索引',
    author        VARCHAR(100)  NOT NULL COMMENT '作者，普通索引',
    publisher     VARCHAR(100)  DEFAULT NULL,
    publish_date  DATE          DEFAULT NULL COMMENT '只用日期，不用DATETIME',
    category_id   BIGINT        DEFAULT NULL COMMENT '逻辑外键，普通索引，无物理外键约束',
    price         DECIMAL(10,2) DEFAULT NULL COMMENT '金额用DECIMAL精确存储',
    stock         INT           NOT NULL DEFAULT 0,
    total_stock   INT           NOT NULL DEFAULT 0,
    description   TEXT          DEFAULT NULL COMMENT '长文本用TEXT',
    cover_url     VARCHAR(500)  DEFAULT NULL,
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，自动填充',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，自动刷新',
    deleted       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删，1已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_isbn (isbn),          -- 唯一索引：ISBN不重复
    KEY idx_title (title),               -- 普通索引：按书名查询
    KEY idx_author (author),             -- 普通索引：按作者查询
    KEY idx_category_id (category_id)    -- 普通索引：按分类筛选
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书表';
```

---

## 十一、本节练习

### 练习1：用 EXPLAIN 分析项目里的查询

启动项目和 MySQL，执行以下 SQL，观察 EXPLAIN 输出：
```sql
-- 1. 主键查询（应该是 const）
EXPLAIN SELECT * FROM book WHERE id = 1;

-- 2. 索引查询（应该是 ref）
EXPLAIN SELECT * FROM book WHERE category_id = 1;

-- 3. 模糊查询（左边有%，索引失效，应该是 ALL）
EXPLAIN SELECT * FROM book WHERE title LIKE '%Java%';

-- 4. 模糊查询（右边有%，索引有效，应该是 range）
EXPLAIN SELECT * FROM book WHERE title LIKE 'Java%';

-- 5. 排序查询（观察是否有 Using filesort）
EXPLAIN SELECT * FROM book WHERE deleted = 0 ORDER BY create_time DESC LIMIT 10;
```

记录每个查询的 type、key、rows、Extra，分析性能差异。

### 练习2：给借阅记录表加联合索引

当前 borrow_record 表有 `idx_user_id` 和 `idx_status` 两个单列索引。请：
1. 添加联合索引 `idx_user_status (user_id, status)`
2. 用 EXPLAIN 分析 `SELECT * FROM borrow_record WHERE user_id = 2 AND status = 'BORROWED'`，观察 key 是不是用了联合索引
3. 再分析 `SELECT * FROM borrow_record WHERE status = 'BORROWED'`（不用 user_id），观察索引是否失效（最左前缀原则）

### 练习3：设计一个"图书评论"表

假设要给项目加一个图书评论功能，用户可以对图书发表评论。请设计一张 `book_comment` 表，要求：
- 包含主键、用户ID、图书ID、评论内容、评分（1-5星）、创建时间、逻辑删除
- 选择合适的字段类型
- 建立合适的索引（考虑查询场景：查某本书的评论、查某个用户的评论）
- 说明表关系（评论和用户、图书的关系）
- 写出完整的 CREATE TABLE 语句

---

## 十二、自测题

### Q1：项目里的 4 张表是什么关系？为什么没有物理外键？

<details>
<summary>点击查看答案</summary>

4 张表的关系：
- **用户（sys_user） 1:N 借阅记录（borrow_record）**：一个用户可以借多本书
- **图书（book） 1:N 借阅记录（borrow_record）**：一本书可以被多次借阅
- **分类（book_category） 1:N 图书（book）**：一个分类下有多本图书
- **用户 N:M 图书**：通过借阅记录表（中间表）实现多对多关系

没有物理外键（FOREIGN KEY 约束）的原因：
1. **性能**：外键约束在每次 INSERT/UPDATE/DELETE 时都要检查关联表，有性能开销
2. **分库分表**：物理外键在分布式、分库分表场景下无法使用
3. **灵活性**：数据迁移、批量操作、软删除时外键约束会碍事
4. **应用层保证**：数据一致性由 Service 层保证（如删除分类前检查是否有图书引用），不依赖数据库约束
5. 这是互联网企业项目的常见做法，传统企业项目可能会用物理外键

项目里的 user_id、book_id、category_id 都是逻辑外键（普通字段+索引），没有物理约束。

</details>

### Q2：主键为什么用 BIGINT 而不是 INT？金额为什么用 DECIMAL 而不是 FLOAT？

<details>
<summary>点击查看答案</summary>

**主键用 BIGINT 的原因**：
- INT 是 4 字节，范围约 -21亿 ~ 21亿，看起来大但高并发业务、分布式系统可能超过上限
- BIGINT 是 8 字节，范围 -922亿亿 ~ 922亿亿，永远用不完
- 现在存储成本低，多 4 字节不是问题
- 企业项目规范：主键一律用 BIGINT，避免未来数据量增长后改字段类型的麻烦

**金额用 DECIMAL 的原因**：
- FLOAT/DOUBLE 是二进制浮点数，存在精度损失（如 0.1 + 0.2 在计算机里可能等于 0.30000000000000004），不适合金额
- DECIMAL 是定点数，用十进制精确存储，不会有精度损失
- `DECIMAL(10,2)` 表示最多 10 位数字，其中 2 位小数，范围 -99999999.99 ~ 99999999.99，适合图书价格和罚款
- 金额、价格、利率等对精度要求高的字段必须用 DECIMAL

</details>

### Q3：索引有哪些类型？联合索引的最左前缀原则是什么？

<details>
<summary>点击查看答案</summary>

**索引类型**：
1. **主键索引**（PRIMARY KEY）：主键自带，唯一且非空，InnoDB 中是聚簇索引（数据和索引存在一起）
2. **唯一索引**（UNIQUE KEY）：值唯一，允许 NULL，保证数据唯一性，如 username、isbn
3. **普通索引**（KEY/INDEX）：加速查询，没有唯一性约束，如 title、author、category_id
4. **联合索引**：多个字段组成的索引，如 `idx_user_status (user_id, status)`
5. **全文索引**（FULLTEXT）：用于文本搜索，项目里没用

**最左前缀原则**：联合索引查询时必须从索引的最左列开始，才能命中索引。
对于 `idx_user_status (user_id, status)`：
- `WHERE user_id = 1` → 命中（用了最左列）
- `WHERE user_id = 1 AND status = 'BORROWED'` → 命中（完全匹配）
- `WHERE status = 'BORROWED'` → 不命中（跳过了最左列 user_id）
- `WHERE user_id = 1 OR status = 'BORROWED'` → 不命中（OR 导致索引失效）

索引字段的顺序很重要，区分度高的字段放前面。

</details>

### Q4：什么是逻辑删除？和物理删除有什么区别？逻辑删除要注意什么？

<details>
<summary>点击查看答案</summary>

**逻辑删除**：不是真的 DELETE 数据，而是用一个字段（如 deleted）标记"已删除"。查询时自动加 WHERE deleted=0，删除时执行 UPDATE SET deleted=1，数据还在数据库里。

**物理删除**：执行 DELETE 语句，数据从数据库中真正移除，不可恢复。

**逻辑删除的优点**：
1. 数据可恢复（误删了把 deleted 改回 0）
2. 保留历史关联（借阅记录关联的图书被删后仍能查到信息）
3. 避免大表 DELETE 的性能问题（锁表、binlog、碎片）
4. 满足审计需求

**逻辑删除的注意事项**：
1. **唯一索引冲突**：username 建了唯一索引，删除用户后再新增同名用户会冲突。解决：唯一索引改成 (username, deleted) 联合，或用删除时间戳
2. **自定义 SQL 手动加 deleted=0**：MyBatis-Plus 只对 BaseMapper 通用方法自动加逻辑删除条件，自定义 SQL（@Select/@Update）必须手动加 AND deleted=0，否则会查到已删除数据
3. **数据量持续增长**：数据不会真正减少，表越来越大。可以定期归档/物理删除历史数据
4. **统计注意**：手写 COUNT(*) 会包含已删除数据，要加 deleted=0 条件
5. **查询性能**：每个查询都多一个 deleted=0 条件，要确保索引包含 deleted 字段或用联合索引

</details>

### Q5：EXPLAIN 的输出里 type、key、rows、Extra 分别看什么？哪些情况需要优化？

<details>
<summary>点击查看答案</summary>

**EXPLAIN 关键字段**：
1. **type（访问类型）**：性能从好到差：system > const > eq_ref > ref > range > index > ALL。**ALL（全表扫描）必须优化**，index（全索引扫描）也要看情况优化
2. **key（实际使用的索引）**：NULL 表示没走索引，需要优化；应该和预期的索引一致
3. **rows（预估扫描行数）**：越小越好，rows 很大说明扫描了太多数据，需要加索引或优化条件
4. **Extra（额外信息）**：
   - `Using index`：覆盖索引，好现象，不需要回表
   - `Using where`：回表后过滤，正常
   - `Using filesort`：文件排序，差，需要给排序字段建索引
   - `Using temporary`：用了临时表，差，常见于 GROUP BY/DISTINCT，需要优化
   - `Using index condition`：索引条件下推，好现象

**需要优化的情况**：
- type = ALL（全表扫描）
- key = NULL（没走索引）
- rows 很大（扫描行数多）
- Extra 有 Using filesort 或 Using temporary

**常见优化手段**：加索引、避免索引失效（函数、!=、OR、LIKE %xx）、用覆盖索引、避免 SELECT *、深分页优化、JOIN 字段建索引。

</details>

---

## 十三、面试题

### 面试题1：你们项目的数据库表是怎么设计的？索引怎么建的？

> **答题要点**：
> 1. **4 张核心表**：
>    - `sys_user`（用户表）：id(BIGINT主键)、username(VARCHAR50唯一索引)、password(VARCHAR100 BCrypt)、role、status(TINYINT)、email/phone(普通索引)、create_time/update_time/deleted
>    - `book_category`（图书分类表）：id、name(VARCHAR50唯一索引)、sort、description
>    - `book`（图书表）：id、isbn(VARCHAR20唯一索引)、title(VARCHAR200普通索引)、author(普通索引)、category_id(普通索引，逻辑外键)、price(DECIMAL10,2)、stock/total_stock(INT)、description(TEXT)、publish_date(DATE)
>    - `borrow_record`（借阅记录表）：id、user_id(普通索引)、book_id(普通索引)、borrow_date/due_date/return_date(DATE)、status(VARCHAR20普通索引)、fine(DECIMAL10,2)
> 2. **表关系**：用户1:N借阅记录N:1图书N:1分类，借阅记录是用户和图书的中间表（多对多）。没有物理外键，用逻辑外键（字段+索引），应用层保证一致性
> 3. **索引策略**：
>    - 主键：每张表 id BIGINT 自增主键
>    - 唯一索引：username、isbn、category_name（保证数据唯一性+加速查询）
>    - 普通索引：高频查询字段（title、author、category_id、user_id、book_id、status）
>    - 没有建联合索引（学习项目简化，生产项目应根据查询模式建联合索引如 idx_user_status）
> 4. **通用字段**：每张表都有 id、create_time（DEFAULT CURRENT_TIMESTAMP）、update_time（ON UPDATE CURRENT_TIMESTAMP）、deleted（TINYINT逻辑删除），符合企业项目规范
> 5. **字段类型**：主键BIGINT、状态TINYINT、金额DECIMAL(10,2)、日期DATETIME/DATE、字符串VARCHAR、长文本TEXT，选择合理

### 面试题2：索引的底层原理是什么？B+ 树和 B 树、Hash 索引有什么区别？

> **答题要点**：
> 1. **索引的作用**：加速查询，类似书的目录。没有索引时查询要全表扫描，有索引时通过索引快速定位
> 2. **InnoDB 索引用 B+ 树**：
>    - B+ 树是多路平衡查找树，非叶子节点只存索引键和指针，不存数据
>    - 叶子节点存所有数据（聚簇索引）或主键值（二级索引），叶子节点之间用双向链表连接
>    - 特点：树高低（一般 3-4 层就能存千万级数据），查询稳定，范围查询方便（叶子节点链表）
> 3. **聚簇索引 vs 二级索引**：
>    - 聚簇索引：主键索引，叶子节点存整行数据，一张表只有一个
>    - 二级索引（非主键索引）：叶子节点存主键值，查询时先查二级索引得到主键，再用主键查聚簇索引（回表）
> 4. **B+ 树 vs B 树**：
>    - B 树的非叶子节点也存数据，B+ 树非叶子节点只存索引键 → B+ 树单个节点能存更多键，树更矮，IO 更少
>    - B+ 树叶子节点有双向链表，B 树没有 → B+ 树范围查询、排序查询更方便
>    - B+ 树查询必须走到叶子节点，查询更稳定；B 树可能在非叶子节点就找到
> 5. **B+ 树 vs Hash 索引**：
>    - Hash 索引查询速度 O(1)，但只支持等值查询（=），不支持范围查询、排序、模糊查询
>    - B+ 树支持等值、范围、排序、模糊查询，通用性强
>    - InnoDB 默认用 B+ 树，Memory 引擎支持 Hash 索引
> 6. **回表**：通过二级索引查到主键值，再用主键值查聚簇索引获取完整数据。覆盖索引（查询字段都在索引里）可以避免回表

### 面试题3：什么是最左前缀原则？哪些情况会导致索引失效？

> **答题要点**：
> 1. **最左前缀原则**：联合索引（如 idx_a_b_c (a,b,c)）查询时必须从最左列开始，才能命中索引。可以是 a、a+b、a+b+c，但不能跳过 a 直接查 b 或 c
> 2. **索引失效的常见场景**：
>    - **违反最左前缀**：联合索引跳过最左列，如 idx(a,b) 只查 WHERE b=1
>    - **索引列上用函数/运算**：WHERE YEAR(create_time)=2024、WHERE price+1=100 → 改成范围查询 WHERE create_time BETWEEN '2024-01-01' AND '2024-12-31'
>    - **隐式类型转换**：字符串字段不加引号，如 WHERE phone=13800000000（phone 是 VARCHAR）→ 加引号 WHERE phone='13800000000'
>    - **LIKE 以 % 开头**：WHERE name LIKE '%Java%' → 前缀匹配 LIKE 'Java%' 可以用索引，或用全文索引/搜索引擎
>    - **用 !=、<>、NOT IN**：可能导致索引失效（优化器可能选择全表扫描）
>    - **OR 连接的条件**：WHERE a=1 OR b=2，如果 a 和 b 不是都有索引，可能失效 → 改成 UNION 或分别建索引
>    - **IS NULL / IS NOT NULL**：可能失效（取决于数据分布）
>    - **数据量小**：表数据很少时，优化器认为全表扫描比走索引快，选择全表扫描
> 3. **验证索引是否生效**：用 EXPLAIN 查看 type、key 字段，type=ALL 或 key=NULL 说明索引失效
> 4. **优化建议**：建索引后用 EXPLAIN 验证，避免上述失效场景，联合索引字段顺序按区分度从高到低、按查询频率排列

---

## 阶段一完成！

恭喜你完成了**阶段一：看懂项目**的全部 10 课！

到这里，你应该已经能：
- 说清楚一个请求从前端到数据库的完整链路
- 理解 Spring Boot 启动原理和自动配置
- 看懂 Controller、Service、Mapper 每一层的代码
- 理解 IOC/DI、事务、MyBatis-Plus 的核心原理
- 理解 Entity/DTO/VO 三层对象设计
- 理解统一返回格式和全局异常处理
- 理解 JWT 认证的完整流程
- 看懂数据库表设计和索引

### 接下来的学习路线

- **阶段二：能修改项目**（第11-16课）：Redis 缓存实战、RabbitMQ 消息队列、React 前端入门、前后端联调、参数校验、分页查询
- **阶段三：能扩展功能**（第17-20课）：新增功能全链路、Spring AOP、Docker 部署、Knife4j 接口文档
- **阶段四：能独立开发 + 面试**（第21-25课）：Code Review、SQL 优化、Redis 进阶、RabbitMQ 进阶、面试高频问题
- **阶段五：AI 应用开发**：Spring AI、RAG、Tool Calling、Agent

如果要继续生成后续课程，告诉我"继续阶段二"即可。
