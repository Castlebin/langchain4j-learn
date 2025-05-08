package com.heller.lj.start;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class TestAiAgent {

    private static AiAgent agent;

    @BeforeAll
    static void init() {
        // OpenAI
        // 使用 demo api key
        ChatModel openai = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")     // 使用 demo api key
                .modelName("gpt-4o-mini")
                .build();

        // 通义千问
        ChatModel qwen = QwenChatModel.builder()
                .apiKey("sk-2833a07601ef4c6bbed1fb41c50c2fda")
                .modelName("qwen-max")
                .build();

        // deepseek
        // 使用 硅基流动 的 api 地址
        // api key
        ChatModel deepseek = OpenAiChatModel.builder()
                .baseUrl("https://api.siliconflow.cn/v1")  // 使用 硅基流动 的 api 地址
                .apiKey("sk-huuqagfsszvnqkhqnxvdadkmrxvpvhokenxvxdwysxdpzkfg")     // api key
                .modelName("deepseek-ai/DeepSeek-V3")
                .build();

        AiAgentDispatcher dispatcher = AiServices.builder(AiAgentDispatcher.class)
                .chatModel(openai)
                .build();
        ChatBot chatBot = AiServices.builder(ChatBot.class)
                .chatModel(deepseek)
                .build();
        CodingAssistant codingAssistant = AiServices.builder(CodingAssistant.class)
                .chatModel(qwen)
                .build();

        agent = new AiAgent(dispatcher, chatBot, codingAssistant);
    }

    @Test
    void testChat() {
        String response = agent.handle("请给我讲一个笑话，50个字以内");
        System.out.println(response);
        System.out.println("-----------------");

        String response1 = agent.handle("请给写一个快速排序的代码，使用 Java 语言编写");
        System.out.println(response1);
        System.out.println("-----------------");

        String response2 = agent.handle("给我画一只猫");
        System.out.println(response2);
        System.out.println("-----------------");

        String response3 = agent.handle("你是谁？你能干些什么？");
        System.out.println(response3);
    }

    // 一个简单的 AI Agent ，将各种 AI 助手组合在一起
    // 通过任务分发器来分发任务
    private static class AiAgent {
        private final AiAgentDispatcher dispatcher;
        private final ChatBot chatBot;
        private final CodingAssistant codingAssistant;

        public AiAgent(AiAgentDispatcher dispatcher, ChatBot chatBot, CodingAssistant codingAssistant) {
            this.dispatcher = dispatcher;
            this.chatBot = chatBot;
            this.codingAssistant = codingAssistant;
        }

        public String handle(String message) {
            // dispathcher 来识别任务类型、分配任务给不同的 AI 来执行
            TaskType taskType = dispatcher.getTaskType(message);
            System.out.println("任务类型: " + taskType);

            return switch (taskType) {
                case CHAT -> chatBot.chat(message);
                case CODING -> codingAssistant.chat(message);
                case GEN_IMAGE ->
                    // 处理生成图片的逻辑
                        "当前还不支持生成图片";
                case OTHER ->
                    // 处理其他任务的逻辑
                        chatBot.chat(message);
            };
        }
    }

    // 任务分发器
    public interface AiAgentDispatcher {
        @UserMessage("以下文本是什么任务？{{it}}")
        TaskType getTaskType(String message);
    }

    public interface ChatBot {
        @SystemMessage("你是一个聊天助手，你的任务是和用户进行对话。")
        String chat(String message);
    }

    public interface CodingAssistant {
        @SystemMessage("你是一个编程助手，你的任务是帮助用户编写代码。")
        String chat(String message);
    }

}
