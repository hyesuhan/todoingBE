package hongik.Todoing.domain.todo.converter;

import hongik.Todoing.domain.label.domain.Label;
import hongik.Todoing.domain.label.domain.LabelType;
import hongik.Todoing.domain.label.repository.LabelRepository;
import hongik.Todoing.domain.todo.domain.Todo;
import hongik.Todoing.domain.todo.dto.response.TodoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TodoConverter {

    private final LabelRepository labelRepository;

    public List<TodoResponseDTO> toTodoDtoList(List<Todo> todos) {
        List<Long> labelIds = todos.stream()
                .map(Todo::getLabelId)
                .distinct()
                .toList();

        // 라벨을 todo 개수만큼 쪼개서 조회하지 않고 한 번의 IN 절로 배치 조회 (N+1 방지)
        Map<Long, LabelType> labelNameById = labelRepository.findAllById(labelIds).stream()
                .collect(Collectors.toMap(Label::getLabelId, Label::getLabelName));

        return todos.stream()
                .map(todo -> new TodoResponseDTO(
                        todo.getTodoId(),
                        todo.getContent(),
                        todo.getTodoDate(),
                        todo.isCompleted(),
                        todo.isAiNeeded(),
                        labelNameById.get(todo.getLabelId())
                ))
                .toList();
    }

}
