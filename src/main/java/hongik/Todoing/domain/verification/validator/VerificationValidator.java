package hongik.Todoing.domain.verification.validator;

import hongik.Todoing.Common.annotation.Validator;
import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.domain.order.adaptor.OrderAdaptor;
import hongik.Todoing.domain.order.adaptor.PassAdaptor;
import hongik.Todoing.domain.order.domain.pass.Pass;
import hongik.Todoing.domain.order.domain.pass.PassStatus;
import hongik.Todoing.domain.order.validator.PassValidator;
import hongik.Todoing.domain.todo.domain.Todo;
import hongik.Todoing.domain.verification.Adaptor.VerificationAdaptor;
import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Validator
@RequiredArgsConstructor
public class VerificationValidator {

    private final VerificationAdaptor verificationAdaptor;
    private final OrderAdaptor orderAdaptor;
    private final PassValidator passValidator;
    private final PassAdaptor passAdaptor;

    //인증 가능한 투두인지 검증
    public void validVerification(User user, Todo todo) {

        //투두의 유저와 인증하는 유저가 같은지 검증
        if(!user.getId().equals(todo.getMemberId())){
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }

        // 투두가 아직 인증 전인지 확인
        if(todo.isCompleted()) {
            throw new GeneralException(ErrorStatus.TODO_ALREADY_COMPLETED);
        }

        // 투두가 이미 시간이 지났는지 확인
        if(todo.getTodoDate() .isBefore(java.time.LocalDate.now())) {
            throw new GeneralException(ErrorStatus.TODO_DEADLINE_INVALID);
        }

        // 사용자의 이용권 검증
        // 유효한 Pass: 만료되지 않았고(현재는 필요 없음), 남은 인증 횟수가 1 이상
        List<Pass> passes = passAdaptor.findByUserId(user.getId());
        if (passes == null || passes.isEmpty()) {
            throw new GeneralException(ErrorStatus.PASS_NOT_AVAILABLE);
        }

        boolean hasUsablePass = passes.stream()
                .anyMatch(pass ->
                        pass.getStatus() == PassStatus.ACTIVE &&
                                pass.remainingCount() > 0
                );

        if (!hasUsablePass) {
            throw new GeneralException(ErrorStatus.PASS_NOT_AVAILABLE);
        }
    }


    // 입력받은 데이터가 올바른지 검증

}
