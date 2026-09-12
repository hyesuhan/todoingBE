package hongik.Todoing.domain.aiChat.domain;


import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatId;

    // 길이 제한
    @Column(length = 255)
    private String message;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;


}
