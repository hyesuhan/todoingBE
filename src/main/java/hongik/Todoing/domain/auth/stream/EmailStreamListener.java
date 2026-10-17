package hongik.Todoing.domain.auth.stream;

import hongik.Todoing.domain.auth.adaptor.EmailSenderAdaptor;
import hongik.Todoing.infrastructure.config.EmailStreamConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    private final EmailSenderAdaptor emailSenderAdaptor;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Map<String, String> value = message.getValue();
        String email = value.get("email");
        String code = value.get("code");

        try {
            emailSenderAdaptor.sendVerificationCode(email, code);
            log.info("[EMAIL_VERIFY] event=SEND_SUCCESS email={}", email);
            redisTemplate.opsForStream()
                    .acknowledge(EmailStreamConfig.STREAM_KEY, EmailStreamConfig.CONSUMER_GROUP, message.getId());
        } catch (Exception e) {
            // ack 하지 않음 - PEL(pending entries list)에 남아 재시도/복구 대상이 됨
            log.warn("[EMAIL_VERIFY] event=SEND_FAILED email={}", email, e);
        }
    }
}
