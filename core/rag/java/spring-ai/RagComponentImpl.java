package nexusai.core.rag.java.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 组件实现类
 * 封装向量检索和 LLM 生成的完整 RAG 流程
 *
 * 配置项：
 * - rag.top-k: 检索文档数量，默认 3
 * - rag.similarity-threshold: 相似度阈值，默认 0.0
 * - rag.system-prompt: 自定义系统提示（需包含 %s 占位符，用于插入知识库内容）
 * - rag.empty-response: 知识库为空时的默认回复
 */
@Component
public class RagComponentImpl implements RagComponent {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    @Value("${rag.top-k:3}")
    private int defaultTopK;

    @Value("${rag.similarity-threshold:0.0}")
    private double defaultSimilarityThreshold;

    @Value("${rag.system-prompt:}")
    private String customDefaultPrompt;

    @Value("${rag.empty-response:知识库中未找到相关信息，请先上传文档到知识库。}")
    private String emptyResponse;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是知识库助手。根据以下【知识库内容】回答用户问题。

            【回答规则】
            1. 基于提供的【知识库内容】回答问题
            2. 如果知识库中没有相关信息，直接说明"根据知识库，我找不到相关信息"
            3. 不要编造任何不在知识库中的内容
            4. 回答要简洁实用，直接给出解决方案

            【知识库内容】：
            %s
            """;

    public RagComponentImpl(VectorStore vectorStore, ChatClient chatClient) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
    }

    @Override
    public String query(String query) {
        return query(query, null);
    }

    @Override
    public String query(String query, String customSystemPrompt) {
        List<String> contexts = retrieve(query);

        if (contexts.isEmpty()) {
            return emptyResponse;
        }

        String context = String.join("\n\n", contexts);

        // 优先级：方法参数 > 配置文件 > 默认值
        String prompt = customSystemPrompt != null ? customSystemPrompt
                : !customDefaultPrompt.isBlank() ? customDefaultPrompt
                : DEFAULT_SYSTEM_PROMPT;
        String systemPrompt = prompt.formatted(context);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(query)
                .call()
                .content();
    }

    @Override
    public List<String> retrieve(String query, int topK) {
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(defaultSimilarityThreshold)
                        .build()
        );

        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> retrieve(String query) {
        return retrieve(query, defaultTopK);
    }
}
