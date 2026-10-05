package hongik.Todoing.infrastructure.config;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /*
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        messageConverters.add(new FormHttpMessageConverter());
        restTemplate.setMessageConverters(messageConverters);

        return restTemplate;
    }

     */

    /* 카카오 페이용 */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        // 커넥션 풀 설정 -> 현대 spring boot 3버전이어서 gradle에 추가하려 했는데 굳이 인거 같아서 안함
        return restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(30)) // 연결 타임아웃 설정
                .readTimeout(Duration.ofSeconds(30)) // 읽기 타임아웃 설정
                .build();
    }
}
