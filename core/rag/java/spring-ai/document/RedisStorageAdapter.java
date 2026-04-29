package nexusai.core.rag.java.springai.document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Redis 存储适配器
 * 基于 Redis 实现文档的删除、清空、计数
 *
 * 配置项：
 * - rag.redis.doc-prefix: 文档 key 前缀，默认 "doc:"
 * - rag.redis.index-name: 索引名，默认 "rag-index"
 */
@Component
@ConditionalOnBean(StringRedisTemplate.class)
public class RedisStorageAdapter implements DocumentStorageAdapter {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${rag.redis.doc-prefix:doc:}")
    private String docPrefix;

    @Value("${rag.redis.index-name:rag-index}")
    private String indexName;

    @Override
    public void deleteDocument(String docId) {
        redisTemplate.delete(docPrefix + docId);
    }

    @Override
    public void clearAll() {
        Set<String> keys = redisTemplate.keys(docPrefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        redisTemplate.delete(indexName);
    }

    @Override
    public long count() {
        Set<String> keys = redisTemplate.keys(docPrefix + "*");
        return keys == null ? 0 : keys.size();
    }

    @Override
    public String getName() {
        return "redis";
    }
}
