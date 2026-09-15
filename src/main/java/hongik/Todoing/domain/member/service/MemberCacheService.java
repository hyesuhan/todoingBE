package hongik.Todoing.domain.member.service;

import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.domain.member.repository.MemberRepository;
import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

// JwtAuthorizationFilter가 매 요청마다 DB를 치지 않도록 User를 캐싱.
// 탈퇴/차단/프로필 변경 시 evict()를 호출해야 즉시 반영됨 (안 부르면 accessToken 만료 시간만큼 stale 상태 유지).
@Component
@RequiredArgsConstructor
public class MemberCacheService {

    private static final String KEY_PREFIX = "user:email:";

    private final RedisTemplate<String, User> userCacheRedisTemplate;
    private final MemberRepository memberRepository;

    @Value("${spring.jwt.token.access-token-expire-time}")
    private long accessTokenExpireMillis;

    public User getByEmail(String email) {
        String key = KEY_PREFIX + email;

        User cached = userCacheRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        userCacheRedisTemplate.opsForValue().set(key, user, Duration.ofMillis(accessTokenExpireMillis));
        return user;
    }

    public void evict(String email) {
        userCacheRedisTemplate.delete(KEY_PREFIX + email);
    }
}
