package nexusai.core.rag.java.springai.document;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文档处理组件实现
 * 基于 VectorStore 和 Redis 实现文档管理
 */
@Component
public class DocumentComponentImpl implements DocumentComponent {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String DOC_PREFIX = "doc:";
    private static final String INDEX_NAME = "rag-index";
    private static final String DEFAULT_DELIMITER = "\\n\\n";

    @Override
    public List<Document> loadFromString(String content) {
        return loadFromString(content, DEFAULT_DELIMITER);
    }

    @Override
    public List<Document> loadFromString(String content, String delimiter) {
        String regex = delimiter.replace("\\n", "\n");
        return Arrays.stream(content.split(regex))
                .filter(segment -> !segment.trim().isEmpty())
                .map(segment -> new Document(segment.trim()))
                .collect(Collectors.toList());
    }

    @Override
    public void store(List<Document> documents) {
        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
    }

    @Override
    public int loadAndStore(String content) {
        List<Document> documents = loadFromString(content);
        store(documents);
        return documents.size();
    }

    @Override
    public List<Document> listDocuments() {
        // 使用通用查询词检索所有文档
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("所有文档内容")
                        .topK(100)
                        .similarityThreshold(0.0)
                        .build()
        );
    }

    @Override
    public void deleteDocument(String docId) {
        redisTemplate.delete(DOC_PREFIX + docId);
    }

    @Override
    public void clearAll() {
        // 删除所有 doc:* key
        Set<String> keys = redisTemplate.keys(DOC_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        // 删除索引
        redisTemplate.delete(INDEX_NAME);
    }

    @Override
    public long count() {
        Set<String> keys = redisTemplate.keys(DOC_PREFIX + "*");
        return keys == null ? 0 : keys.size();
    }
}
