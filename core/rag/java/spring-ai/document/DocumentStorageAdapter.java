package nexusai.core.rag.java.springai.document;

/**
 * 文档存储适配接口
 * 抽象文档的存储、删除、清空、计数操作，支持不同后端实现
 *
 * 内置实现：
 * - QdrantStorageAdapter: 基于 Qdrant 向量数据库
 * - RedisStorageAdapter: 基于 Redis
 */
public interface DocumentStorageAdapter {

    /**
     * 删除指定文档
     *
     * @param docId 文档 ID
     */
    void deleteDocument(String docId);

    /**
     * 清空所有文档
     */
    void clearAll();

    /**
     * 获取文档数量
     *
     * @return 文档数量
     */
    long count();

    /**
     * 获取适配器名称（用于日志和配置区分）
     *
     * @return 适配器名称，如 "qdrant", "redis"
     */
    String getName();
}
