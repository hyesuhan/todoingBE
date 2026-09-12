package hongik.Todoing.domain.order.dto.pass.request;

import hongik.Todoing.domain.order.domain.pass.ProductCode;
import lombok.Getter;

@Getter
public class KakaoReadyRequest {
    private Long userId;
    private ProductCode productCode;
    private int quantity;

}
