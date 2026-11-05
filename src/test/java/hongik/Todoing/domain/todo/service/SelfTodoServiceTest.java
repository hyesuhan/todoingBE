package hongik.Todoing.domain.todo.service;

import hongik.Todoing.domain.todo.converter.TodoConverter;
import hongik.Todoing.domain.todo.domain.Todo;
import hongik.Todoing.domain.todo.dto.request.TodoCreateRequestDTO;
import hongik.Todoing.domain.todo.label.domain.Label;
import hongik.Todoing.domain.todo.label.domain.LabelType;
import hongik.Todoing.domain.todo.label.repository.LabelRepository;
import hongik.Todoing.domain.todo.repository.TodoRepository;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelfTodoServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private TodoRepository todoRepository;
    @Mock
    private LabelRepository labelRepository;
    @Mock
    private TodoConverter todoConverter;

    private SelfTodoService selfTodoService;

    @BeforeEach
    void setUp() {
        selfTodoService = new SelfTodoService(todoRepository, labelRepository, todoConverter);
    }

    @Test
    void createTodo_whenLabelFound_shouldBuildAndSaveTodo() {
        Label label = Label.of(LabelType.WORK);
        LocalDate todoDate = LocalDate.of(2026, 11, 5);
        TodoCreateRequestDTO request = new TodoCreateRequestDTO("content", todoDate, LabelType.WORK, true);
        when(labelRepository.findByLabelName(LabelType.WORK)).thenReturn(Optional.of(label));

        selfTodoService.createTodo(MEMBER_ID, request);

        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(captor.capture());
        Todo saved = captor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(saved.getContent()).isEqualTo("content");
        assertThat(saved.getTodoDate()).isEqualTo(todoDate);
        assertThat(saved.getLabelId()).isEqualTo(label.getLabelId());
        assertThat(saved.isAiNeeded()).isTrue();
    }

    @Test
    void createTodo_whenLabelNotFound_shouldThrowAndNotSave() {
        TodoCreateRequestDTO request = new TodoCreateRequestDTO("content", LocalDate.now(), LabelType.WORK, false);
        when(labelRepository.findByLabelName(LabelType.WORK)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> selfTodoService.createTodo(MEMBER_ID, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", ErrorStatus.NOT_FOUND);

        verify(todoRepository, never()).save(any());
    }
}
