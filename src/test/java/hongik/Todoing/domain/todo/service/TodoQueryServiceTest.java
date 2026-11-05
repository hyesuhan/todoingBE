package hongik.Todoing.domain.todo.service;

import hongik.Todoing.domain.todo.domain.Todo;
import hongik.Todoing.domain.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoQueryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TodoRepository todoRepository;

    private TodoQueryService todoQueryService;

    @BeforeEach
    void setUp() {
        todoQueryService = new TodoQueryService(todoRepository);
    }

    @Test
    void findAllByUserId_delegatesToRepository() {
        Todo todo = Todo.create(USER_ID, "content", LocalDate.now(), 1L, false);
        List<Todo> todos = List.of(todo);
        when(todoRepository.findByMemberId(USER_ID)).thenReturn(todos);

        List<Todo> result = todoQueryService.findAllByUserId(USER_ID);

        assertThat(result).isEqualTo(todos);
    }

    @Test
    void findAllByUserId_whenEmpty_shouldReturnEmptyList() {
        when(todoRepository.findByMemberId(USER_ID)).thenReturn(List.of());

        List<Todo> result = todoQueryService.findAllByUserId(USER_ID);

        assertThat(result).isEmpty();
    }
}
