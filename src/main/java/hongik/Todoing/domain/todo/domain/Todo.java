package hongik.Todoing.domain.todo.domain;

import hongik.Todoing.domain.todoReply.domain.TodoReply;
import hongik.Todoing.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "todo")
public class Todo extends BaseEntity {
    @Id
    @Column(name = "todo_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long todoId;

    @Column(length = 50)
    @NotNull
    private String content;

    @NotNull
    private LocalDate todoDate;

    private boolean isAiNeeded;
    private boolean isCompleted;

    @Column(name = "user_id")
    private Long memberId;

    @Column(name = "label_id", nullable = false)
    private Long labelId;

    @Column(name = "verification_id", nullable = true)
    private Long verification_id;

    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TodoReply> replies;

    public void updateComplete(boolean isCompleted) {
        this.isCompleted = isCompleted;
    }
    public void updateTodo(String content, LocalDate todoDate, Long labelId) {
        this.content = content;
        this.todoDate = todoDate;
        this.labelId = labelId;
    }


    ///2.   @트랜잭션 의 길이가 길어진다.


}
