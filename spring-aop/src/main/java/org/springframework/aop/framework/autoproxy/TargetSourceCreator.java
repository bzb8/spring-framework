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

package org.springframework.aop.framework.autoproxy;

import org.springframework.aop.TargetSource;
import org.springframework.lang.Nullable;

/**
 * Implementations can create special target sources, such as pooling target
 * sources, for particular beans. For example, they may base their choice
 * on attributes, such as a pooling attribute, on the target class.
 *
 * 实现可以为特定 Bean 创建特殊的目标源，例如池化目标源。例如，他们可能会根据目标类的属性（如池化属性）进行选择。
 *
 * <p>AbstractAutoProxyCreator can support a number of TargetSourceCreators,
 * which will be applied in order.
 *
 * AbstractAutoProxyCreator 可以支持多个 TargetSourceCreator，这些 TargetSourceCreators 将按顺序应用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@FunctionalInterface
public interface TargetSourceCreator {

	/**
	 * Create a special TargetSource for the given bean, if any. 为给定的 Bean 创建一个特殊的 TargetSource（如果有）。
	 * @param beanClass the class of the bean to create a TargetSource for
	 * @param beanName the name of the bean
	 * @return a special TargetSource or {@code null} if this TargetSourceCreator isn't
	 * interested in the particular bean 一个特殊的 TargetSource 或 {@code null}（如果此 TargetSourceCreator 对特定 bean 不感兴趣）
	 */
	@Nullable
	TargetSource getTargetSource(Class<?> beanClass, String beanName);

}
