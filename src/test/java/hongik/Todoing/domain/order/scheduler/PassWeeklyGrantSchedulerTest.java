package hongik.Todoing.domain.order.scheduler;

import hongik.Todoing.domain.order.adaptor.PassAdaptor;
import hongik.Todoing.domain.order.domain.pass.Pass;
import hongik.Todoing.domain.order.domain.pass.PassStatus;
import hongik.Todoing.domain.order.domain.pass.ProductCode;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassWeeklyGrantSchedulerTest {

    @Mock private UserRepository userRepository;
    @Mock private PassAdaptor passAdaptor;
    private PassWeeklyGrantScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PassWeeklyGrantScheduler(userRepository, passAdaptor);
    }

    @Test
    void grantWeeklyFreePass_whenUserHasNoExistingFreePass_shouldIssueNewOne() {
        User user = User.create("nick", "a@a.com", "pw", Role.USER);
        ReflectionTestUtils.setField(user, "id", 10L);
        when(userRepository.findAllByStatus(Status.ACTIVE)).thenReturn(List.of(user));
        when(passAdaptor.findActiveFreeWeeklyByUserId(10L)).thenReturn(Optional.empty());

        scheduler.grantWeeklyFreePass();

        verify(passAdaptor).save(org.mockito.ArgumentMatchers.argThat(pass ->
                pass.getUserId().equals(10L) && pass.getProductCode() == ProductCode.FREE_WEEKLY));
    }

    @Test
    void grantWeeklyFreePass_whenUnusedFreePassExists_shouldExpireItBeforeIssuingNew() {
        User user = User.create("nick", "a@a.com", "pw", Role.USER);
        ReflectionTestUtils.setField(user, "id", 10L);
        Pass oldPass = Pass.issue(10L, ProductCode.FREE_WEEKLY, null);
        when(userRepository.findAllByStatus(Status.ACTIVE)).thenReturn(List.of(user));
        when(passAdaptor.findActiveFreeWeeklyByUserId(10L)).thenReturn(Optional.of(oldPass));

        scheduler.grantWeeklyFreePass();

        assertThat(oldPass.getStatus()).isEqualTo(PassStatus.EXPIRED);
        verify(passAdaptor).save(oldPass);
        verify(passAdaptor).save(org.mockito.ArgumentMatchers.argThat(pass ->
                pass != oldPass && pass.getProductCode() == ProductCode.FREE_WEEKLY));
    }
}
