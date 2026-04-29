package nexusai.core.rag.java.springai.document;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 文档处理组件接口
 * 提供文档加载、分割、存储功能
 */
public interface DocumentComponent {

    /**
     * 从字符串加载文档（自动分割）
     *
     * @param content 文档内容
     * @return 分割后的文档段落
     */
    List<Document> loadFromString(String content);

    /**
     * 从字符串加载文档（指定分隔符）
     *
     * @param content   文档内容
     * @param delimiter 分隔符（如 "\n\n" 表示按段落分割）
     * @return 分割后的文档段落
     */
    List<Document> loadFromString(String content, String delimiter);

    /**
     * 存储文档到向量库
     *
     * @param documents 文档列表
     */
    void store(List<Document> documents);

    /**
     * 加载并存储（便捷方法）
     *
     * @param content 文档内容
     * @return 存储的文档数量
     */
    int loadAndStore(String content);

    /**
     * 列出所有文档
     *
     * @return 文档列表
     */
    List<Document> listDocuments();

    /**
     * 删除指定文档
     *
     * @param docId 文档ID
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
}
