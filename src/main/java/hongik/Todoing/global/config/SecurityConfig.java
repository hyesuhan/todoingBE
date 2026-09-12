package hongik.Todoing.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hongik.Todoing.domain.jwt.JwtAuthenticationFilter;
import hongik.Todoing.domain.jwt.JwtAuthorizationFilter;
import hongik.Todoing.domain.jwt.JwtUtil;
import hongik.Todoing.domain.member.repository.MemberRepository;
import hongik.Todoing.global.apiPayload.ApiResponse;
import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.global.util.RedisUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final MemberRepository memberRepository;

    private final String[] allowedUrls = {
            "/login",
            "/api/users/login",
            "/api/users/login/kakao",
            "/api/users/signup",
            "/api/users/reissue",
            "/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**",
            "/chat",
            "/api/verification/**"
    };

    @Bean
    public BCryptPasswordEncoder encodePassword() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {


        // cors 비활성화
        http
                .cors(cors -> cors
                        .configurationSource(CorsConfig.apiConfigurationSource()));

        // csrf disable
        http
                .csrf(AbstractHttpConfigurer::disable);

        // form 로그인 방식 disable
        http
                .formLogin(AbstractHttpConfigurer::disable);

        // http basic 인증 방식 disable
        http
                .httpBasic(AbstractHttpConfigurer::disable);

        // Session을 사용하지 않고, Stateless 서버를 만듬.
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 경로별 인가 — allowedUrls에 없으면 기본적으로 인증 필요 (permitAll이었던 catch-all을 authenticated로)
        http.
                authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers(allowedUrls).permitAll()
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                .anyRequest().authenticated()
                );

        // 인증/인가 실패 시 JSON 401/403 응답 (기본값은 로그인 페이지 리다이렉트 또는 빈 응답)
        http
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) ->
                                writeErrorResponse(response, ErrorStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(response, ErrorStatus.FORBIDDEN))
                );

        // ✅ JWT 인증 필터 (로그인)
        JwtAuthenticationFilter loginFilter = new JwtAuthenticationFilter(
                authenticationManager(authenticationConfiguration), jwtUtil);
        loginFilter.setFilterProcessesUrl("/login");

        // ✅ JWT 권한 필터 (모든 요청에 대해 accessToken 확인)
        JwtAuthorizationFilter authorizationFilter = new JwtAuthorizationFilter(jwtUtil, redisUtil, memberRepository);

        // 필터 순서 중요: 권한 필터는 로그인 필터보다 먼저 실행돼야 함
        http
                .addFilterBefore(authorizationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorStatus errorStatus) throws IOException {
        response.setStatus(errorStatus.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        new ObjectMapper().writeValue(
                response.getWriter(),
                ApiResponse.onFailure(errorStatus.getCode(), errorStatus.getMessage(), null)
        );
    }

}
