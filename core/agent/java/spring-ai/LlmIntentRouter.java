package nexusai.core.agent.java.springai;

import jakarta.annotation.PostConstruct;
import nexusai.core.agent.java.springai.agent.Agent;
import nexusai.core.mcp.java.springai.McpComponent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 LLM 的意图路由实现
 */
@Component
public class LlmIntentRouter implements IntentRouter {

    private final ChatClient chatClient;
    private final McpComponent mcpComponent;
    private final List<Agent> registeredAgents = new ArrayList<>();

    private String customIntentPrompt = null;

    public LlmIntentRouter(ChatClient chatClient, McpComponent mcpComponent) {
        this.chatClient = chatClient;
        this.mcpComponent = mcpComponent;
    }

    @PostConstruct
    public void initialize() {
        // 从 Spring 容器中自动获取所有 Agent
        // 注意：这里需要在外部初始化后调用 registerAgents
    }

    @Override
    public String route(String message) {
        String intentPrompt = buildIntentPrompt(message);

        String intent = chatClient.prompt()
                .user(intentPrompt)
                .call()
                .content()
                .trim()
                .toLowerCase();

        System.out.println("[LlmIntentRouter] 意图识别结果: " + intent);

        // 检查是否匹配某个 Agent
        for (Agent agent : registeredAgents) {
            if (intent.contains(agent.getName().toLowerCase())) {
                return agent.getName();
            }
        }

        // 默认返回 general
        return "general";
    }

    @Override
    public void registerAgent(Agent agent) {
        registeredAgents.add(agent);
        System.out.println("[LlmIntentRouter] 注册 Agent: " + agent.getName());
    }

    @Override
    public void registerAgents(List<Agent> agents) {
        for (Agent agent : agents) {
            registerAgent(agent);
        }
    }

    @Override
    public void setIntentPrompt(String prompt) {
        this.customIntentPrompt = prompt;
    }

    @Override
    public List<Agent> getRegisteredAgents() {
        return new ArrayList<>(registeredAgents);
    }

    private String buildIntentPrompt(String message) {
        if (customIntentPrompt != null) {
            return customIntentPrompt.formatted(getToolDescriptions(), message);
        }

        StringBuilder agentList = new StringBuilder();
        for (Agent agent : registeredAgents) {
            agentList.append("- ").append(agent.getName()).append("\n");
        }

        return """
                你是一个意图分类器。根据用户消息判断应该交给哪个 Agent 处理。

                可用 Agent：
                %s

                MCP 工具列表：
                %s

                判断原则：
                1. 如果用户问题涉及查询具体数据、记录、用户信息、订单、年龄、时间等结构化数据，归为 business
                2. 如果用户问题需要使用 MCP 工具查询数据库，归为 business
                3. 如果是纯知识性、概念性、政策性问题，归为 knowledge
                4. 闲聊、无关问题归为 general

                示例：
                - "wangwu 几岁了" → business
                - "查询订单状态" → business
                - "退货政策是什么" → knowledge
                - "你好" → general

                只需回复 Agent 名称，如果都不匹配则回复 general。

                用户消息：%s
                """.formatted(agentList.toString(), getToolDescriptions(), message);
    }

    private String getToolDescriptions() {
        var tools = mcpComponent.listTools();
        if (tools.isEmpty()) {
            return "暂无可用工具";
        }

        StringBuilder sb = new StringBuilder();
        for (var tool : tools) {
            sb.append("- ").append(tool.name()).append(": ").append(tool.description()).append("\n");
        }
        return sb.toString();
    }
}
