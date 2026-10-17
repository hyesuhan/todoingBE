package hongik.Todoing.domain.order.service;

import hongik.Todoing.domain.order.adaptor.PassAdaptor;
import hongik.Todoing.domain.order.adaptor.PassUsageLogAdaptor;
import hongik.Todoing.domain.order.domain.pass.Pass;
import hongik.Todoing.domain.order.domain.pass.PassStatus;
import hongik.Todoing.domain.order.domain.pass.PassUsageLog;
import hongik.Todoing.domain.order.domain.pass.ProductCode;
import hongik.Todoing.domain.order.dto.pass.response.PassRemainingResponse;
import hongik.Todoing.domain.order.dto.pass.response.PassUsageLogResponse;
import hongik.Todoing.domain.order.exception.passException.PassNotFoundException;
import hongik.Todoing.domain.order.validator.PassValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassServiceTest {

    @Mock private PassAdaptor passAdaptor;
    @Mock private PassUsageLogAdaptor passUsageLogAdaptor;
    private PassValidator passValidator;
    private PassService passService;

    @BeforeEach
    void setUp() {
        passValidator = new PassValidator(passAdaptor);
        passService = new PassService(passAdaptor, passValidator, passUsageLogAdaptor);
    }

    @Test
    void consumePass_whenConsumablePassExists_shouldConsumeAndLogUsage() {
        Pass freePass = Pass.issue(1L, ProductCode.FREE_WEEKLY, null);
        when(passAdaptor.findConsumableByUserId(1L)).thenReturn(Optional.of(freePass));

        passService.consumePass(1L);

        assertThat(freePass.getUsedCount()).isEqualTo(1);
        // Pass.issue()로 만든 패스는 영속화 전이라 passId가 null — PassUsageLog에도 그대로 null로 기록된다.
        verify(passUsageLogAdaptor).save(org.mockito.ArgumentMatchers.argThat(log ->
                log.getUserId().equals(1L)
                        && log.getPassId() == null
                        && log.getProductCode() == ProductCode.FREE_WEEKLY));
    }

    @Test
    void consumePass_whenNoConsumablePass_shouldThrowPassNotFound() {
        when(passAdaptor.findConsumableByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passService.consumePass(1L))
                .isSameAs(PassNotFoundException.EXCEPTION);
    }

    @Test
    void getRemainingCount_shouldExcludeExpiredPasses() {
        Pass activePass = Pass.issue(1L, ProductCode.PASS_1, 100L); // remaining 1
        Pass expiredPass = Pass.issue(1L, ProductCode.FREE_WEEKLY, null);
        ReflectionTestUtils.setField(expiredPass, "status", PassStatus.EXPIRED);
        when(passAdaptor.findByUserId(1L)).thenReturn(List.of(activePass, expiredPass));

        PassRemainingResponse result = passService.getRemainingCount(1L);

        assertThat(result.remaining()).isEqualTo(1);
    }

    @Test
    void getUsageHistory_shouldMapLogsToResponse() {
        PassUsageLog log = PassUsageLog.of(1L, 10L, ProductCode.FREE_WEEKLY);
        when(passUsageLogAdaptor.findByUserId(1L)).thenReturn(List.of(log));

        List<PassUsageLogResponse> result = passService.getUsageHistory(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).productName()).isEqualTo("주간 무료 이용권");
    }
}
