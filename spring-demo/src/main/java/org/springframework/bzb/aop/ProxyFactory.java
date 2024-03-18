package org.springframework.bzb.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyFactory {
//	public static HttpApi getProxy(HttpApi target) {
//		return (HttpApi) Proxy.newProxyInstance(
//				target.getClass().getClassLoader(),
//				target.getClass().getInterfaces(),
//				new LogHandler(target));
//	}

	@SuppressWarnings("unchecked")
	public static <T> T getProxy(T target) {
		return (T) Proxy.newProxyInstance(
				target.getClass().getClassLoader(),
				target.getClass().getInterfaces(),
				new LogHandler<>(target));
	}

	private static class LogHandler<T> implements InvocationHandler {
		private T target;

		LogHandler(T target) {
			this.target = target;
		}

		// method底层的方法无参数时，args为空或者长度为0
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			// 扩展的功能
			System.out.println("http-statistic" + (String) args[0]);
			// 访问基础对象
			return method.invoke(target, args);
		}
	}

	public static void main(String[] args) {
		System.getProperties().put("sun.misc.ProxyGenerator.saveGeneratedFiles", "true");
		HttpApi  target = new RealModule();
		HttpApi proxy = ProxyFactory.getProxy(target);
		System.out.println(proxy.get("xx"));
//		OtherHttpApi proxy = ProxyFactory.getProxy<OtherHttpApi>(otherTarget);

	}
}
