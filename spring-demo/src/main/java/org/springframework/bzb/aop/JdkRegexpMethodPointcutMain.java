package org.springframework.bzb.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.JdkRegexpMethodPointcut;
import org.springframework.lang.Nullable;

/**
 * @Author xiaoyuanzai
 * @Date 2023/5/28 21:26
 * @description:
 */
public class JdkRegexpMethodPointcutMain {

	public static void main(String[] args) {
		// 注意:这里要传入new User(), 用作生成user对象的代理
		ProxyFactory factory = new ProxyFactory(new User());

		// 创建切面
		JdkRegexpMethodPointcut pointcut = new JdkRegexpMethodPointcut();
		//pointcut.setPattern("com.xiaoyuanzai.service.JdkRegexpMethodPointcutMain"); //它会拦截User类下所有age的方法（无法精确到 同方法名不同方法参数 的方法）
		//pointcut.setPattern(".*run.*");//.号匹配除"\r\n"之外的任何单个字符。*号代表零次或多次匹配前面的字符或子表达式  所以它拦截任意包下任意类的age方法
		pointcut.setPatterns(".*age.*", ".*money.*"); //可以配置多个正则表达式


		// 环绕通知
		DoMethodInterceptor advice = new DoMethodInterceptor();
		// 切面=切点+通知
		// 它还有个构造函数：DefaultPointcutAdvisor(Advice advice); 用的切面就是Pointcut.TRUE，所以如果你要指定切面，请使用自己指定的构造函数
		// Pointcut.TRUE：表示啥都返回true，也就是说这个切面作用于所有的方法上/所有的方法
		// addAdvice();方法最终内部都是被包装成一个 `DefaultPointcutAdvisor`，且使用的是Pointcut.TRUE切面，因此需要注意这些区别  相当于new DefaultPointcutAdvisor(Pointcut.TRUE,advice);
		Advisor advisor = new DefaultPointcutAdvisor(pointcut, advice);

		factory.addAdvisor(advisor);
		User u = (User) factory.getProxy();

		// 执行
		u.age();
		u.age(20);
		u.money();
		u.money("小源仔", 10000000);

	}


	static class User {

		public int age() {
			System.out.println("用户年龄...");
			return 0;
		}

		public int age(int i) {
			System.out.println("用户年龄.....(" + i + ")");
			return i;
		}

		public void money() {
			System.out.println("我没钱,好穷");
		}

		public void money(String name, int i) {
			System.out.println(name + " 有钱" + i);
		}

	}
}

class DoMethodInterceptor implements MethodInterceptor {

	@Nullable
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		System.out.println("...放行前拦截...");
		Object obj = invocation.proceed();
		System.out.println("...放行后拦截...");
		return obj;
	}
}