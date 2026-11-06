package hongik.Todoing.domain.order.service;

import hongik.Todoing.domain.order.domain.order.Order;
import hongik.Todoing.domain.order.domain.pass.Pass;
import hongik.Todoing.domain.order.domain.pass.PassStatus;
import hongik.Todoing.domain.order.domain.pass.PassUsageLog;
import hongik.Todoing.domain.order.domain.pass.ProductCode;
import hongik.Todoing.domain.order.dto.order.response.KakaoReadyResponse;
import hongik.Todoing.domain.order.dto.pass.response.PassRemainingResponse;
import hongik.Todoing.domain.order.dto.pass.response.PassUsageLogResponse;
import hongik.Todoing.domain.order.exception.passException.PassNotFoundException;
import hongik.Todoing.domain.order.repository.PassRepository;
import hongik.Todoing.domain.order.repository.PassUsageLogRepository;
import hongik.Todoing.domain.order.validator.OrderValidator;
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

    @Mock private PassRepository passRepository;
    @Mock private PassUsageLogRepository passUsageLogRepository;
    @Mock private OrderService orderService;
    @Mock private OrderValidator orderValidator; // 테스트용 Order 객체를 만들기 위한 보조 목(PassService는 사용하지 않음)
    private PassValidator passValidator;
    private PassService passService;

    @BeforeEach
    void setUp() {
        passValidator = new PassValidator();
        passService = new PassService(passRepository, passValidator, passUsageLogRepository, orderService);
    }

    // PassUsecaseTest에서 이동: 결제 준비는 항상 PASS_1/수량 1로 orderService에 위임
    @Test
    void requestOrderPass_shouldAlwaysUsePass1AndQuantityOne() {
        KakaoReadyResponse response = KakaoReadyResponse.builder().tid("tid-1").build();
        when(orderService.readyOrder(1L, ProductCode.PASS_1, 1)).thenReturn(response);

        KakaoReadyResponse result = passService.requestOrderPass(1L);

        verify(orderService).readyOrder(1L, ProductCode.PASS_1, 1);
        assertThat(result).isSameAs(response);
    }

    // PassUsecaseTest에서 이동: 주문 승인 후 패스 발급까지 이어지는지 검증
    @Test
    void completeOrderAndIssuePass_shouldApproveOrderThenCreatePass() {
        Order order = Order.createReady(1L, ProductCode.PASS_1, 1, "P1261017ABCDE", orderValidator);
        when(orderService.approveOrder(1L, "tid-1", "pg-1")).thenReturn(order);

        passService.completeOrderAndIssuePass(1L, "tid-1", "pg-1");

        verify(orderService).approveOrder(1L, "tid-1", "pg-1");
        verify(passRepository).save(org.mockito.ArgumentMatchers.argThat(pass ->
                pass.getUserId().equals(1L) && pass.getProductCode() == order.getItemCode()));
    }

    @Test
    void consumePass_whenConsumablePassExists_shouldConsumeAndLogUsage() {
        Pass freePass = Pass.issue(1L, ProductCode.FREE_WEEKLY, null);
        when(passRepository.findByUserId(1L)).thenReturn(List.of(freePass));

        passService.consumePass(1L);

        assertThat(freePass.getUsedCount()).isEqualTo(1);
        // Pass.issue()로 만든 패스는 영속화 전이라 passId가 null — PassUsageLog에도 그대로 null로 기록된다.
        verify(passUsageLogRepository).save(org.mockito.ArgumentMatchers.argThat(log ->
                log.getUserId().equals(1L)
                        && log.getPassId() == null
                        && log.getProductCode() == ProductCode.FREE_WEEKLY));
    }

    @Test
    void consumePass_whenNoConsumablePass_shouldThrowPassNotFound() {
        when(passRepository.findByUserId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> passService.consumePass(1L))
                .isSameAs(PassNotFoundException.EXCEPTION);
    }

    // PassAdaptorTest에서 이동: findConsumableByUserId의 FREE_WEEKLY 우선순위 동작 검증
    @Test
    void consumePass_whenFreeAndPaidBothActive_shouldConsumeFreeFirst() {
        Pass freePass = Pass.issue(1L, ProductCode.FREE_WEEKLY, null);
        Pass paidPass = Pass.issue(1L, ProductCode.PASS_1, 100L);
        when(passRepository.findByUserId(1L)).thenReturn(List.of(paidPass, freePass));

        passService.consumePass(1L);

        assertThat(freePass.getUsedCount()).isEqualTo(1);
        assertThat(paidPass.getUsedCount()).isEqualTo(0);
        verify(passUsageLogRepository).save(org.mockito.ArgumentMatchers.argThat(log ->
                log.getProductCode() == ProductCode.FREE_WEEKLY));
    }

    @Test
    void consumePass_whenFreeFullyUsed_shouldConsumePaid() {
        Pass freePass = Pass.issue(1L, ProductCode.FREE_WEEKLY, null);
        ReflectionTestUtils.setField(freePass, "usedCount", 1);
        Pass paidPass = Pass.issue(1L, ProductCode.PASS_1, 100L);
        when(passRepository.findByUserId(1L)).thenReturn(List.of(freePass, paidPass));

        passService.consumePass(1L);

        assertThat(paidPass.getUsedCount()).isEqualTo(1);
        verify(passUsageLogRepository).save(org.mockito.ArgumentMatchers.argThat(log ->
                log.getProductCode() == ProductCode.PASS_1));
    }

    @Test
    void consumePass_whenNoActivePass_shouldThrowPassNotFound() {
        Pass expiredFree = Pass.issue(1L, ProductCode.FREE_WEEKLY, null);
        ReflectionTestUtils.setField(expiredFree, "status", PassStatus.EXPIRED);
        when(passRepository.findByUserId(1L)).thenReturn(List.of(expiredFree));

        assertThatThrownBy(() -> passService.consumePass(1L))
                .isSameAs(PassNotFoundException.EXCEPTION);
    }

    @Test
    void getRemainingCount_shouldExcludeExpiredPasses() {
        Pass activePass = Pass.issue(1L, ProductCode.PASS_1, 100L); // remaining 1
        Pass expiredPass = Pass.issue(1L, ProductCode.FREE_WEEKLY, null);
        ReflectionTestUtils.setField(expiredPass, "status", PassStatus.EXPIRED);
        when(passRepository.findByUserId(1L)).thenReturn(List.of(activePass, expiredPass));

        PassRemainingResponse result = passService.getRemainingCount(1L);

        assertThat(result.remaining()).isEqualTo(1);
    }

    @Test
    void getUsageHistory_shouldMapLogsToResponse() {
        PassUsageLog log = PassUsageLog.of(1L, 10L, ProductCode.FREE_WEEKLY);
        when(passUsageLogRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(log));

        List<PassUsageLogResponse> result = passService.getUsageHistory(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).productName()).isEqualTo("주간 무료 이용권");
    }

    // PassAdaptorTest에서 이동: renewWeeklyFreePass의 FREE_WEEKLY+ACTIVE 필터링 동작 검증
    @Test
    void renewWeeklyFreePass_whenUnusedFreePassExists_shouldExpireItBeforeIssuingNew() {
        Pass oldPass = Pass.issue(10L, ProductCode.FREE_WEEKLY, null);
        Pass paidPass = Pass.issue(10L, ProductCode.PASS_1, 100L);
        when(passRepository.findByUserId(10L)).thenReturn(List.of(paidPass, oldPass));

        passService.renewWeeklyFreePass(10L);

        assertThat(oldPass.getStatus()).isEqualTo(PassStatus.EXPIRED);
        verify(passRepository).save(oldPass);
        verify(passRepository).save(org.mockito.ArgumentMatchers.argThat(pass ->
                pass != oldPass && pass.getProductCode() == ProductCode.FREE_WEEKLY));
    }

    @Test
    void renewWeeklyFreePass_whenNoExistingFreePass_shouldIssueNewOne() {
        Pass paidPass = Pass.issue(10L, ProductCode.PASS_1, 100L);
        when(passRepository.findByUserId(10L)).thenReturn(List.of(paidPass));

        passService.renewWeeklyFreePass(10L);

        verify(passRepository).save(org.mockito.ArgumentMatchers.argThat(pass ->
                pass.getUserId().equals(10L) && pass.getProductCode() == ProductCode.FREE_WEEKLY));
    }
}
