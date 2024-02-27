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

package org.springframework.aop.framework;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;

/**
 * Interface to be implemented by classes that hold the configuration
 * of a factory of AOP proxies. This configuration includes the
 * Interceptors and other advice, Advisors, and the proxied interfaces.
 *
 * 由保存 AOP 代理工厂配置的类实现的接口。此配置包括拦截器和其他advice、Advisors和代理接口。
 *
 * <p>Any AOP proxy obtained from Spring can be cast to this interface to
 * allow manipulation of its AOP advice.
 *
 * 从 Spring 获取的任何 AOP 代理都可以强制转换为此接口，以允许操作其 AOP 建议。
 * --
 * 这个接口中定义了操作Aop代理配置的各种方法（比如指定被代理的目标对象、添加通知、添加顾问等等）。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13.03.2003
 * @see org.springframework.aop.framework.AdvisedSupport
 */
public interface Advised extends TargetClassAware {

	/**
	 * Return whether the Advised configuration is frozen,
	 * in which case no advice changes can be made.
	 * 返回配置是否已冻结，被冻结之后，无法修改已创建好的代理对象中的通知
	 */
	boolean isFrozen();

	/**
	 * Are we proxying the full target class instead of specified interfaces?
	 * 是否对目标类直接创建代理，而不是对接口创建代理，通俗点讲：如果是通过cglib创建代理，此方法返回true，否则返回false
	 */
	boolean isProxyTargetClass();

	/**
	 * Return the interfaces proxied by the AOP proxy.
	 * <p>Will not include the target class, which may also be proxied.
	 * 获取配置中需要代理的接口列表
	 */
	Class<?>[] getProxiedInterfaces();

	/**
	 * Determine whether the given interface is proxied.
	 * 确定给定接口是否被代理。
	 * @param intf the interface to check
	 */
	boolean isInterfaceProxied(Class<?> intf);

	/**
	 * Change the {@code TargetSource} used by this {@code Advised} object.
	 * <p>Only works if the configuration isn't {@linkplain #isFrozen frozen}.
	 * 设置被代理的目标源，创建代理的时候，通常需要传入被代理的对象，最终被代理的对象会被包装为TargetSource类型的
	 *
	 * @param targetSource new TargetSource to use
	 */
	void setTargetSource(TargetSource targetSource);

	/**
	 * Return the {@code TargetSource} used by this {@code Advised} object.
	 * 返回被代理的目标源
	 */
	TargetSource getTargetSource();

	/**
	 * Set whether the proxy should be exposed by the AOP framework as a
	 * {@link ThreadLocal} for retrieval via the {@link AopContext} class.
	 * <p>It can be necessary to expose the proxy if an advised object needs
	 * to invoke a method on itself with advice applied. Otherwise, if an
	 * advised object invokes a method on {@code this}, no advice will be applied.
	 * <p>Default is {@code false}, for optimal performance.
	 * 设置是否需要将代理暴露在ThreadLocal中，这样可以在线程中获取到被代理对象，这个配置挺有用的，稍后会举例说明使用场景
	 */
	void setExposeProxy(boolean exposeProxy);

	/**
	 * Return whether the factory should expose the proxy as a {@link ThreadLocal}.
	 * <p>It can be necessary to expose the proxy if an advised object needs
	 * to invoke a method on itself with advice applied. Otherwise, if an
	 * advised object invokes a method on {@code this}, no advice will be applied.
	 * <p>Getting the proxy is analogous to an EJB calling {@code getEJBObject()}.
	 * 返回exposeProxy
	 * @see AopContext
	 */
	boolean isExposeProxy();

	/**
	 * Set whether this proxy configuration is pre-filtered so that it only
	 * contains applicable advisors (matching this proxy's target class).
	 * <p>Default is "false". Set this to "true" if the advisors have been
	 * pre-filtered already, meaning that the ClassFilter check can be skipped
	 * when building the actual advisor chain for proxy invocations.
	 * @see org.springframework.aop.ClassFilter
	 *
	 * 设置此代理配置是否经过预筛选，以便它只包含适用的顾问(匹配此代理的目标类)。
	 * 默认设置是“假”。如果已经对advisor进行了预先筛选，则将其设置为“true”
	 * 这意味着在为代理调用构建实际的advisor链时可以跳过ClassFilter检查。
	 */
	void setPreFiltered(boolean preFiltered);

	/**
	 * Return whether this proxy configuration is pre-filtered so that it only
	 * contains applicable advisors (matching this proxy's target class).
	 */
	boolean isPreFiltered();

	/**
	 * Return the advisors applying to this proxy.
	 * @return a list of Advisors applying to this proxy (never {@code null})
	 * 返回代理配置中所有的Advisor列表
	 */
	Advisor[] getAdvisors();

	/**
	 * Return the number of advisors applying to this proxy.
	 * <p>The default implementation delegates to {@code getAdvisors().length}.
	 * @since 5.3.1
	 */
	default int getAdvisorCount() {
		return getAdvisors().length;
	}

	/**
	 * Add an advisor at the end of the advisor chain.
	 * <p>The Advisor may be an {@link org.springframework.aop.IntroductionAdvisor},
	 * in which new interfaces will be available when a proxy is next obtained
	 * from the relevant factory.
	 * 添加一个Advisor
	 *
	 * @param advisor the advisor to add to the end of the chain
	 * @throws AopConfigException in case of invalid advice
	 */
	void addAdvisor(Advisor advisor) throws AopConfigException;

	/**
	 * Add an Advisor at the specified position in the chain.
	 * 指定的位置添加一个Advisor
	 *
	 * @param advisor the advisor to add at the specified position in the chain
	 * @param pos position in chain (0 is head). Must be valid.
	 * @throws AopConfigException in case of invalid advice
	 */
	void addAdvisor(int pos, Advisor advisor) throws AopConfigException;

	/**
	 * Remove the given advisor.
	 * 移除一个Advisor
	 * @param advisor the advisor to remove
	 * @return {@code true} if the advisor was removed; {@code false}
	 * if the advisor was not found and hence could not be removed
	 */
	boolean removeAdvisor(Advisor advisor);

	/**
	 * Remove the advisor at the given index.
	 * 移除指定位置的Advisor
	 *
	 * @param index the index of advisor to remove
	 * @throws AopConfigException if the index is invalid
	 */
	void removeAdvisor(int index) throws AopConfigException;

	/**
	 * Return the index (from 0) of the given advisor,
	 * or -1 if no such advisor applies to this proxy.
	 * <p>The return value of this method can be used to index into the advisors array.
	 * 查找某个Advisor的位置
	 *
	 * @param advisor the advisor to search for
	 * @return index from 0 of this advisor, or -1 if there's no such advisor
	 */
	int indexOf(Advisor advisor);

	/**
	 * Replace the given advisor.
	 * <p><b>Note:</b> If the advisor is an {@link org.springframework.aop.IntroductionAdvisor}
	 * and the replacement is not or implements different interfaces, the proxy will need
	 * to be re-obtained or the old interfaces won't be supported and the new interface
	 * won't be implemented.
	 * 对advisor列表中的a替换为b
	 *
	 * @param a the advisor to replace
	 * @param b the advisor to replace it with
	 * @return whether it was replaced. If the advisor wasn't found in the
	 * list of advisors, this method returns {@code false} and does nothing.
	 * @throws AopConfigException in case of invalid advice
	 */
	boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException;

	/**
	 * Add the given AOP Alliance advice to the tail of the advice (interceptor) chain.
	 * <p>This will be wrapped in a DefaultPointcutAdvisor with a pointcut that always
	 * applies, and returned from the {@code getAdvisors()} method in this wrapped form.
	 * <p>Note that the given advice will apply to all invocations on the proxy,
	 * even to the {@code toString()} method! Use appropriate advice implementations
	 * or specify appropriate pointcuts to apply to a narrower set of methods.
	 * 添加一个通知
	 *
	 * @param advice the advice to add to the tail of the chain
	 * @throws AopConfigException in case of invalid advice
	 * @see #addAdvice(int, Advice)
	 * @see org.springframework.aop.support.DefaultPointcutAdvisor
	 */
	void addAdvice(Advice advice) throws AopConfigException;

	/**
	 * Add the given AOP Alliance Advice at the specified position in the advice chain.
	 * <p>This will be wrapped in a {@link org.springframework.aop.support.DefaultPointcutAdvisor}
	 * with a pointcut that always applies, and returned from the {@link #getAdvisors()}
	 * method in this wrapped form.
	 * <p>Note: The given advice will apply to all invocations on the proxy,
	 * even to the {@code toString()} method! Use appropriate advice implementations
	 * or specify appropriate pointcuts to apply to a narrower set of methods.
	 * @param pos index from 0 (head)
	 * @param advice the advice to add at the specified position in the advice chain
	 * @throws AopConfigException in case of invalid advice
	 */
	void addAdvice(int pos, Advice advice) throws AopConfigException;

	/**
	 * Remove the Advisor containing the given advice.
	 * @param advice the advice to remove
	 * @return {@code true} of the advice was found and removed;
	 * {@code false} if there was no such advice
	 */
	boolean removeAdvice(Advice advice);

	/**
	 * Return the index (from 0) of the given AOP Alliance Advice,
	 * or -1 if no such advice is an advice for this proxy.
	 * <p>The return value of this method can be used to index into
	 * the advisors array.
	 * @param advice the AOP Alliance advice to search for
	 * @return index from 0 of this advice, or -1 if there's no such advice
	 */
	int indexOf(Advice advice);

	/**
	 * As {@code toString()} will normally be delegated to the target,
	 * this returns the equivalent for the AOP proxy.
	 * @return a string description of the proxy configuration
	 * 将代理配置转换为字符串，这个方便排错和调试使用的
	 */
	String toProxyConfigString();

}
