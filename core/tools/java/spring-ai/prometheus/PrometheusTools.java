package nexusai.core.tools.java.springai.prometheus;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Map;

/**
 * Prometheus 指标查询工具
 * 通过 @Tool 注解注册为 Spring AI 可调用的工具
 *
 * 使用方式：
 * 1. 配置 prometheus.metrics 定义指标映射（或使用默认的 Spring Boot Actuator 指标）
 * 2. 将此类实例化为 @Component，注入 PrometheusService
 *
 * 配置项（通过构造函数传入 metrics Map）：
 * - key: 指标类型名（如 "cpu_usage"）
 * - value: PromQL 模板，{service} 会被替换为实际服务名
 *
 * 默认指标基于 Spring Boot Actuator (Micrometer)：
 * - cpu_usage: process_cpu_usage{job="{service}"}
 * - memory_usage: sum(jvm_memory_used_bytes{job="{service}",area="heap"}) / sum(jvm_memory_max_bytes{job="{service}",area="heap"}) * 100
 * - qps: sum(rate(http_server_requests_seconds_count{job="{service}"}[5m]))
 * - latency: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{job="{service}"}[5m])) by (le))
 * - error_rate: sum(rate(http_server_requests_seconds_count{job="{service}",status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count{job="{service}"}[5m])) * 100
 * - gc_count: sum(rate(jvm_gc_pause_seconds_count{job="{service}"}[5m])) by (cause)
 */
public class PrometheusTools {

    private final PrometheusService prometheusService;
    private final Map<String, String> metrics;

    /**
     * 使用默认指标模板
     *
     * @param prometheusService Prometheus 服务
     */
    public PrometheusTools(PrometheusService prometheusService) {
        this(prometheusService, defaultMetrics());
    }

    /**
     * 使用自定义指标模板
     *
     * @param prometheusService  Prometheus 服务
     * @param customMetrics      自定义指标映射（key=指标类型, value=PromQL模板，{service}为占位符）
     */
    public PrometheusTools(PrometheusService prometheusService, Map<String, String> customMetrics) {
        this.prometheusService = prometheusService;
        this.metrics = customMetrics;
    }

    @Tool(description = "查询 Prometheus 监控指标，获取服务的 CPU 使用率、内存使用率、QPS、响应延迟等运行数据。当用户询问服务运行状况、性能指标、是否卡顿等问题时使用此工具。")
    public String prometheusQuery(
            @ToolParam(description = "服务名称(即Prometheus的job名)") String service,
            @ToolParam(description = "指标类型，可选值: cpu_usage, memory_usage, qps, latency, error_rate, gc_count") String metric,
            @ToolParam(description = "查询时间范围，如 5m(5分钟), 30m(30分钟), 1h(1小时), 6h(6小时), 24h(24小时)，默认1h") String duration
    ) {
        String dur = (duration == null || duration.isBlank()) ? "1h" : duration;
        String promql = buildPromQL(service, metric);
        return prometheusService.queryRange(promql, dur);
    }

    /**
     * 根据指标类型构建 PromQL 表达式
     */
    private String buildPromQL(String service, String metric) {
        if (metric == null) metric = "";

        String template = metrics.get(metric.toLowerCase());
        if (template != null) {
            return template.replace("{service}", service);
        }

        // 默认返回 up 指标
        return "up{job=\"" + service + "\"}";
    }

    /**
     * 默认指标模板（基于 Spring Boot Actuator / Micrometer）
     */
    public static Map<String, String> defaultMetrics() {
        return Map.of(
                "cpu_usage", "process_cpu_usage{job=\"{service}\"}",
                "memory_usage", "sum(jvm_memory_used_bytes{job=\"{service}\",area=\"heap\"}) / sum(jvm_memory_max_bytes{job=\"{service}\",area=\"heap\"}) * 100",
                "qps", "sum(rate(http_server_requests_seconds_count{job=\"{service}\"}[5m]))",
                "latency", "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{job=\"{service}\"}[5m])) by (le))",
                "error_rate", "sum(rate(http_server_requests_seconds_count{job=\"{service}\",status=~\"5..\"}[5m])) / sum(rate(http_server_requests_seconds_count{job=\"{service}\"}[5m])) * 100",
                "gc_count", "sum(rate(jvm_gc_pause_seconds_count{job=\"{service}\"}[5m])) by (cause)"
        );
    }
}
