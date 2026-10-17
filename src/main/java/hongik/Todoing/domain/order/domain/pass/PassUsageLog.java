package hongik.Todoing.domain.order.domain.pass;

import hongik.Todoing.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PassUsageLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pass_usage_log_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "pass_id", nullable = false)
    private Long passId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_code", nullable = false)
    private ProductCode productCode;

    public static PassUsageLog of(Long userId, Long passId, ProductCode productCode) {
        PassUsageLog log = new PassUsageLog();
        log.userId = userId;
        log.passId = passId;
        log.productCode = productCode;
        return log;
    }
}
