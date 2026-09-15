package hongik.Todoing.domain.order.domain.pass;

import hongik.Todoing.domain.order.validator.PassValidator;
import hongik.Todoing.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class Pass extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pass_id")
    private Long passId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 사용 횟수
    @Column(name = "used_count")
    private Integer usedCount;

    // 총 횟수
    @Column(name = "limit_count")
    private Integer limitCount;

    // 이용권 코드
    @Enumerated(EnumType.STRING)
    private ProductCode productCode;

    // 패스 상태 - ACTIVE / EXPIRED / REVOKED
    @Enumerated(EnumType.STRING)
    private PassStatus status;

    // 결제 아이디 추적용
    @Column(name = "order_id")
    private Long orderId;

    // Optimistic Locking 적용 - 버전 관리 중
    @Version
    private Long version;

    public static Pass issue(Long userId, ProductCode productCode, Long orderId) {
        Pass pass = new Pass();
        pass.userId = userId;
        pass.productCode = productCode;
        pass.limitCount = productCode.getLimitCount();
        pass.usedCount = 0; // 초기 사용 횟수는 0
        pass.status = PassStatus.ACTIVE; // 기본 상태는 ACTIVE
        pass.orderId = orderId; // 결제 아이디 설정
        return pass;
    }

    public Integer remainingCount() {
        return limitCount - usedCount;
    }

    // 패스를 사용합니다.
    public void consume(Long userId, PassValidator passValidator) {
        // 사용 가능한 이용권인지 확인합니다.
        passValidator.validPass(this, userId);
        this.usedCount++;
    }

    // 이미 결제가 완료되었는지 확인합니다.
    public Boolean isPaid() {
        return this.orderId != null;
    }

    // 사용자가 주문을 환불 시킵니다.
    public void revoke(Long userId, PassValidator passValidator) {
        passValidator.validRevoke(this);
        this.status = PassStatus.REVOKED;
    }

    // 패스를 다 사용할 경우 패스 상태가 변경되어야 합니다.
    public void checkPassStatus() {
        if (this.usedCount >= this.limitCount) {
            this.status = PassStatus.EXPIRED;
            return;
        }
    }
}
