package hongik.Todoing.domain.auth.dto;

import lombok.Getter;

@Getter
public class SignUpRequestDto {

    private String nickname;
    private String email;
    private String password;
}
