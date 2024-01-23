/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.lang.reflect.Constructor;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * Extension of the {@link InstantiationAwareBeanPostProcessor} interface,
 * adding a callback for predicting the eventual type of a processed bean.
 *
 * 扩展 {@link InstantiationAwareBeanPostProcessor} 接口，添加用于预测已处理 bean 的最终类型的回调。
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. In general, application-provided
 * post-processors should simply implement the plain {@link BeanPostProcessor}
 * interface or derive from the {@link InstantiationAwareBeanPostProcessorAdapter}
 * class. New methods might be added to this interface even in point releases.
 *
 * 注意：该接口是一个特殊用途的接口，主要供框架内部使用。一般来说，应用程序提供的后处理器应该简单地实现普通的 BeanPostProcessor 接口
 * 或派生自 InstantiationAwareBeanPostProcessorAdapter 类。即使在单点版本中，也可能会向此接口添加新方法。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see InstantiationAwareBeanPostProcessorAdapter
 */
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

	/**
	 * Predict the type of the bean to be eventually returned from this
	 * processor's {@link #postProcessBeforeInstantiation} callback.
	 * <p>The default implementation returns {@code null}.
	 *
	 * 预测最终要从此处理器的 {@link #postProcessBeforeInstantiation} 回调返回的 Bean 类型。
	 * 默认实现返回 {@code null}。
	 *
	 * @param beanClass the raw class of the bean
	 * @param beanName the name of the bean
	 * @return the type of the bean, or {@code null} if not predictable
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	@Nullable
	default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * Determine the candidate constructors to use for the given bean.
	 * <p>The default implementation returns {@code null}.
	 *
	 * 确定要用于给定 Bean 的候选构造函数。
	 * 默认实现返回 {@code null}。
	 *
	 * @param beanClass the raw class of the bean (never {@code null})
	 * @param beanName the name of the bean
	 * @return the candidate constructors, or {@code null} if none specified
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	@Nullable
	default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 * Obtain a reference for early access to the specified bean,
	 * typically for the purpose of resolving a circular reference.
	 * <p>This callback gives post-processors a chance to expose a wrapper
	 * early - that is, before the target bean instance is fully initialized.
	 * The exposed object should be equivalent to the what
	 * {@link #postProcessBeforeInitialization} / {@link #postProcessAfterInitialization}
	 * would expose otherwise. Note that the object returned by this method will
	 * be used as bean reference unless the post-processor returns a different
	 * wrapper from said post-process callbacks. In other words: Those post-process
	 * callbacks may either eventually expose the same reference or alternatively
	 * return the raw bean instance from those subsequent callbacks (if the wrapper
	 * for the affected bean has been built for a call to this method already,
	 * it will be exposes as final bean reference by default).
	 * <p>The default implementation returns the given {@code bean} as-is.
	 * --
	 * 获取对指定bean的早期访问引用，通常用于解析循环引用。
	 * 此回调为后处理器提供了在目标bean实例完全初始化之前提前公开包装器的机会。
	 * 公开的对象应该等同于{@link #postProcessBeforeInitialization} / {@link #postProcessAfterInitialization}
	 * 否则将公开的对象视为bean引用，除非后处理器从这些后处理回调中返回不同的包装器。
	 * 换句话说：这些后处理回调可以最终公开相同的引用，或者从这些后续回调中返回受影响的bean的原始实例
	 * （如果已为对此方法的调用构建了受影响的bean的包装器，则默认情况下将其公开为最终bean引用）。
	 * 默认实现将给定的{@code bean}原样返回。
	 *
	 * @param bean the raw bean instance -- 原始bean实例
	 * @param beanName the name of the bean -- bean的名称
	 * @return the object to expose as bean reference
	 * (typically with the passed-in bean instance as default)
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
