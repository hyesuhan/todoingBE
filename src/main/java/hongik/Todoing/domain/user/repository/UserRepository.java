package hongik.Todoing.domain.user.repository;

import hongik.Todoing.domain.user.domain.Status;
import hongik.Todoing.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);

    Optional<User> findByNickname(String nickname);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByEmailAndNickname(String email, String nickname);

    List<User> findAllByStatus(Status status);
}
