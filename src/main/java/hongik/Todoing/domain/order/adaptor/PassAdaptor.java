package hongik.Todoing.domain.order.adaptor;

import hongik.Todoing.Common.annotation.Adaptor;
import hongik.Todoing.domain.order.domain.pass.Pass;
import hongik.Todoing.domain.order.domain.pass.PassStatus;
import hongik.Todoing.domain.order.domain.pass.ProductCode;
import hongik.Todoing.domain.order.exception.passException.PassNotFoundException;
import hongik.Todoing.domain.order.repository.PassRepository;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Adaptor
@RequiredArgsConstructor
public class PassAdaptor {

    private final PassRepository passRepository;

    public Pass save(Pass pass) {
        return passRepository.save(pass);
    }

    public Pass findById(Long passId) {
        return passRepository
                .findById(passId)
                .orElseThrow(() -> PassNotFoundException.EXCEPTION);
    }

    public List<Pass> findByUserId(Long userId) {
        return passRepository.findByUserId(userId);
    }

    public Pass findByOrderId(Long orderId) {
        return passRepository.findByOrderId(orderId)
                .orElseThrow(() -> PassNotFoundException.EXCEPTION);
    }

    public Optional<Pass> findConsumableByUserId(Long userId) {
        return passRepository.findByUserId(userId).stream()
                .filter(pass -> pass.getStatus() == PassStatus.ACTIVE)
                .filter(pass -> pass.getUsedCount() < pass.getLimitCount())
                .min(Comparator.comparing(pass ->
                        pass.getProductCode() == ProductCode.FREE_WEEKLY ? 0 : 1));
    }

    public Optional<Pass> findActiveFreeWeeklyByUserId(Long userId) {
        return passRepository.findByUserId(userId).stream()
                .filter(pass -> pass.getProductCode() == ProductCode.FREE_WEEKLY)
                .filter(pass -> pass.getStatus() == PassStatus.ACTIVE)
                .findFirst();
    }

}
