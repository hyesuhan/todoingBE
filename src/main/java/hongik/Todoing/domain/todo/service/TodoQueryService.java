package hongik.Todoing.domain.todo.service;

import hongik.Todoing.domain.todo.domain.Todo;
import hongik.Todoing.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 다른 도메인(friend 등)이 TodoRepository를 직접 참조하지 않고
// Todo를 조회할 수 있도록 제공하는 cross-domain 조회 전용 서비스.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoQueryService {

    private final TodoRepository todoRepository;

    public List<Todo> findAllByUserId(Long userId) {
        return todoRepository.findByMemberId(userId);
    }
}
