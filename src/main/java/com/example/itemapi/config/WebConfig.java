package com.example.itemapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class WebConfig {

    // Define a custom async executor to control concurrency settings
    @Bean(name = "customAsyncExecutor")
    public Executor customAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);         // Minimum number of threads
        executor.setMaxPoolSize(10);         // Maximum number of threads
        executor.setQueueCapacity(100);      // Queue size before new threads are created
        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();
        return executor;
    }
}