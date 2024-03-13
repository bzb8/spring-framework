package org.springframework.aop;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DecoratingProxy;

import java.lang.reflect.Proxy;
import java.util.Arrays;

public class ProxyFactoryMain {

	public static void main(String[] args) {
		ProxyFactory proxyFactory = new ProxyFactory(new Hello());
		proxyFactory.addAdvice((MethodBeforeAdvice) (method, args1, target) ->
				System.out.println("你被拦截了：方法名为：" + method.getName() + " 参数为--" + Arrays.asList(args1)));
		HelloInterface hello = (HelloInterface) proxyFactory.getProxy();
		hello.hello();

		System.out.println(proxyFactory.getTargetClass()); //class com.xiaoyuanzai.service.InvocationHandler.Hello
		System.out.println(proxyFactory.getTargetSource()); //SingletonTargetSource for target object [com.xiaoyuanzai.service.InvocationHandler.Hello@5d099f62]
		System.out.println(proxyFactory.getAdvisorCount()); //1
		System.out.println(Arrays.asList(proxyFactory.getAdvisors())); //[org.springframework.aop.support.DefaultPointcutAdvisor: pointcut [Pointcut.TRUE];
		// advice [com.xiaoyuanzai.service.InvocationHandler.ProxyFactoryMain$$Lambda$1/1199823423@37f8bb67]]

		System.out.println(".......................................");
		//代理类的接口HelloInterface、 SpringProxy、Advised、DecoratingProxy
		System.out.println(Arrays.asList(hello.getClass().getInterfaces())); //[interface com.xiaoyuanzai.service.InvocationHandler.HelloInterface, interface org.springframework.aop.SpringProxy, interface org.springframework.aop.framework.Advised, interface org.springframework.core.DecoratingProxy]
		System.out.println(hello instanceof Proxy); //true
		System.out.println(hello instanceof SpringProxy); //true org.springframework.aop.SpringProxy这个是marker接口，空的，不用管，只是做个标记，框架会用到
		System.out.println(hello instanceof DecoratingProxy); //true
		System.out.println(hello instanceof Advised); //true
		System.out.println(hello.getClass());//class com.xiaoyuanzai.service.InvocationHandler.$Proxy0
		System.out.println(Proxy.isProxyClass(hello.getClass()));//true
		System.out.println(AopUtils.isCglibProxy(hello));//false

		System.out.println(".......................................");
		//测试Advised接口、DecoratingProxy的内容
		Advised advised = (Advised) hello;
		System.out.println(advised.isExposeProxy()); //false
		System.out.println(advised.isFrozen()); //false

		System.out.println(".......................................");
		// Object的方法 ==== 所有的Object方法都不会被AOP代理（除了toString()） 这点需要注意
		System.out.println(hello.equals(new Object()));//false
		System.out.println(hello.hashCode());
		System.out.println(hello.getClass());

		//其余方法都没被拦截  只有toString()被拦截了
		//你被拦截了：方法名为：toString 参数为--[]
		//com.xiaoyuanzai.service.InvocationHandler.Hello@5d099f62
		System.out.println(hello.toString());


	}
}

interface HelloInterface {

	void hello();
}

class Hello implements HelloInterface {
	@Override
	public void hello() {
		System.out.println("Hello world");
	}
}