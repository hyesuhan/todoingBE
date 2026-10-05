package hongik.Todoing.infrastructure.aop.AuditingAi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditingAI {
    String value() default "Todo 에 대한 인증을 시작합니다.";
}
