package umc.catchy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@EnableFeignClients
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@EnableAsync
public class CatchyApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatchyApplication.class, args);
	}

}
