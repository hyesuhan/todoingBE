package hongik.Todoing.domain.todoReply.domain;

import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.domain.todo.domain.Todo;
import hongik.Todoing.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoReply extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long replyId;

    private String content;

    @ManyToOne
    @JoinColumn(name = "todo_id")
    private Todo todo;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public static TodoReply write(Todo todo, User user, String content) {
        TodoReply reply = new TodoReply();
        reply.todo = todo;
        reply.user = user;
        reply.content = content;
        return reply;
    }
}
