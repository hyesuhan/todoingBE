package hongik.Todoing.domain.order.scheduler;

import hongik.Todoing.domain.order.adaptor.PassAdaptor;
import hongik.Todoing.domain.order.domain.pass.Pass;
import hongik.Todoing.domain.order.domain.pass.ProductCode;
import hongik.Todoing.domain.user.domain.Status;
import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PassWeeklyGrantScheduler {

    private final UserRepository userRepository;
    private final PassAdaptor passAdaptor;

    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void grantWeeklyFreePass() {
        List<User> activeUsers = userRepository.findAllByStatus(Status.ACTIVE);
        for (User user : activeUsers) {
            grantFor(user.getId());
        }
        log.info("주간 무료 사용권 지급 완료: 대상 유저 {}명", activeUsers.size());
    }

    private void grantFor(Long userId) {
        passAdaptor.findActiveFreeWeeklyByUserId(userId).ifPresent(pass -> {
            pass.expire();
            passAdaptor.save(pass);
        });

        passAdaptor.save(Pass.issue(userId, ProductCode.FREE_WEEKLY, null));
    }
}
