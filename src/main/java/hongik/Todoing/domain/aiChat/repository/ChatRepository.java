package hongik.Todoing.domain.aiChat.repository;

import hongik.Todoing.domain.aiChat.domain.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    // JpaRepository를 상속받아 CRUD 메서드를 사용할 수 있습니다.
    // 추가적인 쿼리 메서드가 필요하다면 여기에 정의할 수 있습니다.
}
