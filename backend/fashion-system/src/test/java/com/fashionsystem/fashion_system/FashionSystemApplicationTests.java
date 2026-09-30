package com.fashionsystem.fashion_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FashionSystemApplicationTests {

	@Autowired
	ApplicationContext context;

	@Test
	void customSecurityDoesNotCreateDefaultUserDetailsService() {
		assertThat(context.getBeansOfType(SecurityFilterChain.class)).hasSize(1);
		assertThat(context.getBeansOfType(UserDetailsService.class)).isEmpty();
		assertThat(context.containsBean("inMemoryUserDetailsManager")).isFalse();
	}

	@Test
	void contextLoads() {
	}

}
