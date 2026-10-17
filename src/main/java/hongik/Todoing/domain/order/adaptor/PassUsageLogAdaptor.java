package hongik.Todoing.domain.order.adaptor;

import hongik.Todoing.Common.annotation.Adaptor;
import hongik.Todoing.domain.order.domain.pass.PassUsageLog;
import hongik.Todoing.domain.order.repository.PassUsageLogRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Adaptor
@RequiredArgsConstructor
public class PassUsageLogAdaptor {

    private final PassUsageLogRepository passUsageLogRepository;

    public PassUsageLog save(PassUsageLog log) {
        return passUsageLogRepository.save(log);
    }

    public List<PassUsageLog> findByUserId(Long userId) {
        return passUsageLogRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }
}
