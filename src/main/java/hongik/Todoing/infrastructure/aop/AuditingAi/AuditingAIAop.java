package hongik.Todoing.infrastructure.aop.AuditingAi;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AuditingAIAop {

    @Around("@annotation(AuditingAI)")
    public Object execute(ProceedingJoinPoint point, AuditingAI AuditingAI) throws Throwable {
        long start = System.currentTimeMillis();

        Object result;
        try {
            result = point.proceed();
        } catch (Throwable t) {
            log.error("Error in AuditingAI: {}", t.getMessage());
            throw t;
        }

        long end = System.currentTimeMillis();
        long duration = end - start;

        String method = point.getSignature().toShortString();
        String user = getCurrentUsername();
        String action = AuditingAI.value();

        log.info("AuditingAI - Method: {}, User: {}, Action: {}, Duration: {} ms", method, user, action, duration);

        return result;

    }

    private String getCurrentUsername() {
        // 현재 로그인된 사용자의 이름
        try {
            return SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getName();
        } catch (Exception e) {
            return "anonymousUser";
        }
    }
}
