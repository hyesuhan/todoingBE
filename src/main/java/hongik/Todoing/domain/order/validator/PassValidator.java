package hongik.Todoing.domain.order.validator;

import hongik.Todoing.Common.annotation.Validator;
import hongik.Todoing.domain.order.domain.pass.Pass;
import hongik.Todoing.domain.order.domain.pass.PassStatus;
import hongik.Todoing.domain.order.exception.passException.PassAlreadyUsedException;
import hongik.Todoing.domain.order.exception.passException.PassNotOwnedException;
import hongik.Todoing.domain.order.exception.passException.PassNotValidException;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Validator
@RequiredArgsConstructor
public class PassValidator {

    public void validCreate(Pass pass) {

    }

    /**
     * 사용 가능한 이용권인지 확인합니다.
     **/
    public void validPass(Pass pass, Long userId) {
        validOwner(pass, userId);
        if(!Objects.equals(pass.getStatus(), PassStatus.ACTIVE ))
            throw PassNotValidException.EXCEPTION;
        if(pass.getUsedCount() >= pass.getLimitCount()) {
            throw PassAlreadyUsedException.EXCEPTION;
        }

    }

    // 환불 가능 이용권인지 확인합니다.
    public void validRevoke(Pass pass) {
        validAvailableRevokeDate(pass);
        if(pass.getStatus() != PassStatus.ACTIVE)
            throw PassNotValidException.EXCEPTION;
    }

    // 이용권에 대한 주인인지 검증합니다.
    public void validOwner(Pass pass, Long currentUserId) {
        if(!pass.getUserId().equals(currentUserId)) {
            throw PassNotOwnedException.EXCEPTION;
        }
    }

    // 사용횟수가 0인지 확인합니다.
    public void validAvailableRevokeDate(Pass pass) {
        if (pass.getUsedCount() > 0) {
            throw PassAlreadyUsedException.EXCEPTION;
        }
    }

    // 패스가 상태 변화가 필요한가?
    public void checkStatus(Pass pass) {

    }
}
