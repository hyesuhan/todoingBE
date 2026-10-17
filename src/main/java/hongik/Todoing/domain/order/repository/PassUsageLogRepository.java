package hongik.Todoing.domain.order.repository;

import hongik.Todoing.domain.order.domain.pass.PassUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PassUsageLogRepository extends JpaRepository<PassUsageLog, Long> {
    List<PassUsageLog> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
