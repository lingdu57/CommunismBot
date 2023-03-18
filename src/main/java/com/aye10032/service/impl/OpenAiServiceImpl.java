package com.aye10032.service.impl;

import com.aye10032.Zibenbot;
import com.aye10032.entity.AiResult;
import com.aye10032.entity.ChatContext;
import com.aye10032.entity.ChatMessage;
import com.aye10032.entity.ChatRequest;
import com.aye10032.service.OpenAiService;
import com.aye10032.utils.JsonUtils;
import com.aye10032.utils.StringUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.internal.sse.RealEventSource;
import okhttp3.sse.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author dazo66(sundazhong.sdz)
 * @date 2023/3/11 10:42
 **/
@Slf4j
@Service
public class OpenAiServiceImpl implements OpenAiService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private final OkHttpClient httpClient = new OkHttpClient();


    public OkHttpClient getOkHttpClient() {
        return httpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .proxy(Zibenbot.getProxy()).build();
    }

    @Override
    public AiResult chatGpt(String moduleType, ChatContext chatContext) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMessages(chatContext.getContext().stream().map(ChatRequest.Message::of).collect(Collectors.toList()));
        chatRequest.setModel(moduleType);
        MediaType mediaType = MediaType.parse("application/json");

        RequestBody requestBody = RequestBody.create(JsonUtils.toJson(chatRequest), mediaType);
        Request request = new Request.Builder().url("https://api.openai.com/v1/chat/completions").method("POST", requestBody).header("Authorization", "Bearer " + openaiApiKey).build();
        try {
            Response execute = getOkHttpClient().newCall(request).execute();
            String string = execute.body().string();
            return JsonUtils.fromJson(string, AiResult.class);
        } catch (Exception e) {
            log.error("调用openai失败：", e);
            return null;
        }
    }

    @Override
    public AiResult chatGptStream(String moduleType, ChatContext chatContext) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMessages(chatContext.getContext().stream().map(ChatRequest.Message::of).collect(Collectors.toList()));
        chatRequest.setModel(moduleType);
        chatRequest.setStream(true);
        MediaType mediaType = MediaType.parse("application/json");

        RequestBody requestBody = RequestBody.create(JsonUtils.toJson(chatRequest), mediaType);
        Request request = new Request.Builder().url("https://api.openai.com/v1/chat/completions").method("POST", requestBody).header("Authorization", "Bearer " + openaiApiKey).build();
        Object lock = new Object();
        List<AiResult> results = new CopyOnWriteArrayList<>();
        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(1, TimeUnit.DAYS)
                    .readTimeout(1, TimeUnit.DAYS)
                    .proxy(Zibenbot.getProxy()).build();
            AtomicBoolean failure = new AtomicBoolean(false);

            RealEventSource realEventSource = new RealEventSource(request, new EventSourceListener() {

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    if ("[DONE]".equals(data)) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                    results.add(JsonUtils.fromJson(data, AiResult.class));
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    synchronized (lock) {
                        lock.notify();
                    }
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    synchronized (lock) {
                        lock.notify();
                    }
                    failure.set(true);;
                }
            });
            realEventSource.connect(okHttpClient);
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            if (failure.get() || results.size() == 0) {
                throw new RuntimeException("调用出错");
            }
            return margeStreemResult(results);
        } catch (Exception e) {
            log.error("调用openai失败：", e);
            return null;
        }
    }

    private AiResult margeStreemResult(List<AiResult> results) {
        AiResult aiResult = new AiResult();
        StringBuilder content = new StringBuilder();
        String role = "assistant";
        for (AiResult result : results) {
            List<AiResult.Choice> choices = result.getChoices();
            for (AiResult.Choice choice : choices) {
                ChatMessage delta = choice.getDelta();
                if (delta == null) {
                    continue;
                }
                if (!StringUtils.isEmpty(delta.getRole())) {
                    role = delta.getRole();
                }
                if (!StringUtils.isEmpty(delta.getContent())) {
                    content.append(delta.getContent());
                }
            }
        }
        aiResult.setCreated(results.get(0).getCreated());
        aiResult.setId(results.get(0).getId());
        aiResult.setModel(results.get(0).getModel());
        aiResult.setObject(results.get(0).getObject());
        ChatMessage message = ChatMessage.of(role, content.toString());
        AiResult.Choice choice = new AiResult.Choice();
        choice.setMessage(message);
        aiResult.setChoices(Collections.singletonList(choice));
        return aiResult;
    }

    public static void main(String[] args) {
        OpenAiServiceImpl openAiService = new OpenAiServiceImpl();
        openAiService.openaiApiKey = "=";
        ChatContext chatContext = new ChatContext();
        chatContext.setContext(Lists.newArrayList(ChatMessage.of("user", "What is the OpenAI mission?")));
//        AiResult aiResult = openAiService.chatGpt("gpt-3.5-turbo", chatContext);
        AiResult aiResult = openAiService.chatGptStream("gpt-3.5-turbo", chatContext);
        System.out.println(JsonUtils.toJson(aiResult));
    }

}
