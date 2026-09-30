package com.fashionsystem.fashion_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

// AuthService verifies database credentials; JwtAuthenticationFilter authenticates requests.
// A custom filter chain alone does not prevent Boot from creating its fallback user.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class FashionSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(FashionSystemApplication.class, args);
	}

}
