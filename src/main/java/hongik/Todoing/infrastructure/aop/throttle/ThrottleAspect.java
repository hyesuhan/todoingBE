package hongik.Todoing.infrastructure.aop.throttle;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Duration;


/*
@Throttle 어노테이션이 붙은 메서드를 실행하기 전에
Redis에 키를 잠시 저장하여 동일 유저가 특정 시간 내에 같은 메서드를 호출하려고 한다면 차단
*/

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ThrottleAspect {

    private final StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(throttle)")
    public Object handleThrottle(ProceedingJoinPoint joinPoint, Throttle throttle) throws Throwable {

        // 현재 로그인 유저 ID (비로그인 사용자면 구분자 추가 필요)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new RuntimeException("인증된 사용자만 사용할 수 있습니다.");
        }
        String userId = auth.getName();

        String key = "throttle:" + userId + ":" + joinPoint.getSignature().toShortString();
        Duration duration = Duration.ofSeconds(throttle.seconds());

        // Redis 에서 해당 유저에 대한 key가 없으면 저장
        Boolean isAllowed = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", duration);

        // Redis 에 해당 유저에 대한 key가 있으면 거부
        if (Boolean.FALSE.equals(isAllowed)) {
            log.warn("[Throttle] 요청 거부됨. key={}, duration={}s", key, duration.getSeconds());
            throw new RuntimeException("요청이 너무 자주 발생했습니다. 잠시만 기다려주세요.");
        }

        log.info("[Throttle] 요청 허용. key={}, duration={}s", key, duration.getSeconds());
        return joinPoint.proceed(); // 실제 메서드 실행
    }
}
