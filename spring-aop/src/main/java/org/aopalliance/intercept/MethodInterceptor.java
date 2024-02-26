/*
 * Copyright 2002-2018 the original author or authors.
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

package org.aopalliance.intercept;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Intercepts calls on an interface on its way to the target. These
 * are nested "on top" of the target.
 * --
 * 拦截目标的接口上的调用。它们嵌套在目标的“顶部”。
 *
 * <p>The user should implement the {@link #invoke(MethodInvocation)}
 * method to modify the original behavior. E.g. the following class
 * implements a tracing interceptor (traces all the calls on the
 * intercepted method(s)):
 *
 * 用户应该实现 {@link #invoke(MethodInvocation)} 方法来修改原始行为。例如。下面的类实现了一个跟踪拦截器（跟踪被拦截方法的所有调用）：
 *
 * <pre class=code>
 * class TracingInterceptor implements MethodInterceptor {
 *   Object invoke(MethodInvocation i) throws Throwable {
 *     System.out.println("method "+i.getMethod()+" is called on "+
 *                        i.getThis()+" with args "+i.getArguments());
 *     Object ret=i.proceed();
 *     System.out.println("method "+i.getMethod()+" returns "+ret);
 *     return ret;
 *   }
 * }
 * </pre>
 * --
 * 方法拦截器，所有的通知均需要转换为MethodInterceptor类型的，最终多个MethodInterceptor组成一个方法拦截器连。
 *
 * @author Rod Johnson
 */
@FunctionalInterface
public interface MethodInterceptor extends Interceptor {

	/**
	 * Implement this method to perform extra treatments before and
	 * after the invocation. Polite implementations would certainly
	 * like to invoke {@link Joinpoint#proceed()}.
	 * --
	 * 实现此方法以在调用之前和之后执行额外的处理。礼貌的实现肯定会调用 {@link Joinpoint#proceed()}.
	 * 拦截目标方法的执行，可以在这个方法内部实现需要增强的逻辑，以及主动调用目标方法。
	 *
	 * @param invocation the method invocation joinpoint 方法调用连接点
	 * @return the result of the call to {@link Joinpoint#proceed()};
	 * might be intercepted by the interceptor
	 * @throws Throwable if the interceptors or the target object
	 * throws an exception
	 */
	@Nullable
	Object invoke(@Nonnull MethodInvocation invocation) throws Throwable;

}
