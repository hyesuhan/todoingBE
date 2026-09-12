package hongik.Todoing.domain.friend.domain;

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
@Table(name = "friend")
public class Friend extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private FriendStatus status;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "friend_id")
    private User friend;

    public void updateStatus(FriendStatus status) {
        this.status = status;
    }

    public static Friend of(User me, User target) {
        return Friend.builder()
                .user(me)
                .friend(target)
                .status(FriendStatus.ACCEPTED) // 초기 상태 (원하는 값으로)
                .build();
    }
}
