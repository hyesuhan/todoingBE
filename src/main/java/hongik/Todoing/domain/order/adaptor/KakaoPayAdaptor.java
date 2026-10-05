package hongik.Todoing.domain.order.adaptor;

import hongik.Todoing.domain.order.domain.order.Order;
import hongik.Todoing.domain.order.dto.order.request.KakaoReadyRequest;
import hongik.Todoing.domain.order.dto.order.response.KakaoApprovePayload;
import hongik.Todoing.domain.order.dto.order.response.KakaoApproveResponse;
import hongik.Todoing.domain.order.dto.order.response.KakaoReadyPayload;
import hongik.Todoing.domain.order.dto.order.response.KakaoReadyResponse;
import hongik.Todoing.infrastructure.config.PaymentsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class KakaoPayAdaptor {

    private final RestTemplate restTemplate;
    private final PaymentsProperties properties;

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + properties.getSecretKey());
        return headers;
    }

    public KakaoReadyResponse ready(Order order) {
        String url = properties.getReadyKakaoUrl();

        KakaoReadyPayload readyPayload = new KakaoReadyPayload(order, properties);

        return restTemplate.postForObject(url, new HttpEntity<>(readyPayload.toReadyMap(), headers()), KakaoReadyResponse.class);

    }

    public KakaoReadyResponse readyWithDto(KakaoReadyRequest request) {
        String url = properties.getReadyUrl();

        KakaoReadyPayload payload = new KakaoReadyPayload(request, properties);

        return  restTemplate.postForObject(url, new HttpEntity<>(payload.toReadyMap(), headers()), KakaoReadyResponse.class);
    }

    public KakaoApproveResponse approve(Order order, String pgToken) {
        String url = properties.getApproveKakaoUrl();

        KakaoApprovePayload approvePayload = new KakaoApprovePayload(order, properties, pgToken);

        return restTemplate.postForObject(url, new HttpEntity<>(approvePayload.toApproveMap(pgToken), headers()), KakaoApproveResponse.class);
    }

    /*
    public KakaoCancelResponse cancelByTid()

     */

    /*
    public Map<String, Object> toApproveMap(String pgToken) {
        return Map.of(
                "cid", cid,
                "partner_order_id", partner_order_id,
                "partner_user_id", partner_user_id,
                "pg_token", pgToken
        );
    }
     */
}
