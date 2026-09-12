package hongik.Todoing.domain.jwt;

import hongik.Todoing.domain.auth.util.PrincipalDetails;
import hongik.Todoing.domain.auth.service.PrincipalDetailService;
import hongik.Todoing.domain.jwt.dto.JwtDTO;
import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.global.apiPayload.exception.GeneralException;
import hongik.Todoing.global.util.RedisUtil;
import io.jsonwebtoken.Jwts;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.GrantedAuthority;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtil {

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final Long access;
    private final Long refreshTokenExpiration;
    private final RedisUtil redisUtil;
    private final PrincipalDetailService principalDetailService;

    public JwtUtil(@Value("${spring.jwt.secret}") String secret,
                   @Value("${spring.jwt.token.access-token-expire-time}") Long access,
                   @Value("${spring.jwt.token.refresh-token-expire-time}") Long refreshTokenExpiration,
                   RedisUtil redisUtil, PrincipalDetailService principalDetailService) {
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
        this.access = access;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.redisUtil = redisUtil;
        this.principalDetailService = principalDetailService;
    }

    public String getUsername(String token) throws SignatureException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String getRoles(String token) throws SignatureException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("roles", String.class);
    }

    public String getTokenType(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("type", String.class);
    }

    public long getExpirationTime(String token) throws SignatureException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .getTime();
    }

    private String issueToken(String subject, String roles, String type, Instant expiration) {
        return Jwts.builder()
                .header()
                .add("typ", "JWT")
                .and()
                .subject(subject)
                .claim("roles", roles)
                .claim("type", type)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    private static String authoritiesOf(PrincipalDetails principalDetails) {
        return principalDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }

    public String createJwtAccessToken(PrincipalDetails principalDetails) {
        Instant expiration = Instant.now().plusMillis(access);
        return issueToken(principalDetails.getUsername(), authoritiesOf(principalDetails), TOKEN_TYPE_ACCESS, expiration);
    }

    public String createJwtRefreshToken(PrincipalDetails principalDetails) {
        Instant expiration = Instant.now().plusMillis(refreshTokenExpiration);
        String refreshToken = issueToken(principalDetails.getUsername(), authoritiesOf(principalDetails), TOKEN_TYPE_REFRESH, expiration);

        try {
            if (redisUtil != null) {
                redisUtil.save(
                        principalDetails.getUsername(),
                        refreshToken,
                        refreshTokenExpiration,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            log.warn("[*] Redis 저장 실패: 로컬 환경이거나 Redis 서버 없음");
        }

        return refreshToken;
    }

    public String resolveAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.info("[*] JWT Token not found");
            return null;
        }
        log.info("[*] JWT Token found");

        return authorizationHeader.split(" ")[1];
    }

    public boolean validateRefreshToken(String refreshToken) throws SignatureException {
        if (!TOKEN_TYPE_REFRESH.equals(getTokenType(refreshToken))) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }

        String username = getUsername(refreshToken);

        if(!redisUtil.hasKey(username)) {
            throw new GeneralException(ErrorStatus.INVALID_PARAMETER);
        }
        return true;
    }

    public JwtDTO reissueToken(String refreshToken) throws SignatureException {
        UserDetails userDetails = principalDetailService.loadUserByUsername(getUsername(refreshToken));

        return new JwtDTO(
                createJwtAccessToken((PrincipalDetails) userDetails),
                createJwtRefreshToken((PrincipalDetails) userDetails)
        );
    }

    public String createAccessToken(String email, String role) {
        Instant expiration = Instant.now().plusMillis(access);
        return issueToken(email, role, TOKEN_TYPE_ACCESS, expiration);
    }

}
