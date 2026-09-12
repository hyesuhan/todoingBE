package hongik.Todoing.domain.member.domain;

import hongik.Todoing.domain.friend.domain.Friend;
import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.global.apiPayload.exception.GeneralException;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "`user`")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "status", nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    public static User createUser(String nickname, String password, String email) {
        User user = new User();
        user.nickname = nickname;
        user.password = password;
        user.email = email;
        user.status = Status.PENDING;
        return user;
    }

    public void emailVerified() {
        this.status = Status.ACTIVE;
    }

    public void withdraw() {
        this.status = Status.WITHDRAWN;

    }

    public List<String> getRoleList() {
        return new ArrayList<>();
    }

    public void updateName(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public Friend createFriendship(User target) {

        if (this.equals(target)) {
            throw new GeneralException(ErrorStatus.CANNOT_BE_FRIEND_WITH_SELF);
        }
        return Friend.of(this, target);
    }
}
