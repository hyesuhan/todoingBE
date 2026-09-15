package hongik.Todoing.domain.aiChat.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatDebounceTimerManager {
    private static final long DELAY_MS = 200;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    public void reset(String userId, Runnable task) {
        if (timers.containsKey(userId)) {
            timers.get(userId).cancel(false);
        }

        long scheduledAt = System.currentTimeMillis();
        Runnable timedTask = () -> {
            long actualDelayMs = System.currentTimeMillis() - scheduledAt;
            log.info("[디바운스] 예약 {}ms 대비 실제 발화까지 {}ms", DELAY_MS, actualDelayMs);
            task.run();
        };

        ScheduledFuture<?> future = scheduler.schedule(timedTask, DELAY_MS, TimeUnit.MILLISECONDS);
        timers.put(userId, future);
    }
}
