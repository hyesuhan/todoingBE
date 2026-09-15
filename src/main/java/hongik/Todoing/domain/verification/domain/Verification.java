package hongik.Todoing.domain.verification.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class Verification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long verificationId;

    @Enumerated(EnumType.STRING)
    private VerificationType type;

    private Boolean success;

    private double confidence;

    @Column(name =  "todo_id")
    private Long todoId;

    public static Verification record(VerificationType type, Long todoId, boolean success, double confidence) {
        Verification verification = new Verification();
        verification.type = type;
        verification.todoId = todoId;
        verification.success = success;
        verification.confidence = confidence;
        return verification;
    }
}
