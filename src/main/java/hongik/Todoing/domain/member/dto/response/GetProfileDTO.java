package hongik.Todoing.domain.member.dto.response;

import hongik.Todoing.domain.member.domain.User;

public record GetProfileDTO(
        String email,
        String nickname
) {
    public static GetProfileDTO from(User user) {
        return new GetProfileDTO(user.getEmail(), user.getNickname());
    }
}
