package hongik.Todoing.infrastructure.aop.throttle;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Throttle {
    int seconds() default 1;
}
