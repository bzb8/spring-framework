package org.springframework.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * AopTest
 *
 * @author: bzb
 * @date: 2023-12-08 09:27
 **/
@Aspect
@Component
public class AopTest {

	@Before("execution(* org.springframework.service.AopService.testAop(..))")
	public void takeSeats() {
		System.out.println("I am Aop @Before");
	}

}
