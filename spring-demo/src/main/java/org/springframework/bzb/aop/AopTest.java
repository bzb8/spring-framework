package org.springframework.bzb.aop;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class AopTest {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig1.class);
		//获取代理对象，代理对象bean的名称为注册ProxyFactoryBean的名称，即：service1Proxy
		Service1 bean = context.getBean("service1Proxy", Service1.class);
		System.out.println("----------------------");
		//调用代理的方法
		bean.m1();
		System.out.println("----------------------");
		//调用代理的方法
		bean.m2();
	}
}
