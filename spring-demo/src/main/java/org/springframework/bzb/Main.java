package org.springframework.bzb;

import org.springframework.bzb.transactionbzb.Role;
import org.springframework.bzb.transactionbzb.UserService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
	public static void main(String[] args) {
		System.out.println("Hello world!");

		//ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:beans.xml");
		//User bean = context.getBean(User.class);
		//System.out.println(bean);

		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		//UserService userService = annotationConfigApplicationContext.getBean(UserService.class);
		//userService.test();

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

		//ILouzai louzai = annotationConfigApplicationContext.getBean(ILouzai.class);
		//louzai.everyDay();
		//
		//DService dService = annotationConfigApplicationContext.getBean(DService.class);

		//Object myFactoryBean = annotationConfigApplicationContext.getBean("myFactoryBean");


		//AopService aopService = annotationConfigApplicationContext.getBean(AopService.class);
		//aopService.testAop();

		testTransaction(applicationContext);

		//RoleService roleService = applicationContext.getBean(RoleService.class);
		//System.out.println(roleService.getById(1));

	}

	/**
	 * 测试事务
	 * @param applicationContext
	 */
	private static void testTransaction(AnnotationConfigApplicationContext applicationContext) {
		UserService userService = applicationContext.getBean(UserService.class);
		Role role = new Role(2, "ROLE_BZB1");
		userService.insertRole(role);
	}
}