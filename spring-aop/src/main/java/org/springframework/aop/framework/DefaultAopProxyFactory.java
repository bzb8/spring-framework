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

import java.io.Serializable;
import java.lang.reflect.Proxy;

import org.springframework.aop.SpringProxy;
import org.springframework.core.NativeDetector;
import org.springframework.util.ClassUtils;

/**
 * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
 * or a JDK dynamic proxy.
 * 默认 AopProxyFactory 实现，创建 CGLIB 代理或 JDK 动态代理。
 *
 * <p>Creates a CGLIB proxy if one the following is true for a given
 * {@link AdvisedSupport} instance:
 * 如果给定 AdvisedSupport 实例满足以下条件之一，则创建 CGLIB 代理：
 * <ul>
 * <li>the {@code optimize} flag is set -- optimize标志已设置
 * <li>the {@code proxyTargetClass} flag is set -- proxyTargetClass标志已设置
 * <li>no proxy interfaces have been specified -- 未指定代理接口
 * </ul>
 *
 * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
 * or specify one or more interfaces to use a JDK dynamic proxy.
 * 通常，指定 proxyTargetClass 强制执行 CGLIB 代理，或指定一个或多个接口以使用 JDK 动态代理。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 12.03.2004
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 */
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	private static final long serialVersionUID = 7930414337282325166L;


	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		// optimize==true || proxyTargetClass 为true || 配置中没有需要代理的接口
		if (!NativeDetector.inNativeImage() &&
				// optimize==true || proxyTargetClass 为true || 配置中没有需要代理的接口
				(config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config))) {
			// 获取需要被代理的类
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			// 如果被代理的类为接口 或者 被代理的类是jdk动态代理创建代理类，则采用JdkDynamicAopProxy的方式，否则采用cglib代理的方式
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)) {
				// 采用jdk动态代理的方式
				return new JdkDynamicAopProxy(config);
			}
			// 采用cglib代理的方式
			return new ObjenesisCglibAopProxy(config);
		}
		else {
			// 采用jdk动态代理的方式
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 * --
	 * 确定所提供的AdvisedSupport是否只指定了SpringProxy接口(或者根本没有指定代理接口)
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
