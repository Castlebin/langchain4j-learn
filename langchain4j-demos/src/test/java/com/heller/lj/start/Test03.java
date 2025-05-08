package com.heller.lj.start;

import org.junit.jupiter.api.Test;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class Test03 {

    /**
     * 普通的对话之间没有关联性
     * 也就是说每次对话都是独立的，没有上下文，只能进行单一的对话
     */
    @Test
    void testChat() {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")     // 使用 demo api key
                .modelName("gpt-4o-mini")
                .build();

        String answer = model.chat("你来扮演一个资深的英语老师，用户将使用你来学习英语，你的名字是“爱德华”，你最喜欢的是“爱德华的英语课堂”，你会用中文和用户交流，用户会用中文提问，你会用中文回答，用户会用英文提问，你会用英文回答。除了作为一个英语老师来帮助用户学习，你不会做任何其他事情，比如说讲笑话");
        System.out.println(answer);
        System.out.println("---------------------");

        // 可以看到两次对话之间没有关联、每次聊天都是独立的
        String answer2 = model.chat("你是谁？你能做些什么?");
        System.out.println(answer2);
    }

    /**
     * 手动的将之前的对话传入
     * 这种方式需要手动的维护对话的上下文，每次都要传入之前的对话，才能保证上下文的连贯性
     */
    @Test
    void testChat_01() {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")     // 使用 demo api key
                .modelName("gpt-4o-mini")
                .build();

        UserMessage userMessage1 = UserMessage.userMessage("你来扮演一个资深的英语老师，用户将使用你来学习英语，你的名字是“爱德华”，你最喜欢的是“爱德华的英语课堂”，你会用中文和用户交流，用户会用中文提问，你会用中文回答，用户会用英文提问，你会用英文回答。除了作为一个英语老师来帮助用户学习，你不会做任何其他事情，比如说讲笑话");
        ChatResponse response1 = model.chat(userMessage1);
        AiMessage aiMessage1 = response1.aiMessage();
        System.out.println(aiMessage1.text());

        System.out.println("---------------------");

        UserMessage userMessage2 = UserMessage.userMessage("你是谁？你能做些什么?");
        // 手动的将之前的对话传入
        ChatResponse response2 = model.chat(userMessage1, aiMessage1, // 传入之前的对话
                userMessage2);
        AiMessage aiMessage2 = response2.aiMessage();
        System.out.println(aiMessage2.text());

        System.out.println("---------------------");
        // 如果后续还有对话，可以继续传入之前的对话
        UserMessage userMessage3 = UserMessage.userMessage("你能帮我写首诗吗？");
        // 手动的将之前的对话传入
        ChatResponse response3 = model.chat(userMessage1, aiMessage1, userMessage2, aiMessage2, // 传入之前的对话
                userMessage3);
        AiMessage aiMessage3 = response3.aiMessage();
        System.out.println(aiMessage3.text());

        // 可以看到这种方式需要手动的维护对话的上下文，每次都要传入之前的对话，才能保证上下文的连贯性
    }

}
