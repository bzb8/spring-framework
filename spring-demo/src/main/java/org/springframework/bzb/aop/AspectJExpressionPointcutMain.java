package org.springframework.bzb.aop;

import com.sun.istack.internal.NotNull;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;

public class AspectJExpressionPointcutMain {

	public static void main(String[] args) {
		//String expression = "args()"; // 所有没有入参的方法会被拦截,如：age()会拦截,但是age(int i)不会被拦截
		// ... AspectJExpressionPointcut支持11种表达式（也就是Spring全部支持的切点表达式类型）
		String expression = "execution(public int                        cccccccccccv                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        v                                vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvb                                                                                                                                                                                                                                                                                                                                                                                org.springframework.aop.User.age())";
		// 代理工厂
		ProxyFactory proxyFactory = new ProxyFactory(new User());
		// 切点
		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(expression);
		// 通知
		DoMethodInterceptor advice = new DoMethodInterceptor();
		Advisor advisor = new DefaultPointcutAdvisor(pointcut, advice);
		proxyFactory.addAdvisor(advisor);
		User user = (User) proxyFactory.getProxy();

		user.age();
		user.age(20);
		user.money();
		user.money("小源仔", 1000000);


	}


	static class DoMethodInterceptor implements MethodInterceptor {

		@Override
		public Object invoke(@NotNull MethodInvocation invocation) throws Throwable {
			System.out.println("...放行前拦截...");
			Object obj = invocation.proceed();
			System.out.println("...放行后拦截...");
			return obj;
		}
	}
}
