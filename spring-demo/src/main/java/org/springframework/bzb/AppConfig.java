package org.springframework.bzb;

import org.springframework.context.annotation.*;
import org.springframework.bzb.entity.Student;
import org.springframework.bzb.service.BzbService;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAspectJAutoProxy
@Configuration
@ComponentScan(basePackages = {"org.springframework.bzb"})
@PropertySource(value = {"classpath:test.properties"})
@Import({Student.class
		//MyImportSelector.class, MyImportBeanDefinitionRegistrar.class, MyDefereredImportSelector.class
})
// 启用事物
@EnableTransactionManagement
public class AppConfig {

	@Bean
	public BzbService bzbService() {
		return new BzbService();
	}

}
