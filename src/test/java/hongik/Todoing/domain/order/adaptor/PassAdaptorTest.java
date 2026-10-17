package hongik.Todoing.domain.order.adaptor;

import hongik.Todoing.domain.order.domain.pass.Pass;
import hongik.Todoing.domain.order.domain.pass.PassStatus;
import hongik.Todoing.domain.order.domain.pass.ProductCode;
import hongik.Todoing.domain.order.repository.PassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassAdaptorTest {

    @Mock private PassRepository passRepository;
    private PassAdaptor passAdaptor;

    @BeforeEach
    void setUp() {
        passAdaptor = new PassAdaptor(passRepository);
    }

    @Test
    void findConsumableByUserId_whenFreeAndPaidBothActive_shouldReturnFreeFirst() {
        Pass freePass = Pass.issue(1L, ProductCode.FREE_WEEKLY, null);
        Pass paidPass = Pass.issue(1L, ProductCode.PASS_1, 100L);
        when(passRepository.findByUserId(1L)).thenReturn(List.of(paidPass, freePass));

        Optional<Pass> result = passAdaptor.findConsumableByUserId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getProductCode()).isEqualTo(ProductCode.FREE_WEEKLY);
    }

    @Test
    void findConsumableByUserId_whenFreeFullyUsed_shouldReturnPaid() {
        Pass freePass = Pass.issue(1L, ProductCode.FREE_WEEKLY, null);
        ReflectionTestUtils.setField(freePass, "usedCount", 1);
        Pass paidPass = Pass.issue(1L, ProductCode.PASS_1, 100L);
        when(passRepository.findByUserId(1L)).thenReturn(List.of(freePass, paidPass));

        Optional<Pass> result = passAdaptor.findConsumableByUserId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getProductCode()).isEqualTo(ProductCode.PASS_1);
    }

    @Test
    void findConsumableByUserId_whenNoActivePass_shouldReturnEmpty() {
        Pass expiredFree = Pass.issue(1L, ProductCode.FREE_WEEKLY, null);
        ReflectionTestUtils.setField(expiredFree, "status", PassStatus.EXPIRED);
        when(passRepository.findByUserId(1L)).thenReturn(List.of(expiredFree));

        Optional<Pass> result = passAdaptor.findConsumableByUserId(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveFreeWeeklyByUserId_whenActiveFreePassExists_shouldReturnIt() {
        Pass freePass = Pass.issue(1L, ProductCode.FREE_WEEKLY, null);
        Pass paidPass = Pass.issue(1L, ProductCode.PASS_1, 100L);
        when(passRepository.findByUserId(1L)).thenReturn(List.of(paidPass, freePass));

        Optional<Pass> result = passAdaptor.findActiveFreeWeeklyByUserId(1L);

        assertThat(result).containsSame(freePass);
    }

    @Test
    void findActiveFreeWeeklyByUserId_whenNoFreePass_shouldReturnEmpty() {
        Pass paidPass = Pass.issue(1L, ProductCode.PASS_1, 100L);
        when(passRepository.findByUserId(1L)).thenReturn(List.of(paidPass));

        Optional<Pass> result = passAdaptor.findActiveFreeWeeklyByUserId(1L);

        assertThat(result).isEmpty();
    }
}
