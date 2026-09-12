package hongik.Todoing.domain.order.controller;

import hongik.Todoing.domain.order.dto.pass.request.KakaoReadyRequest;
import hongik.Todoing.domain.order.facade.PassUsecase;
import hongik.Todoing.domain.order.dto.order.response.KakaoReadyResponse;
import hongik.Todoing.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order/pass")
public class PassController {
    private final PassUsecase passUsecase;

    // 카카오 결제 준비
    @Operation(summary = "카카오페이 결제 요청을 준비합니다.[통합 테스트용]")
    @PostMapping("/ready")
    public ApiResponse<KakaoReadyResponse> requestOrderPass(
            @RequestBody KakaoReadyRequest request) {
        // 카카오페이 결제 준비 요청
        return ApiResponse.onSuccess(passUsecase.requestOrderPass(request.getUserId(), request.getProductCode(), request.getQuantity()));
    }

    @Operation(summary = "카카오페이 결제 완료 후 패스를 발급합니다.[통합 테스트]")
    @GetMapping("/complete")
    public ApiResponse<?> completeOrderPass(
            @RequestParam Long userId, @RequestParam String tid, @RequestParam String pgToken) {
        // 카카오페이 결제 완료 후 패스 발급
        passUsecase.completeOrderPass(userId, tid, pgToken);
        return ApiResponse.onSuccess("null"); // 성공 페이지로 리다이렉트
    }

    //@Operation(summary = "AI 사용 시 Pass가 1개씩 차감됩니다.")
}
