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

package org.springframework.aop.aspectj.annotation;

import java.lang.reflect.Method;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.lang.Nullable;

/**
 * Interface for factories that can create Spring AOP Advisors from classes
 * annotated with AspectJ annotation syntax.
 *
 * 工厂的接口，可以从使用 AspectJ 注解语法注解的类创建 Spring AOP Advisors。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see AspectMetadata
 * @see org.aspectj.lang.reflect.AjTypeSystem
 */
public interface AspectJAdvisorFactory {

	/**
	 * Determine whether the given class is an aspect, as reported
	 * by AspectJ's {@link org.aspectj.lang.reflect.AjTypeSystem}.
	 * <p>Will simply return {@code false} if the supposed aspect is
	 * invalid (such as an extension of a concrete aspect class).
	 * Will return true for some aspects that Spring AOP cannot process,
	 * such as those with unsupported instantiation models.
	 * Use the {@link #validate} method to handle these cases if necessary.
	 *
	 * 确定给定的类是否为 aspect，如 AspectJ 的 {@link org.aspectj.lang.reflect.AjTypeSystem} 所报告的那样。
	 * 如果假定的aspect无效（例如具体方面类的扩展），则将简单地返回 {@code false}。对于 Spring AOP 无法处理的某些方面，
	 * 例如那些具有不受支持的实例化模型的方面，将返回 true。如有必要，请使用 {@link #validate} 方法处理这些情况
	 *
	 * @param clazz the supposed annotation-style AspectJ class
	 * @return whether this class is recognized by AspectJ as an aspect class
	 */
	boolean isAspect(Class<?> clazz);

	/**
	 * Is the given class a valid AspectJ aspect class?
	 * @param aspectClass the supposed AspectJ annotation-style class to validate
	 * @throws AopConfigException if the class is an invalid aspect
	 * (which can never be legal)
	 * @throws NotAnAtAspectException if the class is not an aspect at all
	 * (which may or may not be legal, depending on the context)
	 */
	void validate(Class<?> aspectClass) throws AopConfigException;

	/**
	 * Build Spring AOP Advisors for all annotated At-AspectJ methods
	 * on the specified aspect instance.
	 *
	 * 在指定的 aspect 实例上为所有带注解的 At-AspectJ 方法构建 Spring AOP Advisors。
	 *
	 * @param aspectInstanceFactory the aspect instance factory
	 * (not the aspect instance itself in order to avoid eager instantiation) Aspect实例工厂（不是Aspect实例本身，以避免预先实例化）
	 * @return a list of advisors for this class
	 */
	List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory);

	/**
	 * Build a Spring AOP Advisor for the given AspectJ advice method.
	 * @param candidateAdviceMethod the candidate advice method
	 * @param aspectInstanceFactory the aspect instance factory
	 * @param declarationOrder the declaration order within the aspect
	 * @param aspectName the name of the aspect
	 * @return {@code null} if the method is not an AspectJ advice method
	 * or if it is a pointcut that will be used by other advice but will not
	 * create a Spring advice in its own right
	 */
	@Nullable
	Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
			int declarationOrder, String aspectName);

	/**
	 * Build a Spring AOP Advice for the given AspectJ advice method.
	 *
	 * 为给定的 AspectJ advice方法构建 Spring AOP Advice。
	 *
	 * @param candidateAdviceMethod the candidate advice method
	 * @param expressionPointcut the AspectJ expression pointcut AspectJ 表达式切入点
	 * @param aspectInstanceFactory the aspect instance factory
	 * @param declarationOrder the declaration order within the aspect aspect内的声明顺序
	 * @param aspectName the name of the aspect
	 * @return {@code null} if the method is not an AspectJ advice method
	 * or if it is a pointcut that will be used by other advice but will not
	 * create a Spring advice in its own right {@code null} 如果该方法不是 AspectJ advice方法，或者它是将被其他advice使用的切入点，但不会自行创建 Spring advice
	 * @see org.springframework.aop.aspectj.AspectJAroundAdvice
	 * @see org.springframework.aop.aspectj.AspectJMethodBeforeAdvice
	 * @see org.springframework.aop.aspectj.AspectJAfterAdvice
	 * @see org.springframework.aop.aspectj.AspectJAfterReturningAdvice
	 * @see org.springframework.aop.aspectj.AspectJAfterThrowingAdvice
	 */
	@Nullable
	Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
			MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName);

}
