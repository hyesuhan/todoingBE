package hongik.Todoing.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Bean
    public static CorsConfigurationSource apiConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 프론트 주소 명시적으로 설정 (예: 로컬, 배포 환경)
        List<String> allowedOrigins = List.of(
                "http://localhost:3000",
                "http://localhost:5173",// 개발용 React 주소
                "https://todooungi.netlify.app",      // 실제 배포 주소
                "http://43.203.235.223" // 실제 배포 주소
        );
        configuration.setAllowedOrigins(allowedOrigins);

        // 2. 허용 메서드 제한 (보통 GET, POST, PUT, DELETE 정도)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 3. 허용 헤더 설정 (보통 Authorization 등)
        configuration.addAllowedHeader("*");

        // 4. 쿠키/토큰 교환 허용 (프론트에서 withCredentials: true 설정 필요)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
