package hongik.Todoing.domain.order.controller;

import hongik.Todoing.domain.auth.security.PrincipalDetails;
import hongik.Todoing.domain.order.dto.order.response.OrderSummaryResponse;
import hongik.Todoing.domain.order.service.OrderService;
import hongik.Todoing.infrastructure.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "내 결제 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<OrderSummaryResponse>> getMyOrders(
            @AuthenticationPrincipal PrincipalDetails principal) {
        return ApiResponse.onSuccess(orderService.getMyOrders(principal.getUser().getId()));
    }
}
