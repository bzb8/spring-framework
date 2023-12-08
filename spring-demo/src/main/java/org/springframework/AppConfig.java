package org.springframework;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy
@Configuration
@ComponentScan(basePackages = {"org.springframework.service", "org.springframework.aop"})
public class AppConfig {

	@Bean
	public BzbService bzbService() {
		return new BzbService();
	}

}
