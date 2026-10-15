package hongik.Todoing.domain.auth.dto;

import hongik.Todoing.domain.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class KakaoLoginResponseDto {
    private String email;
    private String name;
    private String accessToken;

    public static KakaoLoginResponseDto of(User user, String accessToken) {
        return KakaoLoginResponseDto.builder()
                .email(user.getEmail())
                .name(user.getNickname())
                .accessToken(accessToken)
                .build();
    }
}
