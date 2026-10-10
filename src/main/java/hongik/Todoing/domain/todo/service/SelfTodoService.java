package hongik.Todoing.domain.todo.service;

import hongik.Todoing.domain.todo.label.domain.Label;
import hongik.Todoing.domain.todo.label.repository.LabelRepository;
import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.domain.user.repository.UserRepository;
import hongik.Todoing.domain.todo.converter.TodoConverter;
import hongik.Todoing.domain.todo.domain.Todo;
import hongik.Todoing.domain.todo.dto.request.TodoUpdateRequestDTO;
import hongik.Todoing.domain.todo.dto.response.TodoResponseDTO;
import hongik.Todoing.domain.todo.repository.TodoRepository;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SelfTodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final LabelRepository labelRepository;
    private final TodoConverter todoConverter;

    @Transactional
    public void createTodo(Long memberId, String content, LocalDate todoDate, Label label, boolean isAiNeeded) {
        Todo todo = Todo.create(memberId, content, todoDate, label.getLabelId(), isAiNeeded);

        todoRepository.save(todo);
    }

    public List<TodoResponseDTO> getTodosByDate(Long memberId, LocalDate date) {
        User user = userRepository.findById(memberId).orElseThrow();
        List<Todo> todos = todoRepository.findByMemberIdAndTodoDate(user.getId(), date);
        return todoConverter.toTodoDtoList(todos);
    }

    @Transactional
    public void deleteTodo(Long memberId, Long todoId) {
        Todo todo = todoRepository.findByTodoId(todoId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TODO_NOT_FOUND));

        User user = userRepository.findById(memberId).orElseThrow();
        User user1 = userRepository.findById(todo.getMemberId()).orElseThrow();

        if(!user1.getEmail().equals(user.getEmail())) {
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        todoRepository.delete(todo);
    }

    @Transactional
    public void toggleTodo(Long memberId, Long todoId) {
        Todo todo = todoRepository.findByTodoId(todoId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TODO_NOT_FOUND));

        User user1 = userRepository.findById(todo.getMemberId()).orElseThrow();
        User user = userRepository.findById(memberId).orElseThrow();

        if(!user1.getEmail().equals(user.getEmail())) {
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }


        todo.updateComplete(!todo.isCompleted());
    }

    @Transactional
    public void updateTodo(Long memberId, Long todoId, TodoUpdateRequestDTO requestDTO) {
        Todo todo = todoRepository.findByTodoId(todoId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TODO_NOT_FOUND));

        User user1 = userRepository.findById(todo.getMemberId()).orElseThrow();
        User user = userRepository.findById(memberId).orElseThrow();

        if(!user1.getEmail().equals(user.getEmail())) {
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        Label label = labelRepository.findByLabelName(requestDTO.labelType())
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        todo.updateTodo(requestDTO.content(), requestDTO.date(), label.getLabelId());
    }

}