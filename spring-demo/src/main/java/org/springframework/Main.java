package org.springframework;

import org.springframework.aop.Louzai;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.entity.Person;
import org.springframework.entity.Student;
import org.springframework.entity.User;
import org.springframework.service.AService;
import org.springframework.service.AopService;
import org.springframework.service.BzbService;

public class Main {
	public static void main(String[] args) {
		System.out.println("Hello world!");

		//ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:beans.xml");
		//User bean = context.getBean(User.class);
		//System.out.println(bean);

		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		//BzbService bzbService = annotationConfigApplicationContext.getBean(BzbService.class);
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

		Louzai louzai = annotationConfigApplicationContext.getBean(Louzai.class);
		louzai.everyDay();

		//AopService aopService = annotationConfigApplicationContext.getBean(AopService.class);
		//aopService.testAop();


	}
}