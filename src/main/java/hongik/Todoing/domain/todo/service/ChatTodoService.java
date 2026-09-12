package hongik.Todoing.domain.todo.service;


import hongik.Todoing.domain.aiChat.dto.ChatSessionState;
import hongik.Todoing.domain.label.domain.Label;
import hongik.Todoing.domain.label.domain.LabelType;
import hongik.Todoing.domain.label.repository.LabelRepository;
import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.domain.todo.domain.Todo;
import hongik.Todoing.domain.todo.dto.request.ChatTodoCreateRequestDTO;
import hongik.Todoing.domain.todo.repository.TodoRepository;
import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatTodoService {

    private final TodoRepository todoRepository;
    private final LabelRepository labelRepository;

    @Transactional
    public void createTodo(User user, ChatTodoCreateRequestDTO requestDTO, ChatSessionState sessionState){
        try {
            Label label = labelRepository.findByLabelName(
                    LabelType.valueOf(sessionState.getCategory().toUpperCase())
            ).orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

            for (ChatTodoCreateRequestDTO.SubQuest subQuest : requestDTO.getSubQuests()) {
                System.out.println("SubQuest: task=" + subQuest.getTask() + ", date=" + subQuest.getDate());
                Todo todo = Todo.builder()
                        .memberId(user.getId())
                        .content(subQuest.getTask())
                        .todoDate(LocalDate.parse(subQuest.getDate()))
                        .labelId(label.getLabelId())
                        .isAiNeeded(true)
                        .isCompleted(false)
                        .build();

                todoRepository.save(todo);
                System.out.println("✅ Todo saved: " + todo.getContent());
            }
        } catch (Exception e) {
            System.out.println(" 예외 발생: " + e.getClass().getSimpleName());
            System.out.println(" 메시지: " + e.getMessage());
            e.printStackTrace(); // 필요 시 포함
            throw e; //
        }


    }

}
