package hongik.Todoing.domain.friend.domain;

import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
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
        Friend friend = new Friend();
        friend.user = me;
        friend.friend = target;
        friend.status = FriendStatus.ACCEPTED; // 초기 상태 (원하는 값으로)
        return friend;
    }
}
