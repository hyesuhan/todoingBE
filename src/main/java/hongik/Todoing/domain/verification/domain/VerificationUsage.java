package hongik.Todoing.domain.verification.domain;

import hongik.Todoing.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationUsage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long usageId;

    private Integer usageCount;

    @Column(name =  "user_id")
    private Long userId;

    public static VerificationUsage init(Long userId) {
        VerificationUsage usage = new VerificationUsage();
        usage.userId = userId;
        usage.usageCount = 0;
        return usage;
    }

    public void increase() {
        if (usageCount == null) {
            usageCount = 0;
        }
        usageCount++;
    }
}
