package hongik.Todoing.domain.friend.repository;

import hongik.Todoing.domain.friend.domain.Friend;
import hongik.Todoing.domain.member.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    boolean existsByUserAndFriend(User user, User friend);
    Friend findByUserAndFriend(User user, User friend);
    List<Friend> findAllByUser(User user);
}
