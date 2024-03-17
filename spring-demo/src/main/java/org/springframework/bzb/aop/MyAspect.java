package org.springframework.bzb.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @Author xiaoyuanzai
 * @Date 2023/5/12 20:33
 * @description:
 */
@Aspect
@Component
public class MyAspect {

	//定义一个切入点：指定哪些方法可以被切入（如果是别的类需要使用 请用该方法的全类名）
	@Pointcut("execution(public void org.springframework.bzb.aop.UserService.test())")
	public void pointCut() {
	}

	@Before("pointCut()")
	public void beforeAdvice(JoinPoint joinPoint) {
		System.out.println("AOP Before Advice...");
	}

	@After("pointCut()")
	public void AfterAdvice(JoinPoint joinPoint) {
		System.out.println("AOP After Advice...");
	}

	@Around("pointCut()")
	public void around(ProceedingJoinPoint joinPoint) {
		System.out.println("AOP Aronud before...");
		try {
			joinPoint.proceed();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		System.out.println("AOP Aronud after...");
	}

	@AfterThrowing(pointcut = "pointCut()", throwing = "error")
	public void afterThrowingAdvice(JoinPoint joinPoint, Throwable error) {
		System.out.println("AOP AfterThrowing Advice..." + error);
		System.out.println("AfterThrowing...");
	}

	//	环绕通知：此处有一个坑，当AfterReturning和Around共存时，AfterReturning是获取不到返回值的
	//	@AfterReturning(pointcut = "pointCut()", returning = "returnVal")
	//	public void afterReturnAdvice(JoinPoint joinPoint, Object returnVal) {
	//		System.out.println("AOP AfterReturning Advice:" + returnVal);
	//	}

}