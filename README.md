# NexusAI

## 简介

NexusAI 是一个模块化的 AI 组件库，提供构建智能应用所需的核心能力。目前基于 Spring AI 框架实现，支持 Java 语言。

## 项目结构

```
nexusai/
├── core/                          # 核心能力层
│   ├── rag/java/spring-ai/        # RAG (检索增强生成)
│   │   ├── document/              # 文档处理组件
│   │   │   ├── DocumentComponent.java
│   │   │   ├── DocumentComponentImpl.java
│   │   │   ├── DocumentStorageAdapter.java   # 存储适配接口
│   │   │   ├── QdrantStorageAdapter.java     # Qdrant 实现
│   │   │   └── RedisStorageAdapter.java      # Redis 实现
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
│   ├── tools/java/spring-ai/      # 工具组件
│   │   └── prometheus/            # Prometheus 监控工具
│   │       ├── PrometheusConfig.java
│   │       ├── PrometheusService.java
│   │       └── PrometheusTools.java
│   ├── llm/java/spring-ai/        # (预留) LLM 统一接口
│   └── utils/java/spring-ai/      # (预留) 工具函数
├── examples/                      # (预留) 使用示例
└── spec/                          # (预留) 接口规范
```

## 组件说明

### 1. RAG 组件
- **DocumentComponent**: 文档加载、分割、存储
- **DocumentComponentImpl**: 基于 StorageAdapter 的可插拔存储实现
- **DocumentStorageAdapter**: 存储适配接口，支持 Qdrant/Redis
- **RagComponent**: 向量检索 + LLM 生成完整 RAG 流程
- **RagComponentImpl**: 提示词配置化（支持 `rag.system-prompt` / `rag.empty-response`）

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

### 5. Tools 组件
- **PrometheusTools**: Prometheus 监控指标查询工具
  - 支持 CPU、内存、QPS、延迟、错误率等指标查询
  - 基于 Spring AI `@Tool` 注解，可被 LLM 自动调用
  - 可自定义 PromQL 模板

## 使用方式

在你的 Spring Boot 项目中引入组件：

```java
import nexusai.core.rag.java.springai.RagComponent;
import nexusai.core.memory.java.springai.SessionManager;
import nexusai.core.mcp.java.springai.McpComponent;
import nexusai.core.agent.java.springai.IntentRouter;
import nexusai.core.tools.java.springai.prometheus.PrometheusTools;

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
    
    @Autowired
    private PrometheusTools prometheusTools;
    
    // 使用组件...
}
```

## 配置

```yaml
# RAG 配置
rag:
  top-k: 3
  similarity-threshold: 0.0
  system-prompt: ""           # 自定义系统提示（可选）
  empty-response: "知识库中未找到相关信息"  # 空结果回复
  list-top-k: 100            # 列出文档数量
  qdrant:
    collection-name: rag_collection
    vector-size: 1024
  redis:
    doc-prefix: "doc:"
    index-name: rag-index

# MCP 配置
mcp:
  mode: local  # 或 docker

# Prometheus 配置
prometheus:
  url: http://localhost:9090
  default-duration: 1h
  default-step-seconds: 0
```

## 依赖

- Spring Boot 3.x
- Spring AI
- 向量存储（任选其一）：
  - Redis + Spring AI Redis Vector Store
  - Qdrant + Qdrant Client
- MCP SDK（可选，本地模式需要）
- WebFlux（Prometheus 组件需要）

## 更新日志

详见 [更新日志.md](更新日志.md)

## License

本项目暂未指定开源许可证，保留所有权利。
