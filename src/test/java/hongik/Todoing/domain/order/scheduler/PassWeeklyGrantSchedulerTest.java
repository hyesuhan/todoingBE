package hongik.Todoing.domain.order.scheduler;

import hongik.Todoing.domain.order.service.PassService;
import hongik.Todoing.domain.user.domain.Role;
import hongik.Todoing.domain.user.domain.Status;
import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassWeeklyGrantSchedulerTest {

    @Mock private UserRepository userRepository;
    @Mock private PassService passService;
    private PassWeeklyGrantScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PassWeeklyGrantScheduler(userRepository, passService);
    }

    @Test
    void grantWeeklyFreePass_shouldRenewFreePassForEveryActiveUser() {
        User user1 = User.create("nick1", "a@a.com", "pw", Role.USER);
        ReflectionTestUtils.setField(user1, "id", 10L);
        User user2 = User.create("nick2", "b@b.com", "pw", Role.USER);
        ReflectionTestUtils.setField(user2, "id", 20L);
        when(userRepository.findAllByStatus(Status.ACTIVE)).thenReturn(List.of(user1, user2));

        scheduler.grantWeeklyFreePass();

        verify(passService).renewWeeklyFreePass(10L);
        verify(passService).renewWeeklyFreePass(20L);
    }
}
