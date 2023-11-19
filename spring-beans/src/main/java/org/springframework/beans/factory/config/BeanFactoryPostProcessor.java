/*
 * Copyright 2002-2019 the original author or authors.
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
 * Factory hook that allows for custom modification of an application context's
 * bean definitions, adapting the bean property values of the context's underlying
 * bean factory.
 * 工厂钩子，允许自定义修改应用程序上下文的 Bean 定义，调整上下文底层 Bean 工厂的 Bean 属性值
 *
 * <p>Useful for custom config files targeted at system administrators that
 * override bean properties configured in the application context. See
 * {@link PropertyResourceConfigurer} and its concrete implementations for
 * out-of-the-box solutions that address such configuration needs.
 *
 * 对于面向系统管理员的自定义配置文件很有用，这些配置文件覆盖了在应用程序上下文中配置的 Bean 属性。
 * 请参阅 {@link PropertyResourceConfigurer} 及其具体实现，了解满足此类配置需求的开箱即用解决方案。
 *
 * <p>A {@code BeanFactoryPostProcessor} may interact with and modify bean
 * definitions, but never bean instances. Doing so may cause premature bean
 * instantiation, violating the container and causing unintended side effects.
 * If bean instance interaction is required, consider implementing
 * {@link BeanPostProcessor} instead.
 *
 * 一个BeanFactoryPostProcessor可以与 Bean definitions交互并修改 Bean definitions，但不能与 Bean 实例交互。
 * 这样做可能会导致 Bean 过早实例化，违反容器并导致意外的副作用。如果需要 Bean 实例交互，请考虑改为实现 {@link BeanPostProcessor}。
 *
 * <h3>Registration</h3>
 * <p>An {@code ApplicationContext} auto-detects {@code BeanFactoryPostProcessor}
 * beans in its bean definitions and applies them before any other beans get created.
 * A {@code BeanFactoryPostProcessor} may also be registered programmatically
 * with a {@code ConfigurableApplicationContext}.
 *
 * ApplicationContext在其Bean definitions中自动检测BeanFactoryPostProcessor beans，并在创建任何其他 Bean 之前应用它们。
 * BeanFactoryPostProcessor也可以以编程方式向ConfigurableApplicationContext注册。
 * （void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor);）
 *
 * <h3>Ordering</h3>
 * <p>{@code BeanFactoryPostProcessor} beans that are autodetected in an
 * {@code ApplicationContext} will be ordered according to
 * {@link org.springframework.core.PriorityOrdered} and
 * {@link org.springframework.core.Ordered} semantics. In contrast,
 * {@code BeanFactoryPostProcessor} beans that are registered programmatically
 * with a {@code ConfigurableApplicationContext} will be applied in the order of
 * registration; any ordering semantics expressed through implementing the
 * {@code PriorityOrdered} or {@code Ordered} interface will be ignored for
 * programmatically registered post-processors. Furthermore, the
 * {@link org.springframework.core.annotation.Order @Order} annotation is not
 * taken into account for {@code BeanFactoryPostProcessor} beans.
 * 
 * 在ApplicationContext中自动检测到的BeanFactoryPostProcessor将根据{@link org.springframework.core.PriorityOrdered}和{@link org.springframework.core.Ordered}语义进行排序。
 * 相反，使用 {@code ConfigurableApplicationContext} 以编程方式注册的 {@code BeanFactoryPostProcessor} bean 将按注册顺序应用;对于以编程方式注册的后处理器，将忽略通过实现 {@code PriorityOrdered}
 * 或 {@code Ordered} 接口表达的任何排序语义。此外，{@link org.springframework.core.annotation.Order @Order} 注释未考虑在 {@code BeanFactoryPostProcessor} bean 中。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 06.07.2003
 * @see BeanPostProcessor
 * @see PropertyResourceConfigurer
 */
@FunctionalInterface
public interface BeanFactoryPostProcessor {

	/**
	 * Modify the application context's internal bean factory after its standard
	 * initialization. All bean definitions will have been loaded, but no beans
	 * will have been instantiated yet. This allows for overriding or adding
	 * properties even to eager-initializing beans.
	 * 在标准初始化后修改应用程序上下文的内部 bean 工厂。所有 bean 定义都将被加载，但还没有任何 bean 被实例化。这甚至允许覆盖或添加属性，甚至是急于初始化的 bean。
	 * @param beanFactory the bean factory used by the application context
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
