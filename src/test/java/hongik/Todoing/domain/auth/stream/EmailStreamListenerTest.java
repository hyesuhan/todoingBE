package hongik.Todoing.domain.auth.stream;

import hongik.Todoing.domain.auth.adaptor.EmailSenderAdaptor;
import hongik.Todoing.infrastructure.config.EmailStreamConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailStreamListenerTest {

    private static final String EMAIL = "user@example.com";
    private static final String CODE = "123456";

    @Mock
    private EmailSenderAdaptor emailSenderAdaptor;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    private EmailStreamListener emailStreamListener;
    private MapRecord<String, String, String> message;

    @BeforeEach
    void setUp() {
        emailStreamListener = new EmailStreamListener(emailSenderAdaptor, redisTemplate);
        message = MapRecord.create(EmailStreamConfig.STREAM_KEY, Map.of("email", EMAIL, "code", CODE))
                .withId(RecordId.of("1-1"));
    }

    @Test
    void onMessage_whenSendSucceeds_shouldAcknowledge() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);

        emailStreamListener.onMessage(message);

        verify(emailSenderAdaptor).sendVerificationCode(EMAIL, CODE);
        verify(streamOperations).acknowledge(
                eq(EmailStreamConfig.STREAM_KEY), eq(EmailStreamConfig.CONSUMER_GROUP), eq(message.getId()));
    }

    @Test
    void onMessage_whenSendFails_shouldNotAcknowledgeSoItStaysPendingForRetry() {
        doThrow(new RuntimeException("smtp down")).when(emailSenderAdaptor).sendVerificationCode(EMAIL, CODE);

        emailStreamListener.onMessage(message);

        verify(redisTemplate, never()).opsForStream();
    }
}
