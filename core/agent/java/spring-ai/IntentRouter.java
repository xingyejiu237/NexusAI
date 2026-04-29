package nexusai.core.agent.java.springai;

import nexusai.core.agent.java.springai.agent.Agent;

import java.util.List;

/**
 * 意图路由组件接口
 * 提供意图识别和 Agent 路由功能
 */
public interface IntentRouter {

    /**
     * 路由消息到对应 Agent
     *
     * @param message 用户消息
     * @return 目标 Agent 名称，如果没有匹配返回 "general"
     */
    String route(String message);

    /**
     * 注册 Agent
     *
     * @param agent Agent 实例
     */
    void registerAgent(Agent agent);

    /**
     * 批量注册 Agent
     *
     * @param agents Agent 列表
     */
    void registerAgents(List<Agent> agents);

    /**
     * 设置自定义意图识别提示
     *
     * @param prompt 自定义提示
     */
    void setIntentPrompt(String prompt);

    /**
     * 获取所有已注册 Agent
     *
     * @return Agent 列表
     */
    List<Agent> getRegisteredAgents();
}
