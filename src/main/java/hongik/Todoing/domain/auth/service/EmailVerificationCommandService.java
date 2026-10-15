package hongik.Todoing.domain.auth.service;

import hongik.Todoing.domain.auth.adaptor.EmailSenderAdaptor;
import hongik.Todoing.domain.user.repository.UserRepository;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class EmailVerificationCommandService {

    private static final String CODE_PREFIX = "auth:email:code:";
    private static final String DAILY_PREFIX = "auth:email:daily:";
    private static final String VERIFY_ATTEMPT_PREFIX = "auth:email:verify-attempt:";
    static final String VERIFIED_PREFIX = "auth:email:verified:";

    private static final Duration CODE_TTL = Duration.ofMinutes(1);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(5);
    private static final Duration DAILY_TTL = Duration.ofHours(24);
    private static final Duration VERIFY_ATTEMPT_WINDOW = Duration.ofMinutes(1);
    private static final long DAILY_LIMIT = 5;
    private static final long VERIFY_ATTEMPT_LIMIT = 100;

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final EmailSenderAdaptor emailSenderAdaptor;
    private final SecureRandom secureRandom = new SecureRandom();

    public void sendCode(String email) {
        validateNotRegistered(email);
        validateNotTooFrequent(email);
        validateDailyLimitNotExceeded(email);

        String code = generateCode();

        // 발송이 비동기라 결과를 기다릴 수 없음 - 저장을 먼저 커밋하고 발송은 fire-and-forget.
        // ponytail: 발송이 끝내 실패해도 이 코드/일일 카운트는 그대로 남는다. 재시도 없이는
        // 사용자가 메일을 못 받고도 쿨다운/일일 한도를 소모한 상태가 됨 - email-race-condition.md 2번 참고.
        redisTemplate.opsForValue().set(CODE_PREFIX + email, code, CODE_TTL);
        incrementDailyCount(email);

        emailSenderAdaptor.sendVerificationCode(email, code);
    }

    public void verifyCode(String email, String code) {
        validateNotTooManyAttempts(email);

        String stored = redisTemplate.opsForValue().get(CODE_PREFIX + email);
        if (stored == null) {
            throw new GeneralException(ErrorStatus.EMAIL_CODE_EXPIRED);
        }
        if (!stored.equals(code)) {
            throw new GeneralException(ErrorStatus.EMAIL_CODE_MISMATCH);
        }

        redisTemplate.delete(CODE_PREFIX + email);
        redisTemplate.delete(VERIFY_ATTEMPT_PREFIX + email);
        redisTemplate.opsForValue().set(VERIFIED_PREFIX + email, "true", VERIFIED_TTL);
    }

    public void consumeVerification(String email) {
        redisTemplate.delete(VERIFIED_PREFIX + email);
    }

    private void validateNotRegistered(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new GeneralException(ErrorStatus.EMAIL_DUPLICATED);
        }
    }

    private void validateNotTooFrequent(String email) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(CODE_PREFIX + email))) {
            throw new GeneralException(ErrorStatus.EMAIL_SEND_TOO_FREQUENT);
        }
    }

    private void validateDailyLimitNotExceeded(String email) {
        String count = redisTemplate.opsForValue().get(DAILY_PREFIX + email);
        if (count != null && Long.parseLong(count) >= DAILY_LIMIT) {
            throw new GeneralException(ErrorStatus.EMAIL_SEND_DAILY_LIMIT_EXCEEDED);
        }
    }

    private void validateNotTooManyAttempts(String email) {
        String key = VERIFY_ATTEMPT_PREFIX + email;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, VERIFY_ATTEMPT_WINDOW);
        }
        if (count != null && count > VERIFY_ATTEMPT_LIMIT) {
            throw new GeneralException(ErrorStatus.EMAIL_VERIFY_TOO_MANY_ATTEMPTS);
        }
    }

    private void incrementDailyCount(String email) {
        Long count = redisTemplate.opsForValue().increment(DAILY_PREFIX + email);
        if (count != null && count == 1) {
            redisTemplate.expire(DAILY_PREFIX + email, DAILY_TTL);
        }
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
