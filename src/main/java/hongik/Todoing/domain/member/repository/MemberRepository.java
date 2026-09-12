package hongik.Todoing.domain.member.repository;

import hongik.Todoing.domain.member.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<User, Long> {
    // Custom query methods can be defined here if needed
    // For example, findByUsername(String username) or findByEmail(String email)
    Optional<User> findById(Long id);
    Optional<User> findByName(String name);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
