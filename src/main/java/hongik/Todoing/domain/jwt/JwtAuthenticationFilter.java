package hongik.Todoing.domain.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import hongik.Todoing.domain.jwt.dto.JwtDTO;
import hongik.Todoing.domain.auth.util.PrincipalDetails;
import hongik.Todoing.domain.auth.dto.LoginRequestDTO;
import hongik.Todoing.infrastructure.apiPayload.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    // login 요청 시 로그인 시도를 위해 실행되는 함수
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException {

        System.out.println("JwtAuthenticationFilter: 로그인 시도 중");
        // 1. username, password 받아서
        ObjectMapper om =new ObjectMapper();
        LoginRequestDTO loginRequestDTO;
        try {
            loginRequestDTO = om.readValue(request.getInputStream(), LoginRequestDTO.class);
            } catch (IOException e) {
            throw new AuthenticationServiceException("Error of request body.");
        }

        // usernamePassword Token generate
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        loginRequestDTO.email(),
                        loginRequestDTO.password()
                );


        // 인증 완료 시 successfulAuthentication
        // 인증 실패 시 unsuccessfulAuthentication
        return authenticationManager.authenticate(authenticationToken);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain, Authentication authentication) throws IOException, ServletException {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

        log.info("[*] Login Success - Login with" + principalDetails.getUsername());
        JwtDTO jwtDTO = new JwtDTO(
                jwtUtil.createJwtAccessToken(principalDetails),
                jwtUtil.createJwtRefreshToken(principalDetails)
        );

        log.info("Access token: " + jwtDTO.accessToken());
        log.info("Refresh token: " + jwtDTO.refreshToken());

        ApiResponse<JwtDTO> apiResponse = ApiResponse.onSuccess(jwtDTO);

        response.setStatus(HttpStatus.CREATED.value());
        response.setContentType("application/json;charset=UTF-8");

        ObjectMapper om = new ObjectMapper();
        om.writeValue(response.getWriter(), apiResponse);

    }
}
