package hongik.Todoing.domain.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationQueryServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String VERIFIED_KEY = "auth:email:verified:" + EMAIL;

    @Mock
    private StringRedisTemplate redisTemplate;

    private EmailVerificationQueryService emailVerificationQueryService;

    @BeforeEach
    void setUp() {
        emailVerificationQueryService = new EmailVerificationQueryService(redisTemplate);
    }

    @Test
    void isVerified_whenVerifiedKeyExists_shouldReturnTrue() {
        when(redisTemplate.hasKey(VERIFIED_KEY)).thenReturn(true);

        boolean result = emailVerificationQueryService.isVerified(EMAIL);

        assertThat(result).isTrue();
    }

    @Test
    void isVerified_whenVerifiedKeyMissing_shouldReturnFalse() {
        when(redisTemplate.hasKey(VERIFIED_KEY)).thenReturn(false);

        boolean result = emailVerificationQueryService.isVerified(EMAIL);

        assertThat(result).isFalse();
    }
}
