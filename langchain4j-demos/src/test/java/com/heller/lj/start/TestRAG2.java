package com.heller.lj.start;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * 测试 RAG
 * <p>
 * RAG 是什么？
 * RAG 是 Retrieval-Augmented Generation 的缩写，
 * 是一种结合了检索和生成的自然语言处理技术。它
 * 通过检索相关信息来增强生成模型的能力，从而提高生成文本的质量和准确性。
 * <p>
 * 具体过程是：
 * 0. 将知识库转化为 Embedding 向量，存放到向量数据库中。
 * 1. 检索：从一个大的知识库中检索出与输入相关的信息。
 * 2. 生成：将检索到的信息与用户的输入信息结合起来，使用生成模型生成最终的输出。
 *
 * 将数据进行分块可以提高检索的效率和准确性。这就是进行文本分割的原因
 */
public class TestRAG2 {

    private static QwenEmbeddingModel EMBEDDING_MODEL;
    private static InMemoryEmbeddingStore<TextSegment> EMBEDDING_STORE;

    @BeforeAll
    public static void init() {
        // 简单用作演示，这里我们使用的是 内存向量数据库
        // 当然也可以使用 Neo4j、ES、PostgreSQL 、Redis 等向量数据库，实际生产环境中使用
        // 向量数据库的使用方式都是一样的
        EMBEDDING_STORE = new InMemoryEmbeddingStore<>();

        // 使用 千问 的 Embedding 模型来做知识库的向量化 （ Embedding ）
        EMBEDDING_MODEL = QwenEmbeddingModel.builder()
                .apiKey("sk-2833a07601ef4c6bbed1fb41c50c2fda")
                .build();

        // 从文件系统中加载原始知识库数据
        List<Document> documents = getKnowledge();
        // 使用分割器将文档 分割 成多个文本片段
        List<TextSegment> segments = splitDocuments(documents);

        // 0. 利用 向量模型（Embedding 模型） 将知识库转化为 Embedding 向量，存放到向量数据库中
        // EMBEDDING_STORE.addAll(EMBEDDING_MODEL.embedAll(segments).content());  (可以直接使用这句将全部数据转化为向量并添加到向量数据库中)
        for (TextSegment segment : segments) {
            Embedding embedding = EMBEDDING_MODEL.embed(segment).content();

            EMBEDDING_STORE.add(embedding, segment);
        }
    }

    /**
     * 简单演示了一下向量数据库的使用
     */
    @Test
    void test() {
        // 0. 将用户输入的文本转化为向量 （要使用同样的 Embedding 模型）
        String userInput = "退票需要多少钱？";
        Embedding userEmbedding = EMBEDDING_MODEL.embed(userInput).content();

        // 1. 检索：从知识库中检索出与用户输入相关的信息
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(userEmbedding)
                //.maxResults(3) // 返回前 3 个最相似的结果
                //.minScore(0.5) // 最小相似度分数 （小于它的将被过滤掉）
                .build();
        EmbeddingSearchResult<TextSegment> result = EMBEDDING_STORE.search(request);
        // 输出一下查询到的结果 (相似度匹配)
        result.matches().forEach(embeddingMatched -> {
                    System.out.println("匹配到的文本信息：" + embeddingMatched.embedded().text());
                    System.out.println("匹配到的相似度分数：" + embeddingMatched.score());
                    System.out.println("---------------------");
                }
        );

        // RAG 检索、增强、生成
        ChatModel model = QwenChatModel.builder()
                .apiKey("sk-2833a07601ef4c6bbed1fb41c50c2fda")
                .modelName("qwen-max")
                .build();

        // 2. 生成：将检索到的信息与用户的输入信息结合起来，使用生成模型生成最终的输出。
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(EMBEDDING_STORE)
                .embeddingModel(EMBEDDING_MODEL)
                .maxResults(3) // 返回前 3 个最相似的结果
                .minScore(0.7) // 最小相似度分数 （小于它的将被过滤掉）
                .build();
        AiRAGAssitant aiRAGAssitant = AiServices.builder(AiRAGAssitant.class)  // 生成我们的 AiRAGAssitant 代理
                .chatModel(model)  // 使用的 语言模型
                .contentRetriever(
                        contentRetriever)  // 使用的 内容检索器 （ RAG ）  ，对话时会自动的去 contentRetriever
                // 中检索相关的信息，并且跟用户输入的信息结合起来，提供给大语言模型进行生成
                .build();

        // 直接返回文本信息
        String answer = aiRAGAssitant.chat(userInput);
        System.out.println("最终的回答是：" + answer);
    }

    public interface AiRAGAssitant {
        String chat(String userInput); // 直接返回文本信息

        TokenStream chatStream(String message); // 流式返回文本信息
    }


    /**
     * 使用文件系统作为知识库数据来源
     */
    private static List<Document> getKnowledge() {
        // 使用 ClassPathDocumentLoader 来加载文档，使用 TextDocumentParser 来解析文档
        DocumentParser documentParser = new TextDocumentParser();
        List<Document> documents = ClassPathDocumentLoader.loadDocumentsRecursively("docs/", documentParser);
        return documents;
    }

    private static List<TextSegment> splitDocuments(List<Document> documents) {
        // 使用 某个文本分割器 来将文档分割成多个文本片段 （分块  chunk ）
        // 这里我们使用的是 DocumentByRegexSplitter 来分割文档
        DocumentByRegexSplitter splitter = new DocumentByRegexSplitter(
                "\\n\\d+\\.", // 正则表达式
                "\n",  // 保留换行符
                120, // 每个文本片段的最大长度
                10, // 允许重叠的最大字符数
                new DocumentByCharacterSplitter(100, 20) // 子分割器
        );

        List<TextSegment> result = new ArrayList<>();
        for (Document document : documents) {
            List<TextSegment> segments = splitter.split(document);
            result.addAll(segments);
        }
        return result;
    }

}
