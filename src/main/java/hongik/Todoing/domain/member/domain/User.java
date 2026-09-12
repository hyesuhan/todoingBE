package hongik.Todoing.domain.member.domain;

import hongik.Todoing.domain.friend.domain.Friend;
import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.global.apiPayload.exception.GeneralException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Entity
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "`user`")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public static User create(String nickname, String email, String password, Role role) {
        User user = new User();
        user.nickname = nickname;
        user.email = email;
        user.password = password;
        user.role = role;
        user.status = Status.PENDING;
        return user;
    }

    public List<String> getRoleList() {
        return List.of("ROLE_" + role.name());
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void verifyEmail() {
        this.status = Status.ACTIVE;
    }

    public void withdraw() {
        this.status = Status.WITHDRAWN;
    }

    public Friend createFriendship(User target) {

        if (this.equals(target)) {
            throw new GeneralException(ErrorStatus.CANNOT_BE_FRIEND_WITH_SELF);
        }
        return Friend.of(this, target);
    }
}
