package hongik.Todoing.domain.aiChat.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatDebounceTimerManager {
    private static final long DELAY_MS = 200;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    public void reset(String userId, Runnable task) {
        long scheduledAt = System.currentTimeMillis();
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();

        Runnable timedTask = () -> {
            long actualDelayMs = System.currentTimeMillis() - scheduledAt;
            log.info("[디바운스] 예약 {}ms 대비 실제 발화까지 {}ms", DELAY_MS, actualDelayMs);
            try {
                task.run();
            } finally {
                // 그 사이 reset()이 다시 불려 교체된 타이머라면 지우지 않는다
                timers.remove(userId, futureRef.get());
            }
        };

        // cancel-and-replace를 같은 키에 대해 원자적으로 수행해 동시 reset()끼리의 레이스를 막는다
        timers.compute(userId, (key, existing) -> {
            if (existing != null) {
                existing.cancel(false);
            }
            ScheduledFuture<?> future = scheduler.schedule(timedTask, DELAY_MS, TimeUnit.MILLISECONDS);
            futureRef.set(future);
            return future;
        });
    }
}
