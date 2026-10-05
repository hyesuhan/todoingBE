package hongik.Todoing.infrastructure.aop.TimeTrace;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class TimeTraceAop {
    @Around("target(org.springframework.data.jpa.repository.JpaRepository)")
    public Object execute(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();

        try {
            return point.proceed();
        } finally {
            long finish = System.currentTimeMillis();
            String methodName = point.getSignature().toShortString();
            log.info("Method [{}] executed in {} ms", methodName, (finish - start));
        }
    }
}
