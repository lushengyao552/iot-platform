# Stage 17：AI 模块

## 阶段目标（基础系统全掌握后才进入）

在已经能独立写完整后端的基础上，加一个"图书馆 AI 助手"：用户能和 AI 聊天，问"这本书讲了什么"、"推荐几本 Java 入门书"。理解 Spring AI、Prompt、上下文、RAG、Vector DB、Tool Calling 的基本概念。

**重要：本阶段必须在 stage16 毕业之后再开。不要为了追 AI 跳过 Java 基础。**

## 前置要求

- stage16 毕业
- 有可用的 OpenAI 兼容 API（DeepSeek / Qwen / 豆包 / GLM 任选）
- 知道什么是大模型 API

## 本阶段知识点（按顺序学，不要跳）

- 大模型 API 调用（HTTP + API Key）
- Spring AI 抽象层
- Prompt 设计
- 对话上下文（多轮对话）
- 会话管理（一个用户多个会话）
- Function Calling / Tool Calling
- RAG：Embedding + 向量数据库 + 检索增强
- 向量数据库选型（PG Vector / Redis Vector / Milvus）

---

## 题目

### 【第 17-1 题：第一个 Chat 接口】

**业务背景**
用户在图书馆网站点"问 AI 助手"。

**我的任务**
1. 引入 Spring AI（或直接用 HTTP 客户端调 OpenAI 兼容 API）
2. 配置 API Key / Base URL / Model
3. 写 POST /api/ai/chat，接收 question，返回 answer
4. 鉴权（必须登录）
5. Knife4j 能测

**限制条件**
- 先不做多轮，先做单轮问答
- API Key 不要提交到 Git
- 超时要设（大模型响应慢）

**验收标准**
- 问"推荐几本 Java 入门书"能返回回答
- 你能说清一次请求花了多久
- 你能说出 Token 是什么、怎么计费

**思考题**
1. 大模型 API 是怎么计费的？
2. 为什么响应这么慢？
3. 流式输出是什么？SSE 是什么？
4. API Key 泄露了会怎样？

---

### 【第 17-2 题：多轮对话与会话】

**业务背景**
单轮对话太傻，它记不住你上一句说什么。

**我的任务**
1. 设计表 conversation（会话）：id / user_id / title / create_time
2. 设计表 chat_message：id / conversation_id / role(user/assistant) / content / create_time
3. POST /api/ai/chat 改成：
   - 传 conversationId（可空，空就新建会话）
   - 后端从数据库取该会话历史消息，拼到 Prompt 里
   - 调大模型
   - 把 user message 和 assistant message 都存库
4. GET /api/ai/conversations/{id}/messages 查历史

**限制条件**
- 历史消息不能无限塞（上下文长度有限），要截断最近 N 条或按 Token 数截断
- 不同用户的会话不串
- 普通用户只能看自己的会话

**验收标准**
- 多轮对话有记忆
- 刷新页面能恢复历史
- 你能说清上下文窗口是什么、为什么会爆
- 你能说出 token 计数怎么算

**思考题**
1. 为什么上下文越长越贵？
2. 多轮对话历史怎么存？全塞 prompt 吗？
3. 会话标题要不要 AI 自动生成？
4. 用户清空历史怎么做？

---

### 【第 17-3 题：让 AI 查图书馆数据（Tool Calling）】

**业务背景**
AI 不知道你图书馆里有什么书。让它能调你的接口。

**我的任务**
1. 写一个工具函数 searchBooks(String keyword)：走你的 bookService 查书
2. 用 Spring AI 的 @Tool 或自己构造 function calling 请求
3. 用户问"你们图书馆有《深入理解 Java 虚拟机》吗？" → AI 自动调 searchBooks → 把结果告诉用户

**限制条件**
- 工具函数返回结果要结构化（JSON）
- 不能让 AI 直接写 SQL
- 工具调用要鉴权（用户必须登录）
- 工具调用次数要有限制（防刷）

**验收标准**
- AI 能根据你图书馆的真实库存回答
- 你能说清 Function Calling 的流程（模型决定调哪个工具 → 你执行 → 把结果喂回模型）
- 你能说出为什么不能让 AI 直接查数据库

**思考题**
1. Function Calling 是大模型自己写代码吗？
2. 工具的描述 Prompt 怎么写才能让模型调对？
3. 一个用户一次对话能调多少次工具？
4. 工具调用失败怎么办？

---

### 【第 17-4 题：RAG 入门（选做，高级）】

**业务背景**
把图书馆的图书简介、书评、FAQ 做成知识库，AI 回答时参考。

**我的任务**
1. 选一个向量库（Redis Stack / PG Vector / Milvus 任选）
2. 把图书简介切片、Embedding、存进向量库
3. 用户提问时：
   - 把问题 Embedding
   - 向量检索 top K 相关图书
   - 把这些资料塞进 Prompt
   - 让大模型基于资料回答
4. 回答里能注明参考了哪本书

**限制条件**
- 理解 RAG 解决什么问题（私有知识、减少幻觉）
- 不需要生产级，跑通即可
- 你要能讲清楚整个流程

**验收标准**
- 问"我们图书馆有什么关于 Spring 的书"，AI 能从真实库存里回答
- 你能说清 Embedding 是什么、向量检索为什么能"语义相似"
- 你能说清 RAG vs 微调的区别

**思考题**
1. RAG 为什么能减少幻觉？
2. 向量检索为什么比 LIKE 更"懂语义"？
3. 文档切片太大太小有什么问题？
4. RAG 和 Function Calling 的区别？
5. 向量数据库为什么快？（近似最近邻 ANN）

---

## 本阶段毕业考试

不看任何资料：
1. 给你一个新需求"图书馆 AI 帮你写借阅延期申请理由"，设计接口 + 提示词
2. 解释一次多轮对话的完整流程（包括历史消息怎么拼）
3. 解释 RAG 的四个步骤

## 本阶段反向面试题

1. LLM、Embedding、Vector DB 各是什么？
2. Prompt Engineering 为什么重要？
3. 幻觉是什么？怎么缓解？
4. RAG 怎么评估效果？
5. Function Calling 怎么调试？
6. 上下文窗口不够怎么办？
7. AI 应用成本怎么控？

## 常见坑

- API Key 硬编码或提交 Git
- 历史消息无限塞，上下文爆
- 让 AI 直接写 SQL
- 没设超时，接口 hang 死
- 工具函数没鉴权，任何人都能查
- RAG 切片不合理，检索效果差
