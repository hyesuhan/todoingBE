package hongik.Todoing.domain.order.service;

import hongik.Todoing.common.annotation.DomainService;
import hongik.Todoing.domain.order.domain.order.Order;
import hongik.Todoing.domain.order.domain.pass.Pass;
import hongik.Todoing.domain.order.domain.pass.PassStatus;
import hongik.Todoing.domain.order.domain.pass.PassUsageLog;
import hongik.Todoing.domain.order.domain.pass.ProductCode;
import hongik.Todoing.domain.order.dto.order.response.KakaoReadyResponse;
import hongik.Todoing.domain.order.dto.pass.response.GetPassInfoResponse;
import hongik.Todoing.domain.order.dto.pass.response.PassRemainingResponse;
import hongik.Todoing.domain.order.dto.pass.response.PassUsageLogResponse;
import hongik.Todoing.domain.order.exception.passException.PassNotFoundException;
import hongik.Todoing.domain.order.repository.PassRepository;
import hongik.Todoing.domain.order.repository.PassUsageLogRepository;
import hongik.Todoing.domain.order.validator.PassValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@DomainService
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PassService {

    private final PassRepository passRepository;
    private final PassValidator passValidator;
    private final PassUsageLogRepository passUsageLogRepository;
    private final OrderService orderService;

    // 결제 준비
    @Transactional
    public KakaoReadyResponse requestOrderPass(Long userId) {
        return orderService.readyOrder(userId, ProductCode.PASS_1, 1);
    }

    // 결제 완료 후 패스 발급
    @Transactional
    public void completeOrderAndIssuePass(Long userId, String tid, String pgToken) {
        Order order = orderService.approveOrder(userId, tid, pgToken);
        createPass(userId, order);
    }

    // 패스 생성
    @Transactional
    public void createPass(Long userId, Order order) {
        // 패스 생성 로직을 구현합니다.
        // 예: 패스 엔티티를 생성하고 저장소에 저장

        Pass pass = Pass.issue(userId, order.getItemCode(), order.getId());

        passRepository.save(pass);

    }

    // 패스 조회
    @Transactional
    public List<GetPassInfoResponse> getPass(Long userId) {
        // 패스 조회 로깆
        List<Pass> passList = passRepository.findByUserId(userId);

        // passList를 stream을 이용해 GetPassInfoResponse 리스트로 변환
        return passList.stream()
                .map(pass -> new GetPassInfoResponse(
                        pass.getPassId(),
                        pass.getUsedCount(),
                        pass.getLimitCount(),
                        pass.getProductCode().getName()
                ))
                .toList();
    }

    // 패스 사용
    @Transactional
    public void usePass(Long userId, Long passId) {
        // 해당 패스 조회
        Pass pass = passRepository.findById(passId)
                .orElseThrow(() -> PassNotFoundException.EXCEPTION);

        // 패스 사용 로직 구현
        pass.consume(userId,  passValidator);

        // 만약 사용 후 사용횟수가 다 찼다면 상태 변경
        pass.checkPassStatus();
    }

    @Transactional
    public void consumePass(Long userId) {
        Pass pass = findConsumableByUserId(userId)
                .orElseThrow(() -> PassNotFoundException.EXCEPTION);

        pass.consume(userId, passValidator);
        pass.checkPassStatus();

        passUsageLogRepository.save(PassUsageLog.of(userId, pass.getPassId(), pass.getProductCode()));

        log.info("패스 소모: userId={}, passId={}, productCode={}", userId, pass.getPassId(), pass.getProductCode());
    }

    public List<PassUsageLogResponse> getUsageHistory(Long userId) {
        return passUsageLogRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(log -> new PassUsageLogResponse(
                        log.getId(), log.getProductCode().getName(), log.getCreatedAt()))
                .toList();
    }

    public PassRemainingResponse getRemainingCount(Long userId) {
        int remaining = passRepository.findByUserId(userId).stream()
                .filter(pass -> pass.getStatus() == PassStatus.ACTIVE)
                .mapToInt(Pass::remainingCount)
                .sum();
        return new PassRemainingResponse(remaining);
    }

    // 스케줄러 전용: 기존 주간 무료 이용권 만료 후 신규 발급
    @Transactional
    public void renewWeeklyFreePass(Long userId) {
        findActiveFreeWeeklyByUserId(userId).ifPresent(pass -> {
            pass.expire();
            passRepository.save(pass);
        });

        passRepository.save(Pass.issue(userId, ProductCode.FREE_WEEKLY, null));
    }

    // ACTIVE 상태이면서 미소진인 패스 중 FREE_WEEKLY를 우선적으로 반환
    private Optional<Pass> findConsumableByUserId(Long userId) {
        return passRepository.findByUserId(userId).stream()
                .filter(pass -> pass.getStatus() == PassStatus.ACTIVE)
                .filter(pass -> pass.getUsedCount() < pass.getLimitCount())
                .min(Comparator.comparing(pass ->
                        pass.getProductCode() == ProductCode.FREE_WEEKLY ? 0 : 1));
    }

    // ACTIVE 상태인 FREE_WEEKLY 패스 조회
    private Optional<Pass> findActiveFreeWeeklyByUserId(Long userId) {
        return passRepository.findByUserId(userId).stream()
                .filter(pass -> pass.getProductCode() == ProductCode.FREE_WEEKLY)
                .filter(pass -> pass.getStatus() == PassStatus.ACTIVE)
                .findFirst();
    }
}
