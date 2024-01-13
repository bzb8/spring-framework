package org.springframework;

import org.springframework.aop.ILouzai;
import org.springframework.aop.Louzai;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.service.BzbService;
import org.springframework.service.DService;

public class Main {
	public static void main(String[] args) {
		System.out.println("Hello world!");

		//ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:beans.xml");
		//User bean = context.getBean(User.class);
		//System.out.println(bean);

		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		BzbService bzbService = annotationConfigApplicationContext.getBean(BzbService.class);
		//System.out.println(bzbService);
		//User user = annotationConfigApplicationContext.getBean(User.class);
		//System.out.println(user);
		//Student student = annotationConfigApplicationContext.getBean(Student.class);
		//System.out.println(student);
		//Person person = annotationConfigApplicationContext.getBean(Person.class);
		//System.out.println(person);
		//
		//AService aService = annotationConfigApplicationContext.getBean(AService.class);
		//aService.testA();

		//ILouzai louzai = annotationConfigApplicationContext.getBean(ILouzai.class);
		//louzai.everyDay();
		//
		//DService dService = annotationConfigApplicationContext.getBean(DService.class);

		Object myFactoryBean = annotationConfigApplicationContext.getBean("myFactoryBean");


		//AopService aopService = annotationConfigApplicationContext.getBean(AopService.class);
		//aopService.testAop();


	}
}