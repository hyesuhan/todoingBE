package hongik.Todoing.domain.auth.service;

import hongik.Todoing.domain.user.repository.UserRepository;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import hongik.Todoing.infrastructure.config.EmailStreamConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationCommandServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String CODE_KEY = "auth:email:code:" + EMAIL;
    private static final String COOLDOWN_KEY = "auth:email:cooldown:" + EMAIL;
    private static final String DAILY_KEY = "auth:email:daily:" + EMAIL;
    private static final String VERIFIED_KEY = "auth:email:verified:" + EMAIL;
    private static final String VERIFY_ATTEMPT_KEY = "auth:email:verify-attempt:" + EMAIL;

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private StreamOperations<String, Object, Object> streamOperations;
    @Mock
    private UserRepository userRepository;

    private EmailVerificationCommandService emailVerificationCommandService;

    @BeforeEach
    void setUp() {
        emailVerificationCommandService =
                new EmailVerificationCommandService(redisTemplate, userRepository);
    }

    @Test
    void sendCode_whenEmailAlreadyRegistered_shouldThrowEmailDuplicated() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> emailVerificationCommandService.sendCode(EMAIL))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_DUPLICATED);
    }

    @Test
    void sendCode_whenCooldownActive_shouldThrowTooFrequent() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(redisTemplate.hasKey(COOLDOWN_KEY)).thenReturn(true);

        assertThatThrownBy(() -> emailVerificationCommandService.sendCode(EMAIL))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_SEND_TOO_FREQUENT);
    }

    @Test
    void sendCode_whenDailyLimitReached_shouldThrowDailyLimitExceeded() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(redisTemplate.hasKey(COOLDOWN_KEY)).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(DAILY_KEY)).thenReturn("5");

        assertThatThrownBy(() -> emailVerificationCommandService.sendCode(EMAIL))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_SEND_DAILY_LIMIT_EXCEEDED);
    }

    @Test
    void sendCode_whenSuccessful_shouldPersistThenEnqueueToStream() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(redisTemplate.hasKey(COOLDOWN_KEY)).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(valueOperations.get(DAILY_KEY)).thenReturn(null);
        when(valueOperations.increment(DAILY_KEY)).thenReturn(1L);

        emailVerificationCommandService.sendCode(EMAIL);

        verify(valueOperations).set(eq(CODE_KEY), anyString(), eq(Duration.ofMinutes(10)));
        verify(valueOperations).set(eq(COOLDOWN_KEY), anyString(), eq(Duration.ofMinutes(1)));
        verify(redisTemplate).expire(DAILY_KEY, Duration.ofHours(24));
        verify(streamOperations).add(eq(EmailStreamConfig.STREAM_KEY), org.mockito.ArgumentMatchers.anyMap());

        // Streams 적재는 영속 처리를 위한 것이므로, 저장이 적재보다 먼저 일어나야 함
        InOrder inOrder = inOrder(valueOperations, streamOperations);
        inOrder.verify(valueOperations).set(eq(CODE_KEY), anyString(), eq(Duration.ofMinutes(10)));
        inOrder.verify(streamOperations).add(eq(EmailStreamConfig.STREAM_KEY), org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    void verifyCode_whenAttemptLimitExceeded_shouldThrowTooManyAttemptsWithoutReadingCode() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(VERIFY_ATTEMPT_KEY)).thenReturn(101L);

        assertThatThrownBy(() -> emailVerificationCommandService.verifyCode(EMAIL, "123456"))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_VERIFY_TOO_MANY_ATTEMPTS);

        verify(valueOperations, never()).get(CODE_KEY);
    }

    @Test
    void verifyCode_whenCodeMissing_shouldThrowExpired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CODE_KEY)).thenReturn(null);

        assertThatThrownBy(() -> emailVerificationCommandService.verifyCode(EMAIL, "123456"))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_CODE_EXPIRED);
    }

    @Test
    void verifyCode_whenCodeMismatch_shouldThrowMismatch() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CODE_KEY)).thenReturn("111111");

        assertThatThrownBy(() -> emailVerificationCommandService.verifyCode(EMAIL, "222222"))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_CODE_MISMATCH);
    }

    @Test
    void verifyCode_whenCodeMatches_shouldSetVerifiedAndDeleteCode() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CODE_KEY)).thenReturn("123456");

        emailVerificationCommandService.verifyCode(EMAIL, "123456");

        verify(redisTemplate).delete(CODE_KEY);
        verify(redisTemplate).delete(VERIFY_ATTEMPT_KEY);
        verify(valueOperations).set(VERIFIED_KEY, "true", Duration.ofMinutes(5));
    }
}
