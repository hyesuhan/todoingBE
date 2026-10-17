package hongik.Todoing.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerifyCodeRequest(
        @NotBlank @Email String email,
        @NotBlank String code
) {
}
