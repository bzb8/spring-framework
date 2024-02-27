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

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.BeforeAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Interceptor to wrap a {@link MethodBeforeAdvice}.
 * <p>Used internally by the AOP framework; application developers should not
 * need to use this class directly.
 * --
 * 这个类实现了MethodInterceptor接口，负责将MethodBeforeAdvice方法前置通知包装为MethodInterceptor类型，
 * 创建这个类型的对象的时候需要传递一个MethodBeforeAdvice类型的参数，重点是invoke方法
 *
 * @author Rod Johnson
 * @see AfterReturningAdviceInterceptor
 * @see ThrowsAdviceInterceptor
 */
@SuppressWarnings("serial")
public class MethodBeforeAdviceInterceptor implements MethodInterceptor, BeforeAdvice, Serializable {

	private final MethodBeforeAdvice advice;


	/**
	 * Create a new MethodBeforeAdviceInterceptor for the given advice.
	 * @param advice the MethodBeforeAdvice to wrap
	 */
	public MethodBeforeAdviceInterceptor(MethodBeforeAdvice advice) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		// 负责调用前置通知的方法
 		this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());
		// 继续执行方法调用链
		return mi.proceed();
	}

}
