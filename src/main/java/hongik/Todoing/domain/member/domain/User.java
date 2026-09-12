package hongik.Todoing.domain.member.domain;

import hongik.Todoing.domain.friend.domain.Friend;
import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.global.apiPayload.exception.GeneralException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Builder.Default
    private Status status = Status.PENDING;

    private String role; // ROLE_USER, ROLE_ADMIN

    public List<String> getRoleList() {
        if (!this.role.isEmpty()) {
            return Arrays.asList(this.role.split(","));
        }
        return new ArrayList<>();
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
