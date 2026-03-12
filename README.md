<div align="center">

**PRD 评审平台** - 基于大语言模型的 PRD 文档智能评审系统

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-blue?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue?logo=typescript)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-336791?logo=postgresql)](https://www.postgresql.org/)


</div>


---

## 项目介绍

PRD Review Platform 是一个集成了 PRD 文档智能评审和知识库管理的 AI 辅助平台。系统利用大语言模型（LLM）和向量数据库技术，为产品经理和研发团队提供智能化的 PRD 评审服务。

## 系统架构

**异步处理流程**：

PRD 评审和知识库向量化采用 Redis Stream 异步处理：

```
评审请求 → 保存记录 → 发送消息到 Stream → 立即返回
                              ↓
                      Consumer 消费消息
                              ↓
                    执行 AI 评审/向量化任务
                              ↓
                      更新数据库状态
                              ↓
                   前端轮询获取最新状态
```

状态流转： `PENDING` → `PROCESSING` → `COMPLETED` / `FAILED`。

## 技术栈

### 后端技术

| 技术                  | 版本  | 说明                      |
| --------------------- | ----- | ------------------------- |
| Spring Boot           | 4.0   | 应用框架                  |
| Java                  | 21    | 开发语言                  |
| Spring AI             | 2.0   | AI 集成框架               |
| PostgreSQL + pgvector | 14+   | 关系数据库 + 向量存储     |
| Redis                 | 6+    | 缓存 + 消息队列（Stream） |
| Apache Tika           | 2.9.2 | 文档解析                  |
| iText 8               | 8.0.5 | PDF 导出                  |
| MapStruct             | 1.6.3 | 对象映射                  |
| Gradle                | 8.14  | 构建工具                  |

### 前端技术

| 技术          | 版本  | 说明     |
| ------------- | ----- | -------- |
| React         | 18.3  | UI 框架  |
| TypeScript    | 5.6   | 开发语言 |
| Vite          | 5.4   | 构建工具 |
| Tailwind CSS  | 4.1   | 样式框架 |
| React Router  | 7.11  | 路由管理 |
| Framer Motion | 12.23 | 动画库   |
| Recharts      | 3.6   | 图表库   |
| Lucide React  | 0.468 | 图标库   |

## 功能特性

### PRD 评审模块

- **多维度评审**：从需求清晰度、范围边界、用户场景、技术风险、验收标准、工作量评估等多个维度进行专业评审。
- **多粒度评审**：支持基础评审、详细评审、全面评审三种粒度，满足不同场景需求。
- **知识库辅助**：支持关联知识库，利用 RAG 技术提供更专业的评审建议。
- **异步处理流**：基于 Redis Stream 实现异步评审，支持实时查看处理进度。
- **稳定性保障**：内置评审失败自动重试机制。
- **报告导出**：支持将评审结果一键导出为 PDF 报告。

### 知识库管理模块

- **文档智能处理**：支持 PDF、DOCX、Markdown 等多种格式文档的自动上传、分块与异步向量化。
- **RAG 检索增强**：集成向量数据库，通过检索增强生成（RAG）提升 AI 问答的准确性与专业度。
- **流式响应交互**：基于 SSE（Server-Sent Events）技术实现打字机式流式响应。
- **智能问答对话**：支持基于知识库内容的智能问答，并提供直观的知识库统计信息。
- **分类管理**：支持知识库分类管理，便于组织和检索。

### TODO

- [x] PRD 评审详情页
- [x] 评审记录管理
- [x] PDF 报告导出
- [x] Docker 快速部署
- [x] 添加 API 限流保护
- [x] 添加用户账号功能
- [ ] PRD 版本对比

## 项目结构

```
prd-review/
├── app/                              # 后端应用
│   ├── src/main/java/prd/guide/
│   │   ├── App.java                  # 主启动类
│   │   ├── common/                   # 通用模块
│   │   │   ├── config/               # 配置类
│   │   │   ├── exception/            # 异常处理
│   │   │   └── result/               # 统一响应
│   │   ├── infrastructure/           # 基础设施
│   │   │   ├── export/               # PDF 导出
│   │   │   ├── file/                 # 文件处理
│   │   │   ├── mapper/               # MapStruct 映射器
│   │   │   ├── redis/                # Redis 服务
│   │   │   └── storage/              # 对象存储
│   │   └── modules/                  # 业务模块
│   │       ├── prdreview/            # PRD 评审模块
│   │       └── knowledgebase/        # 知识库模块
│   └── src/main/resources/
│       ├── application.yml           # 应用配置
│       └── prompts/                  # AI 提示词模板
│
├── frontend/                         # 前端应用
│   ├── src/
│   │   ├── api/                      # API 接口
│   │   ├── components/               # 公共组件
│   │   ├── pages/                    # 页面组件
│   │   ├── types/                    # 类型定义
│   │   └── utils/                    # 工具函数
│   ├── package.json
│   └── vite.config.ts
│
└── README.md
```

## 环境要求

| 依赖          | 版本 | 必需 |
| ------------- | ---- | ---- |
| JDK           | 21+  | 是   |
| Node.js       | 18+  | 是   |
| PostgreSQL    | 14+  | 是   |
| pgvector 扩展 | -    | 是   |
| Redis         | 6+   | 是   |
| S3 兼容存储   | -    | 是   |


## 使用场景

| 用户角色        | 使用场景                               |
| --------------- | -------------------------------------- |
| **产品经理**    | 上传 PRD 获取专业评审建议，优化需求文档 |
| **研发团队**    | 评审 PRD 技术可行性，评估工作量        |
| **项目经理**    | 审核需求完整性，把控项目风险           |

## 许可证

AGPL-3.0 License（只要通过网络提供服务，就必须向用户公开修改后的源码）
