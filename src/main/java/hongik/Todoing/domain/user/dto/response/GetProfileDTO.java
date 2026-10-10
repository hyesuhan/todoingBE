package hongik.Todoing.domain.user.dto.response;

import hongik.Todoing.domain.user.domain.User;

public record GetProfileDTO(
        String email,
        String nickname
) {
    public static GetProfileDTO from(User user) {
        return new GetProfileDTO(user.getEmail(), user.getNickname());
    }
}
