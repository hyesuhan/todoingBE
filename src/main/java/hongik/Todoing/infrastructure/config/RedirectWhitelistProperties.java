package hongik.Todoing.infrastructure.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Configuration
@ConfigurationProperties(prefix = "kakaopay.redirect.whitelist")
public class RedirectWhitelistProperties {
    private final List<String> allowedRedirectUris = List.of();
}
