package hongik.Todoing.domain.auth.controller;

import hongik.Todoing.domain.auth.dto.EmailSendCodeRequest;
import hongik.Todoing.domain.auth.dto.EmailVerifyCodeRequest;
import hongik.Todoing.domain.auth.dto.LoginRequestDTO;
import hongik.Todoing.domain.auth.dto.SignUpRequestDto;
import hongik.Todoing.domain.auth.service.AuthService;
import hongik.Todoing.domain.auth.service.EmailVerificationCommandService;
import hongik.Todoing.domain.auth.jwt.JwtUtil;
import hongik.Todoing.domain.auth.jwt.dto.JwtDTO;
import hongik.Todoing.infrastructure.apiPayload.ApiResponse;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.code.status.SuccessStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.SignatureException;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final AuthService authService;
    private final EmailVerificationCommandService emailVerificationCommandService;

    @Operation(summary = "토큰을 재발급합니다.")
    @GetMapping("/reissue")
    public ApiResponse<JwtDTO> reissueToken(@RequestHeader("RefreshToken") String refreshToken ) {
        try {
            jwtUtil.validateRefreshToken(refreshToken);
            return ApiResponse.onSuccess(
                    jwtUtil.reissueToken(refreshToken)
            );
        } catch (ExpiredJwtException e) {
            throw new GeneralException(ErrorStatus.TOKEN_EXPIRED);
        } catch (JwtException | SignatureException e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    /*
    @Operation(summary = "카카오톡으로 로그인합니다.")
    @GetMapping("/login/kakao")
    public ApiResponse<?> kakaoLogin(@RequestParam("code") String accessCode, HttpServletResponse response) {
        User user = authService.loginByOAuth(accessCode, response);

        String accessToken = response.getHeader("Authorization");
        return ApiResponse.onSuccess(KakaoLoginResponseDto.of(user, accessToken));
    }
     */

    // 로그인
    @Operation(summary = "일반 로그인합니다.")
    @PostMapping("/login")
    public ApiResponse<JwtDTO> loginWithEmail(@RequestBody LoginRequestDTO request) {
        JwtDTO jwtDTO = authService.loginByEmail(request.email(), request.password());
        return ApiResponse.onSuccess(jwtDTO);
    }

    // 회원 가입
    @Operation(summary = "회원 가입합니다.")
    @PostMapping("/signup")
    public ApiResponse<?> signUpWithEmail(@RequestBody SignUpRequestDto request) {
        authService.signUpByEmail(request);
    return ApiResponse.onSuccess(SuccessStatus._CREATED);
    }

    @Operation(summary = "회원가입 전 이메일 인증번호를 발송합니다.")
    @PostMapping("/email/send-code")
    public ApiResponse<Void> sendEmailCode(@Valid @RequestBody EmailSendCodeRequest request) {
        emailVerificationCommandService.sendCode(request.email());
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "이메일 인증번호를 확인합니다.")
    @PostMapping("/email/verify-code")
    public ApiResponse<Void> verifyEmailCode(@Valid @RequestBody EmailVerifyCodeRequest request) {
        emailVerificationCommandService.verifyCode(request.email(), request.code());
        return ApiResponse.onSuccess(null);
    }

}
