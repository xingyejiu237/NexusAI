package nexusai.core.mcp.java.springai;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 组件实现类
 * 支持两种模式：
 * 1. 本地模式：通过 McpSyncClient 连接外部 MCP Server
 * 2. Docker 模式：直接使用 JDBC 连接数据库（零 Node.js 依赖）
 */
@Component
public class McpComponentImpl implements McpComponent {

    private final List<McpSyncClient> mcpClients;
    private final DataSource dataSource;

    @Value("${mcp.mode:local}")
    private String mcpMode;

    public McpComponentImpl(@Autowired(required = false) List<McpSyncClient> mcpClients,
                           @Autowired(required = false) DataSource dataSource) {
        this.mcpClients = mcpClients != null ? mcpClients : new ArrayList<>();
        this.dataSource = dataSource;
    }

    @Override
    public String callTool(String toolName, Map<String, Object> params) {
        // Docker 模式：直接使用 JDBC
        if ("docker".equals(mcpMode) && "mysql_query".equals(toolName)) {
            return executeSqlViaJdbc((String) params.get("sql"));
        }

        // 本地模式：通过 MCP Client
        return callToolViaMcp(toolName, params);
    }

    @Override
    public List<ToolInfo> listTools() {
        List<ToolInfo> tools = new ArrayList<>();

        // Docker 模式：返回固定的 MySQL 工具
        if ("docker".equals(mcpMode)) {
            tools.add(new ToolInfo("mysql_query", "Execute a MySQL query and return results as JSON"));
            return tools;
        }

        // 本地模式：从 MCP Server 获取
        for (McpSyncClient client : mcpClients) {
            try {
                McpSchema.ListToolsResult result = client.listTools();
                for (McpSchema.Tool tool : result.tools()) {
                    tools.add(new ToolInfo(tool.name(), tool.description()));
                }
            } catch (Exception e) {
                System.err.println("[McpComponent] 获取工具列表失败: " + e.getMessage());
            }
        }

        return tools;
    }

    @Override
    public String executeSql(String sql) {
        return callTool("mysql_query", Map.of("sql", sql));
    }

    /**
     * 通过 MCP Client 调用工具（本地模式）
     */
    private String callToolViaMcp(String toolName, Map<String, Object> params) {
        for (McpSyncClient client : mcpClients) {
            try {
                McpSchema.CallToolResult result = client.callTool(
                        new McpSchema.CallToolRequest(toolName, params)
                );
                return result.content().stream()
                        .map(content -> {
                            if (content instanceof McpSchema.TextContent textContent) {
                                return textContent.text();
                            }
                            return content.toString();
                        })
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("");
            } catch (Exception e) {
                System.err.println("[McpComponent] 调用工具失败: " + toolName + ", 错误: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * 通过 JDBC 直接执行 SQL（Docker 模式）
     */
    private String executeSqlViaJdbc(String sql) {
        if (dataSource == null) {
            return "{\"error\": \"DataSource not available in Docker mode\"}";
        }

        // 安全检查：只允许 SELECT/SHOW/DESC
        String trimmedSql = sql.trim().toLowerCase();
        if (!trimmedSql.startsWith("select") && !trimmedSql.startsWith("show") && !trimmedSql.startsWith("desc")) {
            return "{\"error\": \"Only SELECT, SHOW, and DESCRIBE queries are allowed\"}";
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Map<String, Object>> results = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }

            // 转换为 JSON 字符串（简单实现）
            return convertToJson(results);

        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
        }
    }

    /**
     * 简单 JSON 转换（生产环境建议使用 Jackson）
     */
    private String convertToJson(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            sb.append("  {");

            int j = 0;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                sb.append("\"").append(entry.getKey()).append("\": ");
                Object value = entry.getValue();
                if (value == null) {
                    sb.append("null");
                } else if (value instanceof Number || value instanceof Boolean) {
                    sb.append(value);
                } else {
                    sb.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
                }

                if (j < row.size() - 1) {
                    sb.append(", ");
                }
                j++;
            }

            sb.append("}");
            if (i < data.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");

        return sb.toString();
    }
}
