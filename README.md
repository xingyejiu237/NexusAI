# NexusAI

基于 [DocuMind](https://github.com/xingyejiu237/DocuMind) 项目的 AI 组件库

## 简介

NexusAI 是一个模块化的 AI 组件库，提供构建智能应用所需的核心能力。目前基于 Spring AI 框架实现，支持 Java 语言。

## 项目结构

```
nexusai/
├── core/                          # 核心能力层
│   ├── rag/java/spring-ai/        # RAG (检索增强生成)
│   │   ├── document/              # 文档处理组件
│   │   │   ├── DocumentComponent.java
│   │   │   └── DocumentComponentImpl.java
│   │   ├── RagComponent.java
│   │   └── RagComponentImpl.java
│   ├── mcp/java/spring-ai/        # MCP (模型上下文协议)
│   │   ├── McpComponent.java
│   │   └── McpComponentImpl.java
│   ├── memory/java/spring-ai/     # 会话/记忆管理
│   │   ├── SessionManager.java
│   │   ├── InMemorySessionManager.java
│   │   └── Conversation.java
│   ├── agent/java/spring-ai/      # Agent 调度
│   │   ├── IntentRouter.java
│   │   ├── LlmIntentRouter.java
│   │   └── agent/
│   │       └── Agent.java
│   ├── llm/java/spring-ai/        # (预留) LLM 统一接口
│   ├── tools/java/spring-ai/      # (预留) 通用工具集
│   └── utils/java/spring-ai/      # (预留) 工具函数
├── examples/                      # (预留) 使用示例
└── spec/                          # (预留) 接口规范
```

## 组件说明

### 1. RAG 组件
- **DocumentComponent**: 文档加载、分割、存储
- **RagComponent**: 向量检索 + LLM 生成完整 RAG 流程

### 2. MCP 组件
- 支持本地模式（连接外部 MCP Server）
- 支持 Docker 模式（直接使用 JDBC，零 Node.js 依赖）
- 内置 SQL 查询工具

### 3. Memory 组件
- **SessionManager**: 多轮对话上下文管理
- **InMemorySessionManager**: 基于内存的实现（开发测试用）

### 4. Agent 组件
- **IntentRouter**: 意图识别和 Agent 路由
- **LlmIntentRouter**: 基于 LLM 的意图识别实现
- **Agent**: Agent 接口，自定义 Agent 需实现

## 使用方式

在你的 Spring Boot 项目中引入组件：

```java
import nexusai.core.rag.java.springai.RagComponent;
import nexusai.core.memory.java.springai.SessionManager;
import nexusai.core.mcp.java.springai.McpComponent;
import nexusai.core.agent.java.springai.IntentRouter;

@Service
public class YourService {
    
    @Autowired
    private RagComponent ragComponent;
    
    @Autowired
    private SessionManager sessionManager;
    
    @Autowired
    private McpComponent mcpComponent;
    
    @Autowired
    private IntentRouter intentRouter;
    
    // 使用组件...
}
```

## 配置

```yaml
# RAG 配置
rag:
  top-k: 3
  similarity-threshold: 0.0

# MCP 配置
mcp:
  mode: local  # 或 docker
```

## 依赖

- Spring Boot 3.x
- Spring AI
- Redis（向量存储）
- MCP SDK（可选，本地模式需要）

## 更新日志

### 2025-04-29
- 初始版本
- 基于 DocuMind 项目重构组件结构
- 提供 RAG、MCP、Memory、Agent 四大核心组件

## License

MIT
