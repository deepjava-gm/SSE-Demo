package io.github.deepjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class SseDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SseDemoApplication.class);
    }


    // 注入一个全局缓存 用来保存不同用户的SSE连接信息
    @Bean("userSSEMap")
    public ConcurrentHashMap<String, SseEmitter> getUserSSEMap(){
        return new ConcurrentHashMap<>();
    }

}