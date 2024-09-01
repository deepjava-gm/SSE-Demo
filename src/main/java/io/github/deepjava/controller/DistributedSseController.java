package io.github.deepjava.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/dis/sse")
@CrossOrigin(origins = "*")
@Slf4j
public class DistributedSseController {

    @Resource(name = "userSSEMap")
    private ConcurrentHashMap<String, SseEmitter> userSSEMap;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private RedisMessageListenerContainer redisMessageListenerContainer;

    private final ChannelTopic topic = new ChannelTopic("sse-messages");

    @PostConstruct
    public void init() {
        // 订阅 Redis 频道
        redisMessageListenerContainer.addMessageListener(
                new MessageListenerAdapter((MessageListener) (message, pattern) -> {
                    String payload = new String(message.getBody(), StandardCharsets.UTF_8);

                    // 假设消息的格式为 "userId:message"
                    String[] parts = payload.split(":", 2);
                    if (parts.length == 2) {
                        String userId = parts[0];
                        String userMessage = parts[1];
                        // 发送消息给本地的 SSE 连接
                        SseEmitter emitter = userSSEMap.get(userId);
                        if (emitter != null) {
                            try {
                                String jsonMessage = String.format("{\"senderId\":\"%s\", \"message\":\"%s\"}", userId, userMessage);
                                emitter.send(jsonMessage);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                                userSSEMap.remove(userId);
                            }
                        }
                    }
                }), topic);
    }

    // 连接方法：为用户 ID 注册 SSE 链接
    @GetMapping("/connect/{userId}")
    public SseEmitter connect(@PathVariable String userId) {
        SseEmitter emitter = new SseEmitter(0L); // 设置超时时间为无限大
        userSSEMap.put(userId, emitter);
        // 连接正常关闭回调 移除连接
        emitter.onCompletion(() -> {
            userSSEMap.remove(userId);
            log.info("连接正常关闭回调 移除连接");
        });
        // 连接超时回调 移除连接
        emitter.onTimeout(() -> {
            userSSEMap.remove(userId);
            log.info("连接超时回调 移除连接");

        });
        // 连接出错回调 移除连接
        emitter.onError((e) -> {
            userSSEMap.remove(userId);
            log.info("连接出错回调 移除连接");
        });
        log.info("连接成功！");
        return emitter;
    }


    @GetMapping("/push/{userId}")
    public void push(@PathVariable String userId, String message) {
        // 将消息发布到 Redis 频道
        redisTemplate.convertAndSend(topic.getTopic(), userId + ":" + message);
    }
}
