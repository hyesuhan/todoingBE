package hongik.Todoing.infrastructure.apiPayload.code.status;

import hongik.Todoing.infrastructure.apiPayload.code.BaseErrorCode;
import hongik.Todoing.infrastructure.apiPayload.code.errorDto.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {
    // AUTH
    EMAIL_NICKNAME_DUPLICATED(HttpStatus.BAD_REQUEST, "AUTH_001", "사용불가한 이메일이나 닉네임입니다."),
    USER_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "AUTH_002", "이미 탈퇴한 회원입니다."),


    // 400 BAD_REQUEST
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "400", "잘못된 요청입니다."),
    DATE_IS_NULL(HttpStatus.BAD_REQUEST, "400", "시작 또는 종료 날짜가 null입니다."),
    DATE_IS_NOT_VALID(HttpStatus.BAD_REQUEST, "400", "시작 또는 종료 날짜가 유효하지 않습니다."),
    COMMUNITY_NAME_NULL(HttpStatus.BAD_REQUEST, "400", "커뮤니티 이름이나 설명이 null입니다."),
    COMMUNITY_NAME_IS_DUPLICATED(HttpStatus.BAD_REQUEST, "400", "커뮤니티 이름이 중복되었습니다."),
    EMAIL_DUPLICATED(HttpStatus.BAD_REQUEST, "400", "이메일이 중복되었습니다."),
    FRIEND_REQUEST_IS_NULL(HttpStatus.BAD_REQUEST, "400", "친구 요청이 null입니다."),
    FRIEND_REQUEST_DUPLICATED(HttpStatus.BAD_REQUEST, "400", "친구 요청이 중복되었습니다."),
    TODO_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "400", "이미 완료된 할 일입니다."),
    TODO_DEADLINE_INVALID(HttpStatus.BAD_REQUEST, "400", "할 일의 마감 기한이 유효하지 않습니다."),
    PASS_NOT_FOUND(HttpStatus.BAD_REQUEST, "400", "사용자에게 패스가 없습니다."),
    PASS_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "400", "사용 가능한 패스가 없습니다."),
    // 401 UNAUTHORIZED
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401", "인증되지 않은 사용자입니다."),
    COMMUNITY_ADMIN_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401", "커뮤니티 관리자 권한이 없습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "401", "토큰이 만료되었습니다."),
    TOKEN_IS_NOT_VALID(HttpStatus.UNAUTHORIZED, "401", "토큰이 유효하지 않습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "401", "비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "401", "유효하지 않은 토큰입니다."),
    // 403 FORBIDDEN
    FORBIDDEN(HttpStatus.FORBIDDEN, "403", "접근이 금지된 사용자입니다."),
    COMMUNITY_ADMIN_FORBIDDEN(HttpStatus.FORBIDDEN, "403", "커뮤니티 관리자 권한이 없습니다."),
    FRIEND_BLOCKED(HttpStatus.FORBIDDEN, "403", "친구 요청이 거부되었습니다."),
    // 404 NOT_FOUND
    NOT_FOUND(HttpStatus.NOT_FOUND, "404", "요청한 리소스를 찾을 수 없습니다."),
    COMMUNITY_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "커뮤니티를 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "멤버를 찾을 수 없습니다."),
    COMMUNITY_ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "커뮤니티 관리자 정보를 찾을 수 없습니다."),
    MEMBER_NOT_FOUND_BY_EMAIL(HttpStatus.NOT_FOUND, "404", "이메일로 멤버를 찾을 수 없습니다."),
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "친구를 찾을 수 없습니다."),
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "할 일을 찾을 수 없습니다."),
    // 409 CONFLICT
    CONFLICT(HttpStatus.CONFLICT, "409", "요청한 리소스와 충돌이 발생했습니다."),
    // 500 INTERNAL_SERVER_ERROR
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "서버 내부 오류입니다."),
    PASSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "Oauth passing error."),
    // 503 SERVICE_UNAVAILABLE
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "503", "서비스를 사용할 수 없습니다."),
    // 504 GATEWAY_TIMEOUT
    GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "504", "서버가 응답하지 않습니다."),
    // 400 BAD_REQUEST
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "400", "잘못된 파라미터입니다."),

    CANNOT_BE_FRIEND_WITH_SELF(HttpStatus.BAD_REQUEST, "400", "스스로와 친구가 될 수 없습니다."),
    INVALID_REDIRECT_URI(HttpStatus.BAD_REQUEST, "400", "유효하지 않은 리다이렉트 URI입니다."),;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}
