/*
 * Copyright 2002-2017 the original author or authors.
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

import org.aopalliance.aop.Advice;

/**
 * Base interface holding AOP <b>advice</b> (action to take at a joinpoint)
 * and a filter determining the applicability of the advice (such as
 * a pointcut). <i>This interface is not for use by Spring users, but to
 * allow for commonality in support for different types of advice.</i>
 * --
 * 基本接口包含 AOP advice（在连接点处执行的操作）和确定advice适用性的过滤器（例如切入点）。
 * 此接口不是供 Spring 用户使用的，而是为了允许支持不同类型的advice的通用性。
 * --
 * 包含AOP通知（在joinpoint处执行的操作）和确定通知适用性的过滤器（如切入点[PointCut]）的基本接口。
 * 这个接口不是供Spring用户使用的，而是为了支持不同类型的建议的通用性。
 *
 * <p>Spring AOP is based around <b>around advice</b> delivered via method
 * <b>interception</b>, compliant with the AOP Alliance interception API.
 * The Advisor interface allows support for different types of advice,
 * such as <b>before</b> and <b>after</b> advice, which need not be
 * implemented using interception.
 *
 * Spring AOP 基于通过方法拦截提供的advice，符合 AOP 联盟拦截 API。Advisor 接口允许支持不同类型的advice，例如之前和之后的建议，
 * 这些建议不需要使用拦截来实现。
 * --
 * 通知定义了需要做什么，切入点定义了在哪些类的哪些方法中执行通知，那么需要将他们2个组合起来才有效啊。
 * 顾问（Advisor）就是做这个事情的。
 * 在spring aop中，你可以将advisor理解为切面，切面中通常有2个关键信息：
 * 1. 需要增强的目标方法列表，这个通过切入点(Pointcut)来指定
 * 2. 需要在目标方法中增强的逻辑，这个通过(Advice)通知来指定
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface Advisor {

	/**
	 * Common placeholder for an empty {@code Advice} to be returned from
	 * {@link #getAdvice()} if no proper advice has been configured (yet).
	 * @since 5.0
	 */
	Advice EMPTY_ADVICE = new Advice() {};


	/**
	 * Return the advice part of this aspect. An advice may be an
	 * interceptor, a before advice, a throws advice, etc.
	 * -- 返回引用的通知
	 *
	 * @return the advice that should apply if the pointcut matches
	 * 返回引用的通知
	 * @see org.aopalliance.intercept.MethodInterceptor
	 * @see BeforeAdvice
	 * @see ThrowsAdvice
	 * @see AfterReturningAdvice
	 */
	Advice getAdvice();

	/**
	 * Return whether this advice is associated with a particular instance
	 * (for example, creating a mixin) or shared with all instances of
	 * the advised class obtained from the same Spring bean factory.
	 * <p><b>Note that this method is not currently used by the framework.</b>
	 * Typical Advisor implementations always return {@code true}.
	 * Use singleton/prototype bean definitions or appropriate programmatic
	 * proxy creation to ensure that Advisors have the correct lifecycle model.
	 * @return whether this advice is associated with a particular target instance
	 */
	boolean isPerInstance();

}
