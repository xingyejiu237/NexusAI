package nexusai.core.rag.java.springai.document;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档处理组件实现
 * 基于 VectorStore + DocumentStorageAdapter，存储层可插拔
 *
 * 内置适配器：
 * - QdrantStorageAdapter: 自动装配（classpath 有 QdrantClient 时）
 * - RedisStorageAdapter: 自动装配（classpath 有 StringRedisTemplate 时）
 *
 * 配置项：
 * - rag.list-top-k: 列出文档时的检索数量，默认 100
 */
@Component
public class DocumentComponentImpl implements DocumentComponent {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DocumentStorageAdapter storageAdapter;

    @Value("${rag.list-top-k:100}")
    private int listTopK;

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

    /**
     * 带标题的文档分片，标题会被拼到每个分片前面，提高检索命中率
     */
    public List<Document> loadFromString(String content, String delimiter, String title) {
        String regex = delimiter.replace("\\n", "\n");
        return Arrays.stream(content.split(regex))
                .filter(segment -> !segment.trim().isEmpty())
                .map(segment -> {
                    String text = (title != null && !title.isBlank())
                            ? title + "\n" + segment.trim()
                            : segment.trim();
                    return new Document(text);
                })
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
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("所有文档内容")
                        .topK(listTopK)
                        .similarityThreshold(0.0)
                        .build()
        );
    }

    @Override
    public void deleteDocument(String docId) {
        storageAdapter.deleteDocument(docId);
    }

    @Override
    public void clearAll() {
        storageAdapter.clearAll();
    }

    @Override
    public long count() {
        return storageAdapter.count();
    }

    /**
     * 获取当前使用的存储适配器名称
     */
    public String getStorageAdapterName() {
        return storageAdapter.getName();
    }
}
