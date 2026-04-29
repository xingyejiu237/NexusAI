package nexusai.core.memory.java.springai;

import org.springframework.ai.chat.messages.Message;

import java.time.Duration;
import java.util.List;

/**
 * 会话管理组件接口
 * 提供多轮对话上下文管理功能
 */
public interface SessionManager {

    /**
     * 获取或创建会话
     *
     * @param sessionId 会话 ID
     * @return 会话对象
     */
    Conversation getSession(String sessionId);

    /**
     * 添加用户消息
     *
     * @param sessionId 会话 ID
     * @param content   消息内容
     */
    void addUserMessage(String sessionId, String content);

    /**
     * 添加 AI 回复消息
     *
     * @param sessionId 会话 ID
     * @param content   消息内容
     */
    void addAssistantMessage(String sessionId, String content);

    /**
     * 添加系统消息
     *
     * @param sessionId 会话 ID
     * @param content   消息内容
     */
    void addSystemMessage(String sessionId, String content);

    /**
     * 获取最近 N 条消息（用于传给 LLM）
     *
     * @param sessionId 会话 ID
     * @param limit     消息数量限制
     * @return 消息列表
     */
    List<Message> getRecentMessages(String sessionId, int limit);

    /**
     * 获取会话所有消息
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<Message> getAllMessages(String sessionId);

    /**
     * 清空会话
     *
     * @param sessionId 会话 ID
     */
    void clearSession(String sessionId);

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     */
    void deleteSession(String sessionId);

    /**
     * 设置会话过期时间
     *
     * @param sessionId 会话 ID
     * @param duration  过期时长
     */
    void setExpire(String sessionId, Duration duration);

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话 ID
     * @return 是否存在
     */
    boolean exists(String sessionId);
}
