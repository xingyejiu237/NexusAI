package nexusai.core.mcp.java.springai;

import java.util.List;
import java.util.Map;

/**
 * MCP (Model Context Protocol) 组件接口
 * 提供标准化的 MCP 工具调用功能
 */
public interface McpComponent {

    /**
     * 调用指定工具
     *
     * @param toolName 工具名称
     * @param params   工具参数
     * @return 工具执行结果
     */
    String callTool(String toolName, Map<String, Object> params);

    /**
     * 列出所有可用工具
     *
     * @return 工具信息列表
     */
    List<ToolInfo> listTools();

    /**
     * 执行 SQL 查询（便捷方法）
     *
     * @param sql SQL 语句
     * @return 查询结果
     */
    String executeSql(String sql);

    /**
     * 工具信息
     */
    record ToolInfo(String name, String description) {}
}
