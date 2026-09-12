package hongik.Todoing.domain.prompt.domain;

import hongik.Todoing.domain.label.domain.Label;
import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.domain.prompt.exception.PromptException;
import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.Period;

@Entity
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptInput extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long promptInputId;

    @Enumerated(EnumType.STRING)
    private Level level;
    private LocalDate startDate;
    private LocalDate endDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "label_id")
    private Label label;

    public static PromptInput of(Level level, LocalDate startDate, LocalDate endDate, User user, Label label) {
        PromptInput promptInput = new PromptInput();
        promptInput.level = level;
        promptInput.startDate = startDate;
        promptInput.endDate = endDate;
        promptInput.user = user;
        promptInput.label = label;
        return promptInput;
    }

    public Period getPeriod() {
        if(startDate == null || endDate == null) {
            throw new PromptException(ErrorStatus.DATE_IS_NULL);
        }

        return Period.between(startDate, endDate);
    }
}
