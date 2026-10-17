package hongik.Todoing.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationQueryService {

    private final StringRedisTemplate redisTemplate;

    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(EmailVerificationCommandService.VERIFIED_PREFIX + email));
    }
}
