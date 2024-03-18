package org.springframework.bzb.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import java.lang.reflect.Modifier;

/**
 * @Author xiaoyuanzai
 * @Date 2023/6/5 20:09
 * @description:
 */
@Aspect
@Component
public class MyAspectV3 {

	//定义一个切入点：指定哪些方法可以被切入（如果是别的类需要使用 请用该方法的全类名）
	@Pointcut(value = "execution(public void org.springframework.aop.UserService.test())")
	public void pointCut() {
	}

	@Before("pointCut()")
	public void beforeAdvice(JoinPoint joinPoint) {
		System.out.println("目标方法名为:" + joinPoint.getSignature().getName());
		System.out.println("目标方法所属类的简单类名:" + joinPoint.getSignature().getDeclaringType().getSimpleName());
		System.out.println("目标方法所属类的类名:" + joinPoint.getSignature().getDeclaringTypeName());
		System.out.println("目标方法声明类型:" + Modifier.toString(joinPoint.getSignature().getModifiers()));
		//获取传入目标方法的参数
		Object[] args = joinPoint.getArgs();
		for (int i = 0; i < args.length; i++) {
			System.out.println("第" + (i + 1) + "个参数为: " + args[i]);
		}
		System.out.println("被代理的对象:" + joinPoint.getTarget());
		System.out.println("代理对象自己:" + joinPoint.getThis());


	}


}