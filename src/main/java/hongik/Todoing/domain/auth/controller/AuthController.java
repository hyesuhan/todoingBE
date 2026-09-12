package hongik.Todoing.domain.auth.controller;

import hongik.Todoing.domain.auth.converter.AuthConverter;
import hongik.Todoing.domain.auth.dto.LoginRequestDTO;
import hongik.Todoing.domain.auth.dto.SignUpRequestDto;
import hongik.Todoing.domain.auth.service.AuthService;
import hongik.Todoing.domain.jwt.JwtUtil;
import hongik.Todoing.domain.jwt.dto.JwtDTO;
import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.global.apiPayload.ApiResponse;
import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.global.apiPayload.code.status.SuccessStatus;
import hongik.Todoing.global.apiPayload.exception.GeneralException;
import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.SignatureException;

@RestController
@AllArgsConstructor
@RequestMapping("/api/users")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @Operation(summary = "토큰을 재발급합니다.")
    @GetMapping("/reissue")
    public ApiResponse<JwtDTO> reissueToken(@RequestHeader("RefreshToken") String refreshToken ) {
        try {
            jwtUtil.validateRefreshToken(refreshToken);
            return ApiResponse.onSuccess(
                    jwtUtil.reissueToken(refreshToken)
            );
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        } catch (ExpiredJwtException e) {
            throw  new GeneralException(ErrorStatus.TOKEN_EXPIRED);
        }
    }

    @Operation(summary = "카카오톡으로 로그인합니다.")
    @GetMapping("/login/kakao")
    public ApiResponse<?> kakaoLogin(@RequestParam("code") String accessCode, HttpServletResponse response) {
        User user = authService.loginByOAuth(accessCode, response);

        String accessToken = response.getHeader("Authorization");
        return ApiResponse.onSuccess(AuthConverter.JoinResponse(user, accessToken));
    }

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

}
