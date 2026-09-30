package com.crawler.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadPoolConfig {

    private static final int THREAD_POOL_SIZE = 10;

    @Bean(
        name = "crawlExecutor",
        destroyMethod = "shutdown"
    )
    public ExecutorService crawlExecutor() {

        return Executors.newFixedThreadPool(
                THREAD_POOL_SIZE
        );
    }
}
