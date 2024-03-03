/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aop.framework;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.core.DecoratingProxy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility methods for AOP proxy factories.
 * Mainly for internal use within the AOP framework.
 *
 * AOP 代理工厂的实用方法。主要供 AOP 框架内的内部使用。
 *
 * <p>See {@link org.springframework.aop.support.AopUtils} for a collection of
 * generic AOP utility methods which do not depend on AOP framework internals.
 *
 * 请参阅 {@link org.springframework.aop.support.AopUtils} 获取不依赖于 AOP 框架内部的通用 AOP 实用程序方法的集合。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see org.springframework.aop.support.AopUtils
 */
public abstract class AopProxyUtils {

	// JDK 17 Class.isSealed() method available?
	// JDK 17 Class.isSealed() 方法可用吗？
	@Nullable
	private static final Method isSealedMethod = ClassUtils.getMethodIfAvailable(Class.class, "isSealed");


	/**
	 * Obtain the singleton target object behind the given proxy, if any.
	 * --
	 * 获取给定代理后面的单实目标对象（如果有）。
	 *
	 * @param candidate the (potential) proxy to check 要检查的（潜在）代理
	 * @return the singleton target object managed in a {@link SingletonTargetSource},
	 * or {@code null} in any other case (not a proxy, not an existing singleton target)
	 *
	 * 在 {@link SingletonTargetSource} 中管理的单例目标对象，或在任何其他情况下 {@code null}（不是代理，不是现有的单例目标）
	 *
	 * @since 4.3.8
	 * @see Advised#getTargetSource()
	 * @see SingletonTargetSource#getTarget()
	 */
	@Nullable
	public static Object getSingletonTarget(Object candidate) {
		if (candidate instanceof Advised) {
			TargetSource targetSource = ((Advised) candidate).getTargetSource();
			if (targetSource instanceof SingletonTargetSource) {
				return ((SingletonTargetSource) targetSource).getTarget();
			}
		}
		return null;
	}

	/**
	 * Determine the ultimate target class of the given bean instance, traversing
	 * not only a top-level proxy but any number of nested proxies as well &mdash;
	 * as long as possible without side effects, that is, just for singleton targets.
	 *
	 * 确定给定 bean 实例的最终目标类，不仅遍历顶级代理，还遍历任意数量的嵌套代理 — 尽可能长且没有副作用，即仅针对单例目标。
	 *
	 * @param candidate the instance to check (might be an AOP proxy)
	 * @return the ultimate target class (or the plain class of the given
	 * object as fallback; never {@code null})
	 * @see org.springframework.aop.TargetClassAware#getTargetClass()
	 * @see Advised#getTargetSource()
	 */
	public static Class<?> ultimateTargetClass(Object candidate) {
		Assert.notNull(candidate, "Candidate object must not be null");
		Object current = candidate;
		Class<?> result = null;
		while (current instanceof TargetClassAware) {
			result = ((TargetClassAware) current).getTargetClass();
			current = getSingletonTarget(current);
		}
		if (result == null) {
			result = (AopUtils.isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
		}
		return result;
	}

	/**
	 * Determine the complete set of interfaces to proxy for the given AOP configuration.
	 * <p>This will always add the {@link Advised} interface unless the AdvisedSupport's
	 * {@link AdvisedSupport#setOpaque "opaque"} flag is on. Always adds the
	 * {@link org.springframework.aop.SpringProxy} marker interface.
	 * @param advised the proxy config
	 * @return the complete set of interfaces to proxy
	 * @see SpringProxy
	 * @see Advised
	 */
	public static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised) {
		return completeProxiedInterfaces(advised, false);
	}

	/**
	 * Determine the complete set of interfaces to proxy for the given AOP configuration.
	 * <p>This will always add the {@link Advised} interface unless the AdvisedSupport's
	 * {@link AdvisedSupport#setOpaque "opaque"} flag is on. Always adds the
	 * {@link org.springframework.aop.SpringProxy} marker interface.
	 * --
	 * 确定给定 AOP 配置的完整接口集。
	 * 这将始终添加 {@link Advised} 接口，除非 AdvisedSupport 的 {@link AdvisedSupport#setOpaque "opaque"} 标志已打开。
	 * 始终添加 {@link org.springframework.aop.SpringProxy} 标记接口。
	 * --
	 * 默认情况下会得到下面的一个列表
	 * [开发者硬编码指定的需要被代理的接口列表,SpringProxy,Advised,DecoratingProxy]
	 * 最终创建出来的代理对象，默认会实现上面列的所有接口，后面3个接口是aop中自动给我们加上的。
	 *
	 * @param advised the proxy config
	 * @param decoratingProxy whether to expose the {@link DecoratingProxy} interface 是否公开 {@link DecoratingProxy} 接口
	 * @return the complete set of interfaces to proxy
	 * @since 4.3
	 * @see SpringProxy
	 * @see Advised
	 * @see DecoratingProxy
	 */
	static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised, boolean decoratingProxy) {
		// 获取代理配置中需要被代理的接口
		Class<?>[] specifiedInterfaces = advised.getProxiedInterfaces();
		// 需要被代理的接口数量为0
		if (specifiedInterfaces.length == 0) {
			// No user-specified interfaces: check whether target class is an interface.
			// 没有用户指定的接口：检查目标类是否是接口。
			// 获取需要被代理的目标类型
			Class<?> targetClass = advised.getTargetClass();
			if (targetClass != null) {
				// 目标类型为接口
				if (targetClass.isInterface()) {
					// 将其添加到需要代理的接口中
					advised.setInterfaces(targetClass);
				}
				// 目标类型为jdk动态代理创建的代理对象
				else if (Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)) {
					// 获取目标类型上的所有接口，将其添加到需要被代理的接口中
					advised.setInterfaces(targetClass.getInterfaces());
				}
				// 再次获取代理配置中需要被代理的接口
				specifiedInterfaces = advised.getProxiedInterfaces();
			}
		}

		List<Class<?>> proxiedInterfaces = new ArrayList<>(specifiedInterfaces.length + 3);
		for (Class<?> ifc : specifiedInterfaces) {
			// Only non-sealed interfaces are actually eligible for JDK proxying (on JDK 17)
			// 只有非密封接口实际上才有资格进行 JDK 代理（在 JDK 17 上）
			if (isSealedMethod == null || Boolean.FALSE.equals(ReflectionUtils.invokeMethod(isSealedMethod, ifc))) {
				proxiedInterfaces.add(ifc);
			}
		}

		// 添加SpringProxy接口
		if (!advised.isInterfaceProxied(SpringProxy.class)) {
			proxiedInterfaces.add(SpringProxy.class);
		}
		// 添加Advised接口
		if (!advised.isOpaque() && !advised.isInterfaceProxied(Advised.class)) {
			proxiedInterfaces.add(Advised.class);
		}
		// 添加DecoratingProxy接口
		if (decoratingProxy && !advised.isInterfaceProxied(DecoratingProxy.class)) {
			proxiedInterfaces.add(DecoratingProxy.class);
		}

		return ClassUtils.toClassArray(proxiedInterfaces);
	}

	/**
	 * Extract the user-specified interfaces that the given proxy implements,
	 * i.e. all non-Advised interfaces that the proxy implements.
	 * @param proxy the proxy to analyze (usually a JDK dynamic proxy)
	 * @return all user-specified interfaces that the proxy implements,
	 * in the original order (never {@code null} or empty)
	 * @see Advised
	 */
	public static Class<?>[] proxiedUserInterfaces(Object proxy) {
		Class<?>[] proxyInterfaces = proxy.getClass().getInterfaces();
		int nonUserIfcCount = 0;
		if (proxy instanceof SpringProxy) {
			nonUserIfcCount++;
		}
		if (proxy instanceof Advised) {
			nonUserIfcCount++;
		}
		if (proxy instanceof DecoratingProxy) {
			nonUserIfcCount++;
		}
		Class<?>[] userInterfaces = Arrays.copyOf(proxyInterfaces, proxyInterfaces.length - nonUserIfcCount);
		Assert.notEmpty(userInterfaces, "JDK proxy must implement one or more interfaces");
		return userInterfaces;
	}

	/**
	 * Check equality of the proxies behind the given AdvisedSupport objects.
	 * Not the same as equality of the AdvisedSupport objects:
	 * rather, equality of interfaces, advisors and target sources.
	 *
	 * 检查给定 AdvisedSupport 对象背后的代理是否相等。与 AdvisedSupport 对象的平等不同：相反，接口、advisors和目标源的平等。
	 */
	public static boolean equalsInProxy(AdvisedSupport a, AdvisedSupport b) {
		return (a == b ||
				(equalsProxiedInterfaces(a, b) && equalsAdvisors(a, b) && a.getTargetSource().equals(b.getTargetSource())));
	}

	/**
	 * Check equality of the proxied interfaces behind the given AdvisedSupport objects.
	 */
	public static boolean equalsProxiedInterfaces(AdvisedSupport a, AdvisedSupport b) {
		return Arrays.equals(a.getProxiedInterfaces(), b.getProxiedInterfaces());
	}

	/**
	 * Check equality of the advisors behind the given AdvisedSupport objects.
	 */
	public static boolean equalsAdvisors(AdvisedSupport a, AdvisedSupport b) {
		return a.getAdvisorCount() == b.getAdvisorCount() && Arrays.equals(a.getAdvisors(), b.getAdvisors());
	}


	/**
	 * Adapt the given arguments to the target signature in the given method,
	 * if necessary: in particular, if a given vararg argument array does not
	 * match the array type of the declared vararg parameter in the method.
	 * 如有必要，将给定的参数调整为给定方法中的目标签名：特别是，如果给定的 vararg 参数数组与方法中声明的 vararg 参数的数组类型不匹配
	 *
	 * @param method the target method -- 目标方法
	 * @param arguments the given arguments -- 给定的原始参数
	 * @return a cloned argument array, or the original if no adaptation is needed
	 * 克隆的参数数组，如果不需要调整，则为原始参数数组
	 * @since 4.2.3
	 */
	static Object[] adaptArgumentsIfNecessary(Method method, @Nullable Object[] arguments) {
		if (ObjectUtils.isEmpty(arguments)) {
			return new Object[0];
		}
		// 方法是具有可变参数类型的方法
		if (method.isVarArgs()) {
			// 当前调用方法和给定的参数数量相等
			if (method.getParameterCount() == arguments.length) {
				// 方法的参数类型列表
				Class<?>[] paramTypes = method.getParameterTypes();
				// 可变参数的索引下标，最后一个
				int varargIndex = paramTypes.length - 1;
				// 获取可变参数的类型
				Class<?> varargType = paramTypes[varargIndex];
				// 可变参数是数组类型的
				if (varargType.isArray()) {
					// 可变参数的值
					Object varargArray = arguments[varargIndex];
					// 给定的可变参数的值是Object[] && 给定的可变参数的值不是当前方法的可变参数类型的实例
					if (varargArray instanceof Object[] && !varargType.isInstance(varargArray)) {
						// 新创建一个参数的数组对象
						Object[] newArguments = new Object[arguments.length];
						// 复制原来的参数值 - 1
						System.arraycopy(arguments, 0, newArguments, 0, varargIndex);
						Class<?> targetElementType = varargType.getComponentType();
						int varargLength = Array.getLength(varargArray);
						Object newVarargArray = Array.newInstance(targetElementType, varargLength);
						// 将当前可变参数的值复制给新的可变参数
						System.arraycopy(varargArray, 0, newVarargArray, 0, varargLength);
						// 给方法参数最后一个可变参数复制
						newArguments[varargIndex] = newVarargArray;
						return newArguments;
					}
				}
			}
		}
		return arguments;
	}

}
