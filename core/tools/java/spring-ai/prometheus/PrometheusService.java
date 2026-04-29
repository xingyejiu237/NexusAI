package nexusai.core.tools.java.springai.prometheus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Prometheus HTTP 客户端服务
 *
 * 封装 Prometheus HTTP API 的范围查询和即时查询，
 * 自动将 JSON 响应格式化为人类可读摘要。
 *
 * 使用方式：
 * 1. 创建 WebClient Bean（baseUrl 设为 Prometheus 地址）
 * 2. 注入 PrometheusService
 *
 * 踩坑记录：
 * - WebClient .uri(String) 会对 query 参数二次编码
 * - WebClient .uri(new URI(相对路径)) 不会解析 baseUrl，请求发到 localhost:80
 * - WebClient .queryParam(name, value) 会把 value 中的 {xxx} 当模板变量展开
 * - 正确姿势：UriBuilder + "{placeholder}" + .build(实际值)
 */
public class PrometheusService {

    private static final Logger log = LoggerFactory.getLogger(PrometheusService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final PrometheusConfig config;

    public PrometheusService(WebClient prometheusWebClient, ObjectMapper objectMapper, PrometheusConfig config) {
        this.webClient = prometheusWebClient;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    /**
     * 执行 PromQL 范围查询
     *
     * @param promql   PromQL 表达式（如 process_cpu_usage{job="my-service"}）
     * @param duration 时间范围（如 "30m", "1h", "24h"），null 使用默认值
     * @return 格式化后的查询结果
     */
    public String queryRange(String promql, String duration) {
        long end = System.currentTimeMillis() / 1000;
        String dur = (duration != null && !duration.isBlank()) ? duration : config.getDefaultDuration();
        long start = end - parseDuration(dur);
        long step = config.getDefaultStepSeconds() > 0
                ? config.getDefaultStepSeconds()
                : Math.max(60, (end - start) / 100);

        log.debug("[Prometheus] queryRange PromQL={}, duration={}, step={}", promql, dur, step);

        try {
            // PromQL 含花括号如 {job="xxx"}，必须用 URI 模板变量传入，
            // 否则 UriBuilder 会把 {job="xxx"} 当成模板变量展开导致报错
            String json = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/query_range")
                            .queryParam("query", "{promql}")
                            .queryParam("start", start)
                            .queryParam("end", end)
                            .queryParam("step", step)
                            .build(promql))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return formatPrometheusResponse(json, promql);
        } catch (Exception e) {
            log.error("Prometheus query failed: {}", e.getMessage());
            return "查询 Prometheus 失败: " + e.getMessage();
        }
    }

    /**
     * 执行 PromQL 即时查询
     *
     * @param promql PromQL 表达式
     * @return 格式化后的查询结果
     */
    public String queryInstant(String promql) {
        log.debug("[Prometheus] queryInstant PromQL={}", promql);

        try {
            String json = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/query")
                            .queryParam("query", "{promql}")
                            .build(promql))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return formatPrometheusResponse(json, promql);
        } catch (Exception e) {
            log.error("Prometheus instant query failed: {}", e.getMessage());
            return "查询 Prometheus 失败: " + e.getMessage();
        }
    }

    /**
     * 格式化 Prometheus JSON 响应为人类可读摘要
     */
    private String formatPrometheusResponse(String json, String promql) {
        if (json == null || json.isEmpty()) {
            return "Prometheus 返回空结果";
        }

        try {
            JsonNode root = objectMapper.readTree(json);

            String status = root.path("status").asText();
            if (!"success".equals(status)) {
                String error = root.path("error").asText("未知错误");
                return "Prometheus 查询错误: " + error;
            }

            JsonNode result = root.path("data").path("result");
            if (result.isEmpty()) {
                return "没有找到匹配的指标数据 (PromQL: " + promql + ")";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Prometheus 查询结果:\n");

            for (JsonNode item : result) {
                JsonNode metric = item.path("metric");
                String instance = metric.path("instance").asText("unknown");
                String job = metric.path("job").asText("unknown");

                sb.append("  服务[").append(job).append("] 实例[").append(instance).append("]:\n");

                JsonNode values = item.path("values");
                if (values.isArray() && !values.isEmpty()) {
                    JsonNode latest = values.get(values.size() - 1);
                    double value = latest.get(1).asDouble();
                    sb.append("    当前值: ").append(formatValue(value));

                    if (values.size() > 1) {
                        JsonNode earliest = values.get(0);
                        double firstValue = earliest.get(1).asDouble();
                        double change = value - firstValue;
                        String trend = change > 0.05 ? "↑ 上升" : change < -0.05 ? "↓ 下降" : "→ 平稳";
                        sb.append(", 趋势: ").append(trend);
                    }
                    sb.append("\n");
                } else {
                    JsonNode value = item.path("value");
                    if (!value.isEmpty()) {
                        double v = value.get(1).asDouble();
                        sb.append("    当前值: ").append(formatValue(v)).append("\n");
                    }
                }
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("格式化 Prometheus 响应失败: {}", e.getMessage());
            return "解析 Prometheus 响应失败: " + e.getMessage();
        }
    }

    /**
     * 格式化数值，保留合适的小数位
     */
    private String formatValue(double value) {
        if (Math.abs(value) >= 100) {
            return String.format("%.1f", value);
        } else if (Math.abs(value) >= 1) {
            return String.format("%.2f", value);
        } else {
            return String.format("%.4f", value);
        }
    }

    /**
     * 解析持续时间字符串为秒数
     */
    private long parseDuration(String duration) {
        if (duration == null || duration.isBlank()) {
            return 3600; // 默认 1 小时
        }

        duration = duration.trim().toLowerCase();
        try {
            if (duration.endsWith("s")) {
                return Long.parseLong(duration.substring(0, duration.length() - 1));
            } else if (duration.endsWith("m")) {
                return Long.parseLong(duration.substring(0, duration.length() - 1)) * 60;
            } else if (duration.endsWith("h")) {
                return Long.parseLong(duration.substring(0, duration.length() - 1)) * 3600;
            } else if (duration.endsWith("d")) {
                return Long.parseLong(duration.substring(0, duration.length() - 1)) * 86400;
            } else {
                return Long.parseLong(duration);
            }
        } catch (NumberFormatException e) {
            log.warn("无法解析持续时间: {}, 使用默认值 1h", duration);
            return 3600;
        }
    }
}
