package hongik.Todoing.domain.todo.facade;


import hongik.Todoing.domain.label.domain.Label;
import hongik.Todoing.domain.label.repository.LabelRepository;
import hongik.Todoing.domain.member.service.MemberService;
import hongik.Todoing.domain.todo.dto.request.TodoCreateRequestDTO;
import hongik.Todoing.domain.todo.service.SelfTodoService;
import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoUsecase {

    private final SelfTodoService  selfTodoService;
    private final MemberService memberService;
    private final LabelRepository labelRepository;



    // 멤버삭제
    // 챗, 튜듀,
    // delete use facade 만들어서.

    // 기다린게 목적 x 도메인간의 참조를 끊어내는 거임.
    // 스프링 이벤트는 어플리테이서ㅕㄴ이 다운되면,사라자미.
    // 멤버 도메인 서비스에서 userDeleteEvent 발행.
    // 각 도매인 서비스에서 리스닝.

    // 면접보다가. 성능 개선. 개선한다면 어떻게 하실거에요?

    // 큐가 계속 남아있다 . 제 3자
    // mq messege queue ex ) kafka, rabbitmq


    /**
     * 절차지향 적인 Process 를 나타낼 수 있음.
     * @param request
     */

    @Transactional
    public void createTodo(Long memberId,TodoCreateRequestDTO request){

        // 라벨을 찾고
        Label label = labelRepository.findByLabelName(request.labelType())
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 튜두를 만든다.
        selfTodoService.createTodo(
                memberId,
                request.content(),
                request.todoDate(),
                label,
                request.isAiNeeded()
        );
    }

}
