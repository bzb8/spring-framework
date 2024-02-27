/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;

/**
 * Extension of the AOP Alliance {@link org.aopalliance.intercept.MethodInvocation}
 * interface, allowing access to the proxy that the method invocation was made through.
 *
 * <p>Useful to be able to substitute return values with the proxy,
 * if necessary, for example if the invocation target returned itself.
 * --
 * AOP 联盟 MethodInvocation 接口的扩展，允许访问通过其进行方法调用的代理。
 * 如有必要，能够将返回值替换为代理非常有用，例如，如果调用目标返回自身
 *
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @since 1.1.3
 * @see org.springframework.aop.framework.ReflectiveMethodInvocation
 * @see org.springframework.aop.support.DelegatingIntroductionInterceptor
 */
public interface ProxyMethodInvocation extends MethodInvocation {

	/**
	 * Return the proxy that this method invocation was made through.
	 * 返回通过此方法调用的代理。
	 *
	 * @return the original proxy object
	 */
	Object getProxy();

	/**
	 * Create a clone of this object. If cloning is done before {@code proceed()}
	 * is invoked on this object, {@code proceed()} can be invoked once per clone
	 * to invoke the joinpoint (and the rest of the advice chain) more than once.
	 * --
	 * 创建此对象的克隆。如果在此对象上调用之前 proceed() 已完成克隆， proceed() 则每个克隆可以调用一次，以多次调用连接点（以及建议链的其余部分）。
	 *
	 * @return an invocable clone of this invocation.
	 * {@code proceed()} can be called once per clone.
	 * 此调用的可调用克隆。 proceed() 每个克隆可以调用一次。
	 */
	MethodInvocation invocableClone();

	/**
	 * Create a clone of this object. If cloning is done before {@code proceed()}
	 * is invoked on this object, {@code proceed()} can be invoked once per clone
	 * to invoke the joinpoint (and the rest of the advice chain) more than once.
	 * @param arguments the arguments that the cloned invocation is supposed to use,
	 * overriding the original arguments -- 克隆调用应该使用的参数，覆盖原始参数
	 * @return an invocable clone of this invocation.
	 * {@code proceed()} can be called once per clone.
	 */
	MethodInvocation invocableClone(Object... arguments);

	/**
	 * Set the arguments to be used on subsequent invocations in the any advice
	 * in this chain.
	 * 设置要用于此链中任何通知的后续调用的参数。
	 *
	 * @param arguments the argument array
	 */
	void setArguments(Object... arguments);

	/**
	 * Add the specified user attribute with the given value to this invocation.
	 * <p>Such attributes are not used within the AOP framework itself. They are
	 * just kept as part of the invocation object, for use in special interceptors.
	 * --
	 * 添加一些扩展用户属性，这些属性不在AOP框架内使用。它们只是作为调用对象的一部分保留，用于特殊的拦截器。
	 *
	 * @param key the name of the attribute
	 * @param value the value of the attribute, or {@code null} to reset it
	 */
	void setUserAttribute(String key, @Nullable Object value);

	/**
	 * Return the value of the specified user attribute.
	 * --
	 * 根据key获取对应的用户属性
	 *
	 * @param key the name of the attribute
	 * @return the value of the attribute, or {@code null} if not set
	 * @see #setUserAttribute
	 */
	@Nullable
	Object getUserAttribute(String key);

}
