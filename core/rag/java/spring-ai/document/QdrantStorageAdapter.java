package nexusai.core.rag.java.springai.document;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Qdrant 存储适配器
 * 基于 Qdrant 向量数据库实现文档的删除、清空、计数
 *
 * 配置项：
 * - rag.qdrant.collection-name: 集合名称，默认 "rag_collection"
 * - rag.qdrant.vector-size: 向量维度，默认 1024
 */
@Component
@ConditionalOnBean(QdrantClient.class)
public class QdrantStorageAdapter implements DocumentStorageAdapter {

    private static final Logger log = LoggerFactory.getLogger(QdrantStorageAdapter.class);

    private final VectorStore vectorStore;
    private final QdrantClient qdrantClient;

    @Value("${rag.qdrant.collection-name:rag_collection}")
    private String collectionName;

    @Value("${rag.qdrant.vector-size:1024}")
    private int vectorSize;

    public QdrantStorageAdapter(VectorStore vectorStore, QdrantClient qdrantClient) {
        this.vectorStore = vectorStore;
        this.qdrantClient = qdrantClient;
    }

    @Override
    public void deleteDocument(String docId) {
        vectorStore.delete(List.of(docId));
    }

    @Override
    public void clearAll() {
        try {
            qdrantClient.deleteCollectionAsync(collectionName).get();
            qdrantClient.createCollectionAsync(
                    Collections.CreateCollection.newBuilder()
                            .setCollectionName(collectionName)
                            .setVectorsConfig(Collections.VectorsConfig.newBuilder()
                                    .setParams(Collections.VectorParams.newBuilder()
                                            .setSize(vectorSize)
                                            .setDistance(Collections.Distance.Cosine)
                                            .build())
                                    .build())
                            .build()
            ).get();
            log.info("[QdrantStorageAdapter] 集合 {} 已重建", collectionName);
        } catch (Exception e) {
            throw new RuntimeException("清空 Qdrant 集合失败: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try {
            var info = qdrantClient.getCollectionInfoAsync(collectionName).get();
            return info.getPointsCount();
        } catch (Exception e) {
            log.warn("[QdrantStorageAdapter] 获取文档数量失败: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public String getName() {
        return "qdrant";
    }
}
