package hongik.Todoing.domain.jwt;

import hongik.Todoing.domain.auth.util.PrincipalDetails;
import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.domain.member.service.MemberCacheService;
import hongik.Todoing.global.apiPayload.exception.GeneralException;
import hongik.Todoing.global.util.RedisUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SignatureException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    // private final RedisUtil redisUtil;
    private final MemberCacheService memberCacheService;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain) throws ServletException, IOException {

        log.info("JwtAuthorizationFilter: 인증 시작");

        String accessToken = jwtUtil.resolveAccessToken(request);

        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!"access".equals(jwtUtil.getTokenType(accessToken))) {
                log.warn("[*] case : not an access token");
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtUtil.getUsername(accessToken);

            User user = memberCacheService.getByEmail(email);

            PrincipalDetails principalDetails = new PrincipalDetails(user);

            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    principalDetails,
                    null,
                    principalDetails.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (JwtException | IllegalArgumentException | GeneralException | SignatureException e) {
            log.warn("[*] case : invalid access token ({})", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
