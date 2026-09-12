package hongik.Todoing.domain.member.controller;

import hongik.Todoing.domain.auth.util.PrincipalDetails;
import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.domain.member.dto.response.GetProfileDTO;
import hongik.Todoing.domain.member.dto.response.UpdateProfileDTO;
import hongik.Todoing.domain.member.service.MemberService;
import hongik.Todoing.global.apiPayload.ApiResponse;
import hongik.Todoing.global.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 내 프로필 조회
    @Operation(summary = "내 프로필을 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<GetProfileDTO> getMyProfile(
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = principal.getUser();
        return ApiResponse.onSuccess(memberService.getProfile(user));
    }

    // 내 프로필 변경
    @Operation(summary = "내 프로필을 변경합니다.")
    @PatchMapping("/me")
    public ApiResponse<Void> updateMyProfile (
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody UpdateProfileDTO dto
            ) {
        User user = principal.getUser();
        memberService.updateProfile(user, dto);
        return ApiResponse.of(SuccessStatus._OK, null);
    }

}
