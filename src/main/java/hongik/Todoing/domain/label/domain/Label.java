package hongik.Todoing.domain.label.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class Label {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long labelId;

    @Enumerated(EnumType.STRING)
    private LabelType labelName;

    public static Label of(LabelType labelName) {
        Label label = new Label();
        label.labelName = labelName;
        return label;
    }
}
