package org.springframework.config;

import org.springframework.BzbService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

	@Bean
	public BzbService bzbService() {
		return new BzbService();
	}

}
