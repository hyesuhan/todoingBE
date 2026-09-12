package hongik.Todoing.domain.auth.converter;

import hongik.Todoing.domain.auth.dto.KakaoLoginResponseDto;
import hongik.Todoing.domain.member.domain.User;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthConverter {

    public static User toMember(String email, String nickname, String password, PasswordEncoder passwordEncoder) {

        String passwordToUse = password != null ? passwordEncoder.encode(password) :
                passwordEncoder.encode("defaultPassword");
        return User.builder()
                .email(email)
                .role("ROLE_USER")
                .password(passwordToUse)
                .nickname(nickname)
                .build();
    }

    public static KakaoLoginResponseDto JoinResponse(User user, String accessToken) {
        return KakaoLoginResponseDto.builder()
                .email(user.getEmail())
                .name(user.getNickname())
                .accessToken(accessToken)
                .build();

    }
}
