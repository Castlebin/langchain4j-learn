package com.heller.lj.start;

import org.junit.jupiter.api.Test;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class Test01 {

    @Test
    void testChat() { // OpenAI
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")     // 使用 demo api key
                .modelName("gpt-4o-mini")
                .build();
        String answer = model.chat("你好，你是谁？你能做些什么？");
        System.out.println(answer);
    }

    @Test
    void testChatDeepseek() { // deepseek
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.siliconflow.cn/v1")  // 使用 硅基流动 的 api 地址
                .apiKey("sk-huuqagfsszvnqkhqnxvdadkmrxvpvhokenxvxdwysxdpzkfg")     // deepseek api key
                .modelName("deepseek-ai/DeepSeek-V3")
                .build();
        String answer = model.chat("你好，你是谁？你能做些什么？");
        System.out.println(answer);
    }

    @Test
    void testChatQwen() { // 通义千问
        ChatModel model = QwenChatModel.builder()
                .apiKey("sk-2833a07601ef4c6bbed1fb41c50c2fda")
                .modelName("qwen-max")
                .build();
        String answer = model.chat("你好，你是谁？你能做些什么？");
        System.out.println(answer);
    }

    @Test
    void testCharOllama() { // Ollama
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("deepseek-r1:1.5b")
                .build();
        String answer = model.chat("你好，你是谁？你能做些什么？");
        System.out.println(answer);
    }

}
