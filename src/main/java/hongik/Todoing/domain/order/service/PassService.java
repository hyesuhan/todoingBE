package hongik.Todoing.domain.order.service;

import hongik.Todoing.Common.annotation.DomainService;
import hongik.Todoing.domain.order.adaptor.PassAdaptor;
import hongik.Todoing.domain.order.adaptor.PassUsageLogAdaptor;
import hongik.Todoing.domain.order.domain.order.Order;
import hongik.Todoing.domain.order.domain.pass.Pass;
import hongik.Todoing.domain.order.domain.pass.PassStatus;
import hongik.Todoing.domain.order.domain.pass.PassUsageLog;
import hongik.Todoing.domain.order.dto.pass.response.GetPassInfoResponse;
import hongik.Todoing.domain.order.dto.pass.response.PassRemainingResponse;
import hongik.Todoing.domain.order.dto.pass.response.PassUsageLogResponse;
import hongik.Todoing.domain.order.exception.passException.PassNotFoundException;
import hongik.Todoing.domain.order.validator.PassValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@DomainService
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PassService {

    private final PassAdaptor passAdaptor;
    private final PassValidator passValidator;
    private final PassUsageLogAdaptor passUsageLogAdaptor;

    // 패스 생성
    @Transactional
    public void createPass(Long userId, Order order) {
        // 패스 생성 로직을 구현합니다.
        // 예: 패스 엔티티를 생성하고 저장소에 저장

        Pass pass = Pass.issue(userId, order.getItemCode(), order.getId());

        passAdaptor.save(pass);

    }

    // 패스 조회
    @Transactional
    public List<GetPassInfoResponse> getPass(Long userId) {
        // 패스 조회 로깆
        List<Pass> passList = passAdaptor.findByUserId(userId);

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
        Pass pass = passAdaptor.findById(passId);

        // 패스 사용 로직 구현
        pass.consume(userId,  passValidator);

        // 만약 사용 후 사용횟수가 다 찼다면 상태 변경
        pass.checkPassStatus();
    }

    @Transactional
    public void consumePass(Long userId) {
        Pass pass = passAdaptor.findConsumableByUserId(userId)
                .orElseThrow(() -> PassNotFoundException.EXCEPTION);

        pass.consume(userId, passValidator);
        pass.checkPassStatus();

        passUsageLogAdaptor.save(PassUsageLog.of(userId, pass.getPassId(), pass.getProductCode()));

        log.info("패스 소모: userId={}, passId={}, productCode={}", userId, pass.getPassId(), pass.getProductCode());
    }

    public List<PassUsageLogResponse> getUsageHistory(Long userId) {
        return passUsageLogAdaptor.findByUserId(userId).stream()
                .map(log -> new PassUsageLogResponse(
                        log.getId(), log.getProductCode().getName(), log.getCreatedAt()))
                .toList();
    }

    public PassRemainingResponse getRemainingCount(Long userId) {
        int remaining = passAdaptor.findByUserId(userId).stream()
                .filter(pass -> pass.getStatus() == PassStatus.ACTIVE)
                .mapToInt(Pass::remainingCount)
                .sum();
        return new PassRemainingResponse(remaining);
    }
}
