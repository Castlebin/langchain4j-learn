package com.heller.lj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import com.heller.lj.service.FunctionCallService;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.PostConstruct;

@Configuration
public class AiAssistant {

    public interface Assistant {
        String chat(String message); // 普通的对话

        TokenStream chatStream(String message); // 流式响应的对话

        // 票务助手 ( 使用 @SystemMessage 来设置系统预定义的提示词 )
        @SystemMessage("""
                你是 九州航空公司 的 票务助手，你正在使用在线聊天的方式给用户提供服务。
                你必须以友好、乐于助人且愉快的方式来回答客户的问题。
                你可以帮助用户查询和取消订单。
                在为用户提供订单服务时，必须让用户提供姓名和订单号。
                用户必须讲中文。
                今天的日期是 {current_date}。""")
        TokenStream chatStream(@UserMessage String userMessage, @V("current_date") String currentDate);
    }

    public interface AssistantUnique {
        String chat(@MemoryId String memoryId, @UserMessage String message); // 普通的对话

        TokenStream chatStream(@MemoryId String memoryId, @UserMessage String message); // 流式响应的对话
    }

    public interface AssistantUniqueRedis {
        String chat(@MemoryId String memoryId, @UserMessage String message); // 普通的对话

        TokenStream chatStream(@MemoryId String memoryId, @UserMessage String message); // 流式响应的对话
    }

    public interface AiRAGAssitant {
        String chat(@MemoryId String memoryId, @UserMessage String message); // 普通的对话

        TokenStream chatStream(@MemoryId String memoryId, @UserMessage String message); // 流式响应的对话
    }

    @Bean
    public Assistant assistant(ChatModel qwenChatModel, StreamingChatModel qwenStreamingChatModel,
            FunctionCallService functionCallService) {
        // 使用 ChatMemory (存储对话的上下文，默认是在内存中)
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10); // 设置 最多存储 10 条对话

        Assistant assistant = AiServices.builder(Assistant.class)  // 帮助生成一个 Assistant 的动态代理
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemory(chatMemory) // 设置对话的上下文，使用 ChatMemory 来保存对话的上下文
                .tools(functionCallService) // 设置 Function Call 工具，这里可以设置多个
                .build();

        return assistant;
    }

    @Bean
    public AssistantUnique assistantUnique(ChatModel qwenChatModel,
            StreamingChatModel qwenStreamingChatModel) {
        AssistantUnique assistant = AiServices.builder(AssistantUnique.class)  // 帮助生成一个 AssitantUnique 的动态代理
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .maxMessages(10)
                        .id(memoryId)
                        .build()) // 设置对话的上下文，使用 ChatMemory 来保存对话的上下文。使用 memoryId 来区分不同的对话
                .build();

        return assistant;
    }

    @Bean
    public AssistantUniqueRedis assistantRedis(ChatModel qwenChatModel,
            StreamingChatModel qwenStreamingChatModel,
            RedisTemplate redisTemplate) {
        // 使用 RedisChatMemoryStore (存储对话的上下文，默认是在内存中)
        ChatMemoryStore chatMemory = new RedisChatMemoryStore(redisTemplate);

        AssistantUniqueRedis assistantRedis = AiServices.builder(AssistantUniqueRedis.class)  // 帮助生成一个 Assistant 的动态代理
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .maxMessages(10)
                        .id(memoryId)
                        .chatMemoryStore(chatMemory) // 设置对话的上下文，使用 RedisChatMemoryStore 来保存对话的上下文
                        .build()) // 设置对话的上下文，使用 ChatMemory 来保存对话的上下文。使用 memoryId 来区分不同的对话
                .build();

        return assistantRedis;
    }

    @Bean
    public AiRAGAssitant aiRAGAssitant(ChatModel qwenChatModel,
            StreamingChatModel qwenStreamingChatModel,
            ContentRetriever contentRetriever,
            RedisTemplate redisTemplate) {
        // 使用 RedisChatMemoryStore (存储对话的上下文，默认是在内存中)
        ChatMemoryStore chatMemory = new RedisChatMemoryStore(redisTemplate);

        return AiServices.builder(AiRAGAssitant.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .maxMessages(10)
                        .id(memoryId)
                        .chatMemoryStore(chatMemory)
                        .build())
                .contentRetriever(
                        contentRetriever)  // 使用的 内容检索器 （ RAG ）  ，对话时会自动的去 contentRetriever
                // 中检索相关的信息，并且跟用户输入的信息结合起来，提供给大语言模型进行生成
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore embeddingStore, EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3) // 返回前 3 个最相似的结果
                .minScore(0.7) // 最小相似度分数 （小于它的将被过滤掉）
                .build();
    }

    @Bean
    public EmbeddingStore embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

}
