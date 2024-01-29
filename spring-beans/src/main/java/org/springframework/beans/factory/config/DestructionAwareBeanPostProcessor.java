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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * Subinterface of {@link BeanPostProcessor} that adds a before-destruction callback.
 * {@link BeanPostProcessor} 的子接口，用于添加销毁前回调。
 *
 * <p>The typical usage will be to invoke custom destruction callbacks on
 * specific bean types, matching corresponding initialization callbacks.
 * 典型的用法是在特定的 Bean 类型上调用自定义销毁回调，匹配相应的初始化回调。
 *
 * @author Juergen Hoeller
 * @since 1.0.1
 */
public interface DestructionAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * Apply this BeanPostProcessor to the given bean instance before its
	 * destruction, e.g. invoking custom destruction callbacks.
	 * <p>Like DisposableBean's {@code destroy} and a custom destroy method, this
	 * callback will only apply to beans which the container fully manages the
	 * lifecycle for. This is usually the case for singletons and scoped beans.
	 * --
	 * 在销毁之前将此 BeanPostProcessor 应用于给定的 Bean 实例，例如调用自定义销毁回调。
	 * 与 DisposableBean destroy 和自定义销毁方法一样，此回调仅适用于容器完全管理其生命周期的 bean。这通常是单例和作用域 Bean 的情况。
	 *
	 * @param bean the bean instance to be destroyed
	 * @param beanName the name of the bean
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#setDestroyMethodName(String)
	 */
	void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException;

	/**
	 * Determine whether the given bean instance requires destruction by this
	 * post-processor.
	 * <p>The default implementation returns {@code true}. If a pre-5 implementation
	 * of {@code DestructionAwareBeanPostProcessor} does not provide a concrete
	 * implementation of this method, Spring silently assumes {@code true} as well.
	 * --
	 * 这段代码描述了一个方法，用于确定给定的bean实例是否需要由这个后置处理器进行销毁。
	 * <p>默认实现返回{@code true}。如果一个在5.0版本之前的{@code DestructionAwareBeanPostProcessor}实现没有提供对该方法的具体实现，
	 * Spring会默默地假设也返回{@code true}
	 *
	 * @param bean the bean instance to check
	 * @return {@code true} if {@link #postProcessBeforeDestruction} is supposed to
	 * be called for this bean instance eventually, or {@code false} if not needed
	 * --
	 * 如果最终应该为此bean实例调用{@link #postProcessBeforeDestruction}，则返回{@code true}，否则返回{@code false}
	 *
	 * @since 4.3
	 */
	default boolean requiresDestruction(Object bean) {
		return true;
	}

}
