package hongik.Todoing.domain.order.controller;

import hongik.Todoing.domain.auth.security.PrincipalDetails;
import hongik.Todoing.domain.order.facade.PassUsecase;
import hongik.Todoing.domain.order.dto.order.response.KakaoReadyResponse;
import hongik.Todoing.domain.order.dto.pass.response.PassRemainingResponse;
import hongik.Todoing.domain.order.dto.pass.response.PassUsageLogResponse;
import hongik.Todoing.infrastructure.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order/pass")
public class PassController {
    private final PassUsecase passUsecase;

    @Operation(summary = "카카오페이 결제 요청을 준비합니다.")
    @PostMapping("/ready")
    public ApiResponse<KakaoReadyResponse> requestOrderPass(
            @AuthenticationPrincipal PrincipalDetails principal) {
        return ApiResponse.onSuccess(passUsecase.requestOrderPass(principal.getUser().getId()));
    }

    @Operation(summary = "카카오페이 결제 완료 후 패스를 발급합니다.")
    @GetMapping("/complete")
    public ApiResponse<?> completeOrderPass(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestParam String tid, @RequestParam String pgToken) {
        passUsecase.completeOrderPass(principal.getUser().getId(), tid, pgToken);
        return ApiResponse.onSuccess("null"); // 성공 페이지로 리다이렉트
    }

    @Operation(summary = "내 사용권 사용 기록을 조회합니다.")
    @GetMapping("/usage")
    public ApiResponse<List<PassUsageLogResponse>> getUsageHistory(
            @AuthenticationPrincipal PrincipalDetails principal) {
        return ApiResponse.onSuccess(passUsecase.getUsageHistory(principal.getUser().getId()));
    }

    @Operation(summary = "남은 사용권 개수를 조회합니다.")
    @GetMapping("/remaining")
    public ApiResponse<PassRemainingResponse> getRemainingCount(
            @AuthenticationPrincipal PrincipalDetails principal) {
        return ApiResponse.onSuccess(passUsecase.getRemainingCount(principal.getUser().getId()));
    }
}
