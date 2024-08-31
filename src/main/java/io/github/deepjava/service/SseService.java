package io.github.deepjava.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseService {

    @Resource(name = "userSSEMap")
    private ConcurrentHashMap<String, SseEmitter> clients;

    public void extracted(String userId, String message) {
        SseEmitter emitter = clients.get(userId);
        if (emitter != null) {
            try {
                // 创建包含用户 ID 和消息内容的 JSON 对象
                String jsonMessage = String.format("{\"senderId\":\"%s\", \"message\":\"%s\"}", userId, message);
                emitter.send(jsonMessage);
                log.info("消息推送成功！");
            } catch (IOException e) {
                clients.remove(userId);
                log.info("消息推送失败！");
            }
        }
    }

}
