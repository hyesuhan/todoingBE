package hongik.Todoing.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ExecutorConfig {

    @Bean(name = "llmExecutor")
    public ThreadPoolExecutor llmExecutor() {
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
    }
}