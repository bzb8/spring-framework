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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * A single {@code condition} that must be {@linkplain #matches matched} in order
 * for a component to be registered.
 * 一个条件接口，组件只有在该条件被{@linkplain #matches 匹配}的情况下才能被注册。
 *
 * <p>Conditions are checked immediately before the bean-definition is due to be
 * registered and are free to veto registration based on any criteria that can
 * be determined at that point.
 * <p>条件在 bean 定义即将注册的立即时刻进行检查，并且可以根据当时任何可确定的条件自由决定是否阻止注册
 *
 * <p>Conditions must follow the same restrictions as {@link BeanFactoryPostProcessor}
 * and take care to never interact with bean instances. For more fine-grained control
 * of conditions that interact with {@code @Configuration} beans consider implementing
 * the {@link ConfigurationCondition} interface.
 * <p>条件必须遵守与{@link BeanFactoryPostProcessor}相同的限制，并且要避免与 bean 实例交互。
 * 如需更细粒度地控制与{@code @Configuration} beans 交互的条件，可以考虑实现 {@link ConfigurationCondition} 接口。
 *
 * @author Phillip Webb
 * @since 4.0
 * @see ConfigurationCondition
 * @see Conditional
 * @see ConditionContext
 */
@FunctionalInterface
public interface Condition {

	/**
	 * Determine if the condition matches.
	 * 判断条件是否匹配。
	 * @param context the condition context
	 *                条件上下文，包含环境信息和元数据。
	 * @param metadata the metadata of the {@link org.springframework.core.type.AnnotationMetadata class}
	 * or {@link org.springframework.core.type.MethodMetadata method} being checked
	 *                 当前正在检查的类或方法的注解元数据。
	 * @return {@code true} if the condition matches and the component can be registered,
	 * or {@code false} to veto the annotated component's registration
	 * 如果条件匹配且组件可以注册，则返回{@code true}；如果条件不匹配，要阻止注解组件的注册，则返回{@code false}。
	 */
	boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);

}
