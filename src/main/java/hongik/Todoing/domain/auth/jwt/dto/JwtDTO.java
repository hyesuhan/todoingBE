package hongik.Todoing.domain.auth.jwt.dto;

public record JwtDTO (
        String accessToken,
        String refreshToken
) {
}
