package org.springframework;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.service.AopService;

public class Main {
	public static void main(String[] args) {
		System.out.println("Hello world!");

		//ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:beans.xml");
		//User bean = context.getBean(User.class);
		//System.out.println(bean);

		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		BzbService bzbService = annotationConfigApplicationContext.getBean(BzbService.class);
		System.out.println(bzbService);

		AopService aopService = annotationConfigApplicationContext.getBean(AopService.class);
		aopService.testAop();
	}
}