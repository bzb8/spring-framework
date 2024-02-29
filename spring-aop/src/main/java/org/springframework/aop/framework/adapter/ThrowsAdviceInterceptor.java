/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework.adapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.AfterAdvice;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Interceptor to wrap an after-throwing advice.
 *
 * <p>The signatures on handler methods on the {@code ThrowsAdvice}
 * implementation method argument must be of the form:<br>
 *
 * {@code void afterThrowing([Method, args, target], ThrowableSubclass);}
 *
 * <p>Only the last argument is required.
 *
 * <p>Some examples of valid methods would be:
 *
 * <pre class="code">public void afterThrowing(Exception ex)</pre>
 * <pre class="code">public void afterThrowing(RemoteException)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, Exception ex)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, ServletException ex)</pre>
 *
 * <p>This is a framework class that need not be used directly by Spring users.
 * --
 * 这个类实现了MethodInterceptor接口，负责将ThrowsAdvice异常通知包装为MethodInterceptor类型，
 * 创建这个类型的对象的时候需要传递一个Object类型的参数，通常这个参数是ThrowsAdvice类型的，重点是invoke方法。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see MethodBeforeAdviceInterceptor
 * @see AfterReturningAdviceInterceptor
 */
public class ThrowsAdviceInterceptor implements MethodInterceptor, AfterAdvice {

	private static final String AFTER_THROWING = "afterThrowing";

	private static final Log logger = LogFactory.getLog(ThrowsAdviceInterceptor.class);


	private final Object throwsAdvice;

	/**
	 * Methods on throws advice, keyed by exception class.
	 * 异常类型->异常处理方法（afterThrowing名称的方法）
	 */
	private final Map<Class<?>, Method> exceptionHandlerMap = new HashMap<>();


	/**
	 * Create a new ThrowsAdviceInterceptor for the given ThrowsAdvice.
	 * @param throwsAdvice the advice object that defines the exception handler methods
	 * (usually a {@link org.springframework.aop.ThrowsAdvice} implementation)
	 *                    -- 定义异常处理程序方法（通常是 org.springframework.aop.ThrowsAdvice 实现）的 Advice 对象
	 */
	public ThrowsAdviceInterceptor(Object throwsAdvice) {
		Assert.notNull(throwsAdvice, "Advice must not be null");
		this.throwsAdvice = throwsAdvice;

		// 获取异常通知中定义的所有方法(public 修饰的字段；包含接口中 default 修饰的方法 (JDK1.8))
		Method[] methods = throwsAdvice.getClass().getMethods();
		for (Method method : methods) {
			// 方法名称为afterThrowing && 方法参数为1或者4
			if (method.getName().equals(AFTER_THROWING) &&
					(method.getParameterCount() == 1 || method.getParameterCount() == 4)) {
				// 获取方法的最后一个参数类型
				Class<?> throwableParam = method.getParameterTypes()[method.getParameterCount() - 1];
				// 判断方法参数类型是不是Throwable类型的
				if (Throwable.class.isAssignableFrom(throwableParam)) {
					// An exception handler to register...
					// 缓存异常处理方法到map中（异常类型->异常处理方法）
					this.exceptionHandlerMap.put(throwableParam, method);
					if (logger.isDebugEnabled()) {
						logger.debug("Found exception handler method on throws advice: " + method);
					}
				}
			}
		}
		// 如果exceptionHandlerMap，抛出异常，所以最少要有一个异常处理方法
		if (this.exceptionHandlerMap.isEmpty()) {
			throw new IllegalArgumentException(
					"At least one handler method must be found in class [" + throwsAdvice.getClass() + "]");
		}
	}


	/**
	 * Return the number of handler methods in this advice.
	 * 获取异常通知中自定义的处理异常方法的数量
	 */
	public int getHandlerMethodCount() {
		return this.exceptionHandlerMap.size();
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		try {
			// 调用通知链
			return mi.proceed();
		}
		catch (Throwable ex) {
			// 获取异常通知中自定义的处理异常的方法
			Method handlerMethod = getExceptionHandler(ex);
			if (handlerMethod != null) {
				// 调用异常处理方法
				invokeHandlerMethod(mi, ex, handlerMethod);
			}
			// 继续向外抛出异常
			throw ex;
		}
	}

	/**
	 * Determine the exception handle method for the given exception.
	 * 获取throwsAdvice中处理exception参数指定的异常的方法
	 *
	 * @param exception the exception thrown
	 * @return a handler for the given exception type, or {@code null} if none found
	 */
	@Nullable
	private Method getExceptionHandler(Throwable exception) {
		Class<?> exceptionClass = exception.getClass();
		if (logger.isTraceEnabled()) {
			logger.trace("Trying to find handler for exception of type [" + exceptionClass.getName() + "]");
		}
		Method handler = this.exceptionHandlerMap.get(exceptionClass);
		// 来一个循环，查询处理方法，循环条件：方法为空 && 异常类型!=Throwable
		while (handler == null && exceptionClass != Throwable.class) {
			// 获取异常的父类型
			exceptionClass = exceptionClass.getSuperclass();
			// 从缓存中查找异常对应的处理方法
			handler = this.exceptionHandlerMap.get(exceptionClass);
		}

		if (handler != null && logger.isTraceEnabled()) {
			logger.trace("Found handler for exception of type [" + exceptionClass.getName() + "]: " + handler);
		}
		return handler;
	}

	/**
	 *
	 * @param mi
	 * @param ex
	 * @param method
	 * @throws Throwable
	 */
	private void invokeHandlerMethod(MethodInvocation mi, Throwable ex, Method method) throws Throwable {
		Object[] handlerArgs;
		if (method.getParameterCount() == 1) {
			// 若只有1个参数，参数为：异常对象
			handlerArgs = new Object[] {ex};
		}
		else {
			// 4个参数（方法、方法请求参数、目标对象、异常对象）
			handlerArgs = new Object[] {mi.getMethod(), mi.getArguments(), mi.getThis(), ex};
		}
		try {
			// 通过反射调用异常通知中的方法
			method.invoke(this.throwsAdvice, handlerArgs);
		}
		catch (InvocationTargetException targetEx) {
			throw targetEx.getTargetException();
		}
	}

}
