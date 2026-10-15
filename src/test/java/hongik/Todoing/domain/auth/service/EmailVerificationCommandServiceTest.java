package hongik.Todoing.domain.auth.service;

import hongik.Todoing.domain.auth.adaptor.EmailSenderAdaptor;
import hongik.Todoing.domain.auth.exception.EmailSendFailedException;
import hongik.Todoing.domain.user.repository.UserRepository;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationCommandServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String CODE_KEY = "auth:email:code:" + EMAIL;
    private static final String DAILY_KEY = "auth:email:daily:" + EMAIL;
    private static final String VERIFIED_KEY = "auth:email:verified:" + EMAIL;

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailSenderAdaptor emailSenderAdaptor;

    private EmailVerificationCommandService emailVerificationCommandService;

    @BeforeEach
    void setUp() {
        emailVerificationCommandService =
                new EmailVerificationCommandService(redisTemplate, userRepository, emailSenderAdaptor);
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
    void sendCode_whenCodeStillActive_shouldThrowTooFrequent() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(redisTemplate.hasKey(CODE_KEY)).thenReturn(true);

        assertThatThrownBy(() -> emailVerificationCommandService.sendCode(EMAIL))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_SEND_TOO_FREQUENT);
    }

    @Test
    void sendCode_whenDailyLimitReached_shouldThrowDailyLimitExceeded() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(redisTemplate.hasKey(CODE_KEY)).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(DAILY_KEY)).thenReturn("5");

        assertThatThrownBy(() -> emailVerificationCommandService.sendCode(EMAIL))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_SEND_DAILY_LIMIT_EXCEEDED);
    }

    @Test
    void sendCode_whenAdaptorFails_shouldPropagateAndNotPersistAnyState() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(redisTemplate.hasKey(CODE_KEY)).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(DAILY_KEY)).thenReturn(null);
        doThrow(EmailSendFailedException.EXCEPTION)
                .when(emailSenderAdaptor).sendVerificationCode(eq(EMAIL), anyString());

        assertThatThrownBy(() -> emailVerificationCommandService.sendCode(EMAIL))
                .isInstanceOf(EmailSendFailedException.class);

        verify(valueOperations, never()).set(eq(CODE_KEY), anyString(), any(Duration.class));
        verify(valueOperations, never()).increment(DAILY_KEY);
    }

    @Test
    void sendCode_whenSuccessful_shouldStoreCodeAndIncrementDailyCount() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(redisTemplate.hasKey(CODE_KEY)).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(DAILY_KEY)).thenReturn(null);
        when(valueOperations.increment(DAILY_KEY)).thenReturn(1L);

        emailVerificationCommandService.sendCode(EMAIL);

        verify(valueOperations).set(eq(CODE_KEY), anyString(), eq(Duration.ofMinutes(1)));
        verify(redisTemplate).expire(DAILY_KEY, Duration.ofHours(24));
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
        verify(valueOperations).set(VERIFIED_KEY, "true", Duration.ofMinutes(5));
    }
}
