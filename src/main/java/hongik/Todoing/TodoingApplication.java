package hongik.Todoing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EntityScan(basePackages = "hongik.Todoing.domain")
public class TodoingApplication {

	public static void main(String[] args) {
		SpringApplication.run(TodoingApplication.class, args);
	}



}
