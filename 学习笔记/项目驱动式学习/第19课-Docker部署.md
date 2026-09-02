# 第19课：Docker 部署——Dockerfile、docker-compose、容器化部署全流程

> **本课目标**：理解 Docker 的核心概念，学会写 Dockerfile 和 docker-compose，把项目完整容器化部署。学完这课，你应该能独立部署一个前后端分离的 Java 项目。

---

## 一、什么是 Docker？为什么需要容器化？

### 问题场景

"在我电脑上能跑啊，怎么到你那就不行了？"

这是开发中最常见的问题之一。原因：
- 开发环境是 Windows，生产环境是 Linux
- 开发用 JDK 17，生产环境是 JDK 8
- 开发用 MySQL 8.0，生产环境是 MySQL 5.7
- 缺少依赖库、环境变量配置不同、端口冲突

### Docker 的解决方案

Docker 把应用和它的所有依赖（运行环境、库、配置）打包成一个**镜像（Image）**，在任何安装了 Docker 的机器上都能以相同的方式运行。

就像 shipping container（集装箱）：不管里面装的是什么，码头都用统一的方式装卸。Docker 容器就是软件的"集装箱"。

### Docker 的核心优势

| 优势 | 说明 |
|------|------|
| **环境一致** | 开发、测试、生产环境完全一致，"在我电脑上能跑"的问题消失 |
| **快速部署** | 镜像秒级启动，不需要手动安装环境 |
| **隔离性** | 每个容器独立运行，互不影响（一个容器挂了不影响其他） |
| **可移植** | 镜像可以在任何 Docker 环境运行，跨云、跨平台 |
| **版本管理** | 镜像有版本，可以回滚、追溯 |
| **资源高效** | 容器共享宿主机内核，比虚拟机轻量（启动秒级，占用 MB 级） |

### Docker vs 虚拟机

| | Docker 容器 | 虚拟机（VM） |
|---|------------|-------------|
| 架构 | 共享宿主机内核，容器级隔离 | 完整的操作系统，硬件级虚拟化 |
| 启动速度 | 秒级（1-5秒） | 分钟级（30秒-几分钟） |
| 资源占用 | MB 级（几十 MB） | GB 级（几 GB） |
| 性能 | 接近原生（几乎无损耗） | 有虚拟化损耗（5-20%） |
| 隔离性 | 进程级隔离（共享内核） | 完全隔离（独立内核） |

项目用 Docker 主要是为了**快速搭建开发环境**（MySQL、Redis、RabbitMQ 一键启动）和**生产部署**（应用容器化）。

---

## 二、Docker 核心概念

| 概念 | 说明 | 类比 |
|------|------|------|
| **镜像（Image）** | 应用+依赖的只读模板，包含运行应用所需的一切 | 菜谱 / 类（Class） |
| **容器（Container）** | 镜像的运行实例，是一个独立运行的环境 | 按菜谱做的菜 / 对象（Instance） |
| **Dockerfile** | 构建镜像的脚本，定义镜像里有什么、怎么构建 | 菜谱的制作步骤 |
| **仓库（Registry）** | 存储和分发镜像的地方（Docker Hub、阿里云镜像仓库） | 应用商店 |
| **docker-compose** | 多容器编排工具，用一个 YAML 文件定义和运行多个容器 | 一桌菜的菜单（同时做多道菜） |

### 镜像和容器的关系

```
Dockerfile --build--> 镜像(Image) --run--> 容器(Container)
                         │
                         ├─ 只读模板
                         ├─ 包含 JDK + 应用 jar + 配置
                         └─ 可以有多个版本（tag）
```

一个镜像可以启动多个容器（比如同一个 MySQL 镜像启动 3 个不同的数据库容器）。

---

## 三、项目里的 docker-compose.yml 详解

项目根目录的 `docker-compose.yml` 定义了三个基础服务：MySQL、Redis、RabbitMQ。

### 3.1 整体结构

```yaml
version: '3.8'  # docker-compose 版本

services:        # 定义的服务（容器）列表
  mysql: ...     # MySQL 服务
  redis: ...     # Redis 服务
  rabbitmq: ...  # RabbitMQ 服务

volumes:         # 数据卷（持久化数据）
  mysql_data: ...
  redis_data: ...
  rabbitmq_data: ...

networks:        # 网络（容器间通信）
  library-network: ...
```

### 3.2 MySQL 服务详解

```yaml
mysql:
  image: mysql:8.0                          # 使用的镜像（MySQL 8.0）
  container_name: library-mysql             # 容器名称
  restart: unless-stopped                   # 重启策略（除非手动停止，否则自动重启）
  environment:                               # 环境变量
    MYSQL_ROOT_PASSWORD: ll521521           # root 密码
    MYSQL_DATABASE: library_db               # 自动创建的数据库
    MYSQL_USER: library                       # 自动创建的用户
    MYSQL_PASSWORD: ll521521                 # 用户密码
    TZ: Asia/Shanghai                         # 时区
  ports:
    - "3306:3306"                            # 端口映射（宿主机端口:容器端口）
  volumes:
    - mysql_data:/var/lib/mysql              # 数据卷：持久化 MySQL 数据
    - ./src/main/resources/db/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro  # 挂载初始化脚本
  command:                                     # 启动命令参数
    - --character-set-server=utf8mb4
    - --collation-server=utf8mb4_unicode_ci
    - --default-authentication-plugin=mysql_native_password
  healthcheck:                                 # 健康检查
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-pll521521"]
    interval: 10s    # 每10秒检查一次
    timeout: 5s      # 超时5秒
    retries: 5       # 重试5次后标记为不健康
  networks:
    - library-network
```

**关键点说明**：

1. **image: mysql:8.0**：从 Docker Hub 拉取 MySQL 8.0 官方镜像。`8.0` 是 tag（版本），也可以用 `mysql:latest`（最新版，不推荐，版本不确定）。

2. **ports: "3306:3306"**：把容器的 3306 端口映射到宿主机的 3306 端口。这样宿主机上的应用（Spring Boot）可以通过 `localhost:3306` 访问容器里的 MySQL。
   - 格式：`宿主机端口:容器端口`
   - 如果宿主机 3306 被占用，可以改成 `"3307:3306"`（宿主机用 3307）

3. **volumes**：
   - `mysql_data:/var/lib/mysql`：把 MySQL 的数据目录挂载到数据卷，容器删除后数据不丢失（持久化）
   - `./schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro`：把宿主机的 schema.sql 挂载到容器的初始化目录，MySQL 首次启动时会自动执行这个脚本（建表+初始化数据）。`:ro` 表示只读（read-only）

4. **environment**：MySQL 镜像的环境变量，用于初始化：
   - `MYSQL_ROOT_PASSWORD`：root 用户密码（必填）
   - `MYSQL_DATABASE`：自动创建的数据库名
   - `MYSQL_USER` / `MYSQL_PASSWORD`：自动创建的普通用户和密码

5. **healthcheck**：健康检查，Docker 定期执行命令判断容器是否健康。健康检查通过后，依赖这个服务的其他服务才会启动（`depends_on: condition: service_healthy`）。

6. **restart: unless-stopped**：重启策略：
   - `no`：不自动重启（默认）
   - `always`：总是自动重启
   - `unless-stopped`：除非手动停止，否则自动重启（推荐）
   - `on-failure`：失败时自动重启

### 3.3 Redis 服务

```yaml
redis:
  image: redis:7-alpine                              # Redis 7，alpine 版本（体积小）
  container_name: library-redis
  restart: unless-stopped
  ports:
    - "6379:6379"
  volumes:
    - redis_data:/data                                # 持久化数据
  command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]               # 用 redis-cli ping 检查
    interval: 10s
    timeout: 5s
    retries: 5
  networks:
    - library-network
```

**关键点**：
- `redis:7-alpine`：alpine 是精简版 Linux，镜像体积小（几十 MB vs 几百 MB）
- `command: redis-server --appendonly yes`：启动参数，开启 AOF 持久化（数据不丢失）
- `--maxmemory 256mb`：最大内存 256MB
- `--maxmemory-policy allkeys-lru`：内存满时淘汰策略（LRU，最近最少使用的 key 被淘汰）

### 3.4 RabbitMQ 服务

```yaml
rabbitmq:
  image: rabbitmq:3.12-management-alpine             # 带管理界面的版本
  container_name: library-rabbitmq
  restart: unless-stopped
  environment:
    RABBITMQ_DEFAULT_USER: guest
    RABBITMQ_DEFAULT_PASS: guest
    RABBITMQ_DEFAULT_VHOST: /
  ports:
    - "5672:5672"      # AMQP 协议端口（应用连接）
    - "15672:15672"    # 管理界面端口（浏览器访问 http://localhost:15672）
  volumes:
    - rabbitmq_data:/var/lib/rabbitmq
  healthcheck:
    test: ["CMD", "rabbitmq-diagnostics", "check_port_connectivity"]
    interval: 10s
    timeout: 5s
    retries: 5
  networks:
    - library-network
```

**关键点**：
- `rabbitmq:3.12-management-alpine`：`management` 标签表示带管理界面（Web UI），可以在浏览器里查看队列、消息、消费者等
- 两个端口：`5672` 是应用连接的 AMQP 端口，`15672` 是管理界面端口
- 默认账号密码：guest/guest（生产环境应该改）

### 3.5 数据卷和网络

```yaml
volumes:
  mysql_data:       # MySQL 数据卷
    driver: local
  redis_data:       # Redis 数据卷
    driver: local
  rabbitmq_data:    # RabbitMQ 数据卷
    driver: local

networks:
  library-network:  # 自定义网络，容器间可以用服务名互相访问
    driver: bridge
```

**数据卷（Volume）**：Docker 的持久化机制，数据存在宿主机上，容器删除后数据不丢失。如果不用数据卷，容器删除后 MySQL 的数据就没了。

**网络（Network）**：同一个网络下的容器可以用**服务名**互相访问。比如应用容器里可以用 `mysql:3306` 访问 MySQL 容器（不需要 IP），因为 Docker 的 DNS 会把服务名解析成容器 IP。

这就是为什么 docker-compose 里应用连接数据库用 `jdbc:mysql://mysql:3306/library_db`（主机名是 `mysql`，即服务名），而不是 `localhost`。

---

## 四、写后端应用的 Dockerfile

项目里目前只有基础设施的 docker-compose，后端应用是在宿主机上运行的（`mvnw spring-boot:run`）。下面教你怎么把后端应用也容器化。

### 4.1 Dockerfile

在项目根目录创建 `Dockerfile`：

```dockerfile
# ============================================================
# 多阶段构建：第一阶段编译打包
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 先复制 pom.xml，利用 Docker 缓存（依赖不变时不需要重新下载）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并打包
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
# 第二阶段：运行镜像（只包含 JRE + jar 包，体积小）
# ============================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 从构建阶段复制 jar 包
COPY --from=builder /app/target/library-management-0.0.1-SNAPSHOT.jar app.jar

# 时区
RUN apk add --no-cache tzdata
ENV TZ=Asia/Shanghai

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 4.2 多阶段构建说明

**为什么用多阶段构建？**

如果只用一个阶段（`FROM maven:3.9` 然后运行 jar），镜像里会包含 Maven、JDK、源码、编译产物，体积很大（可能 1GB+）。

多阶段构建：
- **第一阶段（builder）**：用 Maven 镜像编译打包，生成 jar 文件
- **第二阶段（运行）**：用 JRE 镜像（只有运行环境，没有编译工具），只复制 jar 文件

最终镜像只有 JRE + jar，体积小很多（几百 MB → 一百多 MB）。

### 4.3 Dockerfile 指令说明

| 指令 | 说明 |
|------|------|
| `FROM` | 基础镜像，每个 Dockerfile 必须以 FROM 开头 |
| `WORKDIR` | 工作目录，后续命令都在这个目录下执行 |
| `COPY` | 复制文件到镜像里 |
| `RUN` | 执行命令（构建时执行，如编译、安装依赖） |
| `ENV` | 设置环境变量 |
| `EXPOSE` | 声明容器监听的端口（只是文档，实际映射靠 -p 或 ports） |
| `ENTRYPOINT` | 容器启动时执行的命令（容器启动后执行） |
| `CMD` | 容器启动命令的默认参数（可以被命令行参数覆盖） |

### 4.4 .dockerignore

在项目根目录创建 `.dockerignore`，排除不需要复制到镜像的文件：

```
target/
*.class
*.jar
!target/*.jar
.git
.gitignore
.idea
*.iml
.vscode
node_modules
frontend/node_modules
frontend/dist
Dockerfile
docker-compose.yml
README.md
学习笔记/
```

作用：加快构建速度（不复制无关文件），减小镜像体积。

---

## 五、完整的 docker-compose（包含应用）

把后端应用也加到 docker-compose.yml 里：

```yaml
version: '3.8'

services:
  mysql:
    # ... 同上（略）

  redis:
    # ... 同上（略）

  rabbitmq:
    # ... 同上（略）

  # ============================================================
  # 后端应用
  # ============================================================
  backend:
    build: .                              # 用当前目录的 Dockerfile 构建镜像
    container_name: library-backend
    restart: unless-stopped
    depends_on:                            # 依赖：等 MySQL/Redis/RabbitMQ 健康后再启动
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/library_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: library
      SPRING_DATASOURCE_PASSWORD: ll521521
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: guest
      SPRING_RABBITMQ_PASSWORD: guest
    ports:
      - "8080:8080"
    networks:
      - library-network

volumes:
  mysql_data:
  redis_data:
  rabbitmq_data:

networks:
  library-network:
    driver: bridge
```

**关键点**：

1. **build: .**：用当前目录的 Dockerfile 构建镜像。也可以用 `image: library-backend:latest` 直接用已构建的镜像。

2. **depends_on + condition: service_healthy**：等依赖服务健康检查通过后再启动应用。这样应用启动时 MySQL/Redis/RabbitMQ 已经准备好了，不会连接失败。

3. **环境变量覆盖配置**：Spring Boot 支持用环境变量覆盖 `application.yml` 里的配置。格式是 `SPRING_配置名_字段名`（大写，下划线分隔）：
   - `spring.datasource.url` → `SPRING_DATASOURCE_URL`
   - `spring.data.redis.host` → `SPRING_DATA_REDIS_HOST`
   - `spring.rabbitmq.host` → `SPRING_RABBITMQ_HOST`

   这样不需要修改 application.yml，通过环境变量就能切换配置（容器里数据库主机名是服务名 `mysql`，不是 `localhost`）。

4. **服务名作为主机名**：在同一个 docker-compose 网络里，应用可以用服务名访问其他容器：
   - MySQL：`mysql:3306`
   - Redis：`redis:6379`
   - RabbitMQ：`rabbitmq:5672`

   Docker 的内置 DNS 会把服务名解析成容器 IP。

---

## 六、前端容器化（Nginx 部署）

### 6.1 前端 Dockerfile

在 `frontend/` 目录创建 `Dockerfile`：

```dockerfile
# 第一阶段：构建
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build  # 构建产物在 dist/ 目录

# 第二阶段：Nginx 运行
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 6.2 Nginx 配置

在 `frontend/` 目录创建 `nginx.conf`：

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # 前端路由（React Router BrowserRouter 需要，刷新页面不 404）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理：把 /api 请求转发到后端
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

这样前端和后端在同一个域名下（前端 80 端口，API 通过 Nginx 代理到后端 8080），不存在跨域问题。

---

## 七、常用 Docker 命令

### 镜像命令

```bash
docker images                          # 列出所有镜像
docker pull mysql:8.0                  # 拉取镜像
docker build -t myapp:latest .         # 构建镜像（-t 指定名称和标签）
docker rmi mysql:8.0                   # 删除镜像
docker tag myapp:latest myapp:v1.0     # 给镜像打标签
```

### 容器命令

```bash
docker ps                              # 列出运行中的容器
docker ps -a                           # 列出所有容器（包括停止的）
docker run -d -p 3306:3306 --name mysql mysql:8.0   # 运行容器
docker stop library-mysql              # 停止容器
docker start library-mysql             # 启动已停止的容器
docker restart library-mysql           # 重启容器
docker rm library-mysql                # 删除容器（必须先停止）
docker logs -f library-mysql           # 查看容器日志（-f 实时跟踪）
docker exec -it library-mysql bash     # 进入容器（交互式）
docker exec library-mysql mysql -uroot -p -e "show databases;"  # 在容器里执行命令
```

### docker-compose 命令

```bash
docker-compose up -d                   # 启动所有服务（-d 后台运行）
docker-compose down                    # 停止并删除所有容器（数据卷保留）
docker-compose down -v                 # 停止并删除容器和数据卷（数据丢失！）
docker-compose ps                      # 查看服务状态
docker-compose logs -f backend         # 查看某个服务的日志
docker-compose restart backend         # 重启某个服务
docker-compose build backend           # 重新构建某个服务的镜像
docker-compose up -d --build           # 重新构建并启动
```

### 项目里的使用流程

```bash
# 1. 启动基础设施（MySQL、Redis、RabbitMQ）
cd library-management
docker-compose up -d

# 2. 查看服务状态
docker-compose ps

# 3. 查看 MySQL 日志
docker-compose logs -f mysql

# 4. 进入 MySQL 容器
docker exec -it library-mysql mysql -uroot -pll521521

# 5. 停止服务
docker-compose down

# 6. 如果后端也容器化了，一键启动所有
docker-compose up -d --build
```

---

## 八、本课必须记住的 7 件事

1. **Docker 核心概念**：镜像（Image，只读模板）、容器（Container，镜像的运行实例）、Dockerfile（构建脚本）、docker-compose（多容器编排）
2. **docker-compose 核心配置**：services（服务列表）、image（镜像）、ports（端口映射 宿主机:容器）、volumes（数据卷持久化）、environment（环境变量）、networks（网络）、healthcheck（健康检查）、depends_on（依赖顺序）
3. **容器间通信**：同一个 docker-compose 网络里，用服务名作为主机名访问（如 `mysql:3306`、`redis:6379`），Docker DNS 自动解析
4. **数据持久化**：用 volumes 挂载数据目录，容器删除后数据不丢失。MySQL 数据、Redis 数据都需要持久化
5. **多阶段构建**：Dockerfile 用多阶段构建（编译阶段 + 运行阶段），最终镜像只包含运行环境和应用，体积小
6. **环境变量覆盖配置**：Spring Boot 用 `SPRING_配置名_字段名` 环境变量覆盖 application.yml，容器里数据库主机名用服务名而不是 localhost
7. **健康检查 + depends_on**：用 healthcheck 检查服务是否就绪，depends_on + condition: service_healthy 保证应用在依赖服务就绪后再启动，避免连接失败

---

## 九、本节练习

### 练习1：启动基础设施服务

在项目根目录执行：
```bash
docker-compose up -d
```
然后：
1. 用 `docker-compose ps` 查看三个服务是否都在运行
2. 用 `docker logs -f library-mysql` 查看 MySQL 启动日志
3. 用 Navicat 或命令行连接 MySQL（localhost:3306，root/ll521521），确认 library_db 数据库和表已创建
4. 浏览器访问 http://localhost:15672（guest/guest），确认 RabbitMQ 管理界面可用

### 练习2：把后端应用容器化

1. 在项目根目录创建 Dockerfile（参考本课代码）
2. 创建 .dockerignore
3. 修改 docker-compose.yml，添加 backend 服务（参考本课代码）
4. 执行 `docker-compose up -d --build` 构建并启动所有服务
5. 用 `docker-compose logs -f backend` 查看后端启动日志
6. 浏览器访问 http://localhost:8080/api/doc.html，确认接口文档可用

### 练习3：把前端应用容器化

1. 在 frontend/ 目录创建 Dockerfile 和 nginx.conf（参考本课代码）
2. 在 docker-compose.yml 里添加 frontend 服务
3. 构建并启动，浏览器访问 http://localhost，确认前端页面可用
4. 登录并测试图书管理功能，确认前后端联调正常

---

## 十、自测题

### Q1：Docker 的镜像和容器有什么区别和关系？

<details>
<summary>点击查看答案</summary>

**区别**：
- **镜像（Image）**：是一个只读模板，包含应用运行所需的一切（代码、运行环境、库、配置）。镜像是静态的，不能运行，只能用来创建容器。
- **容器（Container）**：是镜像的运行实例，是一个独立运行的环境。容器是动态的，可以启动、停止、删除。

**关系**：
- 一个镜像可以创建多个容器（类似类和对象的关系：类是模板，对象是实例）
- 容器 = 镜像 + 可写层（容器运行时产生的数据在可写层，删除容器后可写层数据丢失，除非挂载了数据卷）
- 镜像可以版本化（tag），如 `mysql:8.0`、`mysql:5.7`

**类比**：
- 镜像 = 菜谱 / 类（Class） / 安装程序
- 容器 = 按菜谱做的菜 / 对象（Instance） / 安装好的软件

Dockerfile --build--> 镜像 --run--> 容器

</details>

### Q2：docker-compose 里的 ports、volumes、environment、networks 分别是什么作用？

<details>
<summary>点击查看答案</summary>

1. **ports（端口映射）**：把容器的端口映射到宿主机的端口，格式 `宿主机端口:容器端口`。这样宿主机上的应用可以通过 localhost:宿主机端口 访问容器里的服务。
   - 例：`"3306:3306"` 把容器的 3306 映射到宿主机的 3306
   - 注意：只是声明端口，EXPOSE 只是文档，实际映射靠 ports

2. **volumes（数据卷）**：把宿主机的目录或数据卷挂载到容器里，实现数据持久化和文件共享。容器删除后，数据卷里的数据不丢失。
   - 命名卷：`mysql_data:/var/lib/mysql`（Docker 管理的卷）
   - 绑定挂载：`./schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro`（宿主机文件挂载到容器，:ro 只读）
   - 作用：持久化数据库数据、挂载配置文件、共享代码

3. **environment（环境变量）**：设置容器内的环境变量，应用可以读取这些变量配置自己。
   - MySQL 镜像用环境变量初始化（MYSQL_ROOT_PASSWORD、MYSQL_DATABASE 等）
   - Spring Boot 用环境变量覆盖 application.yml（SPRING_DATASOURCE_URL 等）
   - 作用：配置应用、传递参数、区分环境

4. **networks（网络）**：定义 Docker 网络，同一个网络里的容器可以用服务名互相访问（Docker DNS 解析）。
   - 例：应用容器里用 `mysql:3306` 访问 MySQL 容器，不需要知道 IP
   - 不同网络的容器默认隔离，不能互相访问
   - 作用：容器间通信、网络隔离

</details>

### Q3：为什么 Dockerfile 要用多阶段构建？

<details>
<summary>点击查看答案</summary>

**原因**：减小最终镜像体积，只包含运行时需要的内容。

如果不用多阶段构建，用一个 `FROM maven:3.9` 镜像：
- 镜像里包含 Maven、JDK（不是 JRE）、所有 Maven 依赖、源码、编译产物
- 体积很大（可能 1GB+）
- 包含很多运行时不需要的东西（Maven、源码、编译工具）

用多阶段构建：
- **第一阶段（builder）**：用 Maven 镜像，下载依赖、编译、打包，生成 jar 文件
- **第二阶段（运行）**：用 JRE 镜像（只有运行环境，没有编译工具），只从第一阶段复制 jar 文件
- 最终镜像只有 JRE + jar，体积小很多（一百多 MB vs 1GB+）

**好处**：
1. 镜像体积小，拉取快、存储省
2. 更安全（不包含编译工具和源码，减少攻击面）
3. 构建时利用缓存（先复制 pom.xml 下载依赖，依赖不变时不需要重新下载）

**关键指令**：`COPY --from=builder /app/target/app.jar app.jar`，从 builder 阶段复制文件到当前阶段。

这是 Docker 最佳实践，生产环境的镜像都应该用多阶段构建。

</details>

### Q4：容器里的应用怎么连接 MySQL？为什么用服务名而不是 localhost？

<details>
<summary>点击查看答案</summary>

**容器里的应用连接 MySQL 用服务名**：`jdbc:mysql://mysql:3306/library_db`，主机名是 `mysql`（docker-compose 里的服务名），不是 `localhost`。

**为什么不能用 localhost**：
- 容器里的 `localhost` 指的是容器自己，不是宿主机
- MySQL 运行在另一个容器里，不在应用容器自己里
- 所以用 `localhost:3306` 会连接失败（应用容器里没有 MySQL）

**为什么能用服务名**：
- docker-compose 会为所有服务创建一个默认网络（或自定义网络）
- 同一个网络里的容器，Docker 提供内置 DNS 服务
- DNS 会把服务名（如 `mysql`、`redis`）解析成对应容器的 IP
- 所以应用容器里可以用 `mysql:3306` 访问 MySQL 容器，就像用域名访问网站一样

**怎么配置**：
通过环境变量覆盖 Spring Boot 的数据库配置：
```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/library_db?...
```
Spring Boot 会自动读取 `SPRING_DATASOURCE_URL` 环境变量，覆盖 application.yml 里的 `spring.datasource.url`。

**注意**：如果应用在宿主机上运行（不是容器），连接容器里的 MySQL 用 `localhost:3306`（因为端口映射到了宿主机）。只有应用也在容器里时才用服务名。

</details>

### Q5：healthcheck 和 depends_on 有什么用？为什么应用要等依赖服务健康后再启动？

<details>
<summary>点击查看答案</summary>

**healthcheck（健康检查）**：Docker 定期在容器里执行命令，判断容器是否健康（服务是否就绪）。
- 配置：test（检查命令）、interval（间隔）、timeout（超时）、retries（重试次数）
- 例：MySQL 用 `mysqladmin ping` 检查，Redis 用 `redis-cli ping` 检查
- 容器状态：starting（启动中）→ healthy（健康）/ unhealthy（不健康）

**depends_on（依赖）**：定义服务启动顺序，当前服务依赖其他服务。
- 简单用法：`depends_on: [mysql, redis]`，只保证先启动依赖服务，但不保证服务就绪
- 高级用法：`depends_on: { mysql: { condition: service_healthy } }`，等依赖服务健康检查通过后再启动当前服务

**为什么应用要等依赖服务健康后再启动**：
- 如果应用启动时 MySQL 还没准备好（还在初始化），应用连接数据库会失败
- Spring Boot 启动时会初始化数据库连接池，如果连不上可能启动失败
- 用 `depends_on + condition: service_healthy` 保证 MySQL/Redis/RabbitMQ 都就绪后，应用才启动，避免连接失败

**项目里的配置**：
```yaml
backend:
  depends_on:
    mysql:
      condition: service_healthy
    redis:
      condition: service_healthy
    rabbitmq:
      condition: service_healthy
```
应用等三个基础设施服务都健康后才启动。

**注意**：健康检查只能保证服务进程就绪，不能保证数据就绪（如数据库表已创建）。如果需要等表创建好，可以在应用里配置重试或初始化脚本。

</details>

---

## 十一、面试题

### 面试题1：Docker 的原理？和虚拟机有什么区别？

> **答题要点**：
> 1. **Docker 原理**：基于 Linux 内核的 Namespace（资源隔离）和 Cgroups（资源限制）实现容器化。
>    - Namespace：隔离 PID（进程）、Network（网络）、Mount（文件系统）、UTS（主机名）、IPC（进程间通信）、User（用户）等，让容器看起来像独立的系统
>    - Cgroups：限制容器的 CPU、内存、磁盘 IO 等资源使用，防止一个容器占满宿主机资源
>    - UnionFS（联合文件系统）：镜像分层存储，共享底层层，节省空间
> 2. **和虚拟机的区别**：
>    - 架构：容器共享宿主机内核，容器级隔离；虚拟机有完整的客户操作系统，硬件级虚拟化
>    - 启动速度：容器秒级（1-5秒），虚拟机分钟级
>    - 资源占用：容器 MB 级，虚拟机 GB 级
>    - 性能：容器接近原生（几乎无损耗），虚拟机有虚拟化损耗（5-20%）
>    - 隔离性：容器是进程级隔离（共享内核，隔离性较弱），虚拟机是完全隔离（独立内核，隔离性强）
>    - 可移植性：容器镜像跨平台（只要有 Docker），虚拟机镜像较重
> 3. **适用场景**：
>    - Docker：应用部署、微服务、CI/CD、开发环境一致性
>    - 虚拟机：需要不同操作系统、强隔离要求、运行内核级软件
> 4. **核心概念**：镜像（只读模板）、容器（运行实例）、Dockerfile（构建脚本）、仓库（镜像存储）
> 5. **注意**：Docker 最初只能跑 Linux 容器，Windows 上需要 WSL2 或 Hyper-V 支持。Windows 容器也有但生态不如 Linux。

### 面试题2：Dockerfile 常用指令？多阶段构建的好处？

> **答题要点**：
> 1. **常用指令**：
>    - `FROM`：基础镜像，必须第一条指令
>    - `WORKDIR`：工作目录，后续命令在此目录执行
>    - `COPY` / `ADD`：复制文件到镜像（COPY 更简单，ADD 支持自动解压和 URL）
>    - `RUN`：构建时执行命令（安装依赖、编译），每一层 RUN 会创建新镜像层
>    - `ENV`：设置环境变量
>    - `EXPOSE`：声明容器监听端口（只是文档，实际映射靠 -p）
>    - `ENTRYPOINT`：容器启动命令（不可被命令行参数覆盖，只能追加参数）
>    - `CMD`：容器启动默认命令/参数（可被命令行参数覆盖）
>    - `VOLUME`：声明数据卷
>    - `ARG`：构建参数（构建时传入，不进入最终镜像）
> 2. **多阶段构建**：
>    - 用多个 FROM 指令，每个 FROM 是一个阶段
>    - 第一阶段用完整环境（Maven/Gradle + JDK）编译打包
>    - 第二阶段用精简运行环境（JRE/Alpine），只复制第一阶段的产物
>    - `COPY --from=builder /path/to/app.jar app.jar` 从指定阶段复制
> 3. **好处**：
>    - 减小镜像体积（最终镜像只有运行环境+应用，不含编译工具和源码）
>    - 更安全（攻击面小，不含源码和构建工具）
>    - 构建缓存优化（分层构建，依赖不变时不重新下载）
>    - 分离构建和运行环境
> 4. **最佳实践**：
>    - 用 .dockerignore 排除无关文件
>    - 把变化少的层放前面（如先复制 pom.xml 下载依赖，再复制源码），利用缓存
>    - 合并 RUN 指令减少镜像层数
>    - 用 Alpine 基础镜像减小体积
>    - 不要用 latest 标签（版本不确定）

---

## 十二、下一课预告

**第20课：Knife4j 接口文档 + 阶段三总结**

我们会搞清楚：
- 什么是 Knife4j/Swagger？为什么需要接口文档？
- 项目里的 Knife4j 配置
- 常用注解：@Api、@ApiOperation、@ApiParam、@ApiModel、@ApiModelProperty
- 怎么写规范的接口文档注解
- 接口文档的使用（在线调试、导出、分组）
- 阶段三总结：你现在能做什么？
- 阶段四和阶段五的学习路线
