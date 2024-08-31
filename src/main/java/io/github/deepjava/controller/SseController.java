package io.github.deepjava.controller;

import io.github.deepjava.service.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/sse")
@CrossOrigin(origins = "*")
@Slf4j
public class SseController {

    @Resource(name = "userSSEMap")
    private ConcurrentHashMap<String, SseEmitter> userSSEMap;

    @Resource
    private SseService sseService;

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

    // 推送方法：根据用户 ID 发送消息
    @GetMapping("/push/{userId}")
    public void push(@PathVariable String userId, @RequestParam String message) {
        sseService.extracted(userId, message);
    }

}
