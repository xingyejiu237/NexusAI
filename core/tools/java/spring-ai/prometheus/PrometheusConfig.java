package nexusai.core.tools.java.springai.prometheus;

/**
 * Prometheus 查询配置
 *
 * 配置项：
 * - prometheus.url: Prometheus 服务地址，默认 http://localhost:9090
 * - prometheus.default-duration: 默认查询时间范围，默认 1h
 * - prometheus.default-step-seconds: 默认查询步长（秒），0 表示自动计算
 */
public class PrometheusConfig {

    private String url = "http://localhost:9090";
    private String defaultDuration = "1h";
    private int defaultStepSeconds = 0;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDefaultDuration() {
        return defaultDuration;
    }

    public void setDefaultDuration(String defaultDuration) {
        this.defaultDuration = defaultDuration;
    }

    public int getDefaultStepSeconds() {
        return defaultStepSeconds;
    }

    public void setDefaultStepSeconds(int defaultStepSeconds) {
        this.defaultStepSeconds = defaultStepSeconds;
    }
}
