package nexusai.core.agent.java.springai.agent;

/**
 * Agent 接口
 * 所有自定义 Agent 需实现此接口
 */
public interface Agent {

    /**
     * 获取 Agent 名称
     *
     * @return Agent 名称
     */
    String getName();

    /**
     * 获取 Agent 描述
     *
     * @return Agent 描述
     */
    String getDescription();

    /**
     * 处理消息
     *
     * @param message 用户消息
     * @return Agent 回复
     */
    String process(String message);

    /**
     * 判断是否支持处理该消息
     *
     * @param message 用户消息
     * @return 是否支持
     */
    boolean supports(String message);
}
