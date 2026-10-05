package hongik.Todoing.infrastructure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class PaymentsProperties {
    @Value("${kakaopay.cid}")
    private String cid;
    @Value("${kakaopay.secret_key}")
    private String secretKey;

    private final String apiUrl = "https://open-api.kakaopay.com/online";
    private final String localUrl = "http://localhost:8080";
    private final String readyUrl = "/v1/payment/ready";
    private final String approveUrl = "/v1/payment/approve";
    private final String cancelUrl = "/v1/payment/cancel";
    private final String failUrl = "/v1/payment/fail";

    public String getApproveKakaoUrl() {
        return apiUrl + approveUrl;
    }

    public String getReadyKakaoUrl() {
        return apiUrl + readyUrl;
    }

    public String getCancelRedirect() {
        return apiUrl + cancelUrl;
    }

    public String getFailRedirect() {
        return apiUrl + failUrl;
    }

    public String getLocalApproveUrl() {
        return localUrl + approveUrl;
    }

    public String getLocalReadyUrl() {
        return localUrl + readyUrl;
    }

    public String getLocalCancelRedirect() {
        return localUrl + cancelUrl;
    }

    public String getLocalFailRedirect() {
        return localUrl + failUrl;
    }
}
