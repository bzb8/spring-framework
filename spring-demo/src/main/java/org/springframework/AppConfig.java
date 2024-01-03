package org.springframework;

import org.springframework.config.MyDefereredImportSelector;
import org.springframework.config.MyImportBeanDefinitionRegistrar;
import org.springframework.config.MyImportSelector;
import org.springframework.context.annotation.*;
import org.springframework.entity.Student;
import org.springframework.service.BzbService;

@EnableAspectJAutoProxy
@Configuration
@ComponentScan(basePackages = {"org.springframework.service", "org.springframework.aop", "org.springframework.config"})
@PropertySource(value = {"classpath:test.properties"})
@Import({Student.class
		//MyImportSelector.class, MyImportBeanDefinitionRegistrar.class, MyDefereredImportSelector.class
})
public class AppConfig {

	@Bean
	public BzbService bzbService() {
		return new BzbService();
	}

}
