package hongik.Todoing.domain.aiChat.domain;


import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
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

    public static Chat write(User sender, String message) {
        Chat chat = new Chat();
        chat.sender = sender;
        chat.message = message;
        return chat;
    }
}
