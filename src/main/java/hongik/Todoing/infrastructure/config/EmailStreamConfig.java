package hongik.Todoing.infrastructure.config;

import hongik.Todoing.domain.auth.stream.EmailStreamListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EmailStreamConfig {

    public static final String STREAM_KEY = "auth:email:send-stream";
    public static final String CONSUMER_GROUP = "email-send-group";
    private static final String CONSUMER_NAME_PREFIX = "email-send-consumer-";
    // 컨슈머 1개로는 SMTP 지연 상황에서 순차 처리라 처리량이 심각하게 밀림(loadtest3 참고) -
    // Redis Streams의 경쟁 컨슈머 패턴으로 같은 그룹에 여러 개 등록해 병렬로 처리
    private static final int CONSUMER_COUNT = 4;

    private final StringRedisTemplate redisTemplate;
    private final EmailStreamListener emailStreamListener;

    @PostConstruct
    public void createConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), CONSUMER_GROUP);
        } catch (Exception e) {
            // 그룹이 이미 있으면 BUSYGROUP 에러 - 재가동 시엔 항상 나는 정상 케이스라 무시
            log.info("[EMAIL_VERIFY] consumer group already exists or stream not ready: {}", e.getMessage());
        }
    }

    @Bean(destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> emailStreamListenerContainer(
            RedisConnectionFactory connectionFactory) {

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory,
                        StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                                .pollTimeout(Duration.ofSeconds(1))
                                .build());

        for (int i = 1; i <= CONSUMER_COUNT; i++) {
            container.receive(
                    Consumer.from(CONSUMER_GROUP, CONSUMER_NAME_PREFIX + i),
                    StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()),
                    emailStreamListener
            );
        }

        container.start();
        return container;
    }
}
