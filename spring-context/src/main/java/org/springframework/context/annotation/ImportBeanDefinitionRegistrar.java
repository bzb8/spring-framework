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

package org.springframework.context.annotation;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Interface to be implemented by types that register additional bean definitions when
 * processing @{@link Configuration} classes. Useful when operating at the bean definition
 * level (as opposed to {@code @Bean} method/instance level) is desired or necessary.
 *
 * 由在处理 @{@link Configuration} 类时注册其他 Bean 定义的类型实现的接口。当需要或需要在 Bean 定义级别（而不是 {@code @Bean} 方法实例级别）进行操作时很有用。
 *
 * <p>Along with {@code @Configuration} and {@link ImportSelector}, classes of this type
 * may be provided to the @{@link Import} annotation (or may also be returned from an
 * {@code ImportSelector}).
 *
 * 与 {@code @Configuration} 和 {@link ImportSelector} 一起，此类型的类可以提供给 @{@link Import} 注解（也可以从 {@code ImportSelector} 返回）。
 *
 * <p>An {@link ImportBeanDefinitionRegistrar} may implement any of the following
 * {@link org.springframework.beans.factory.Aware Aware} interfaces, and their respective
 * methods will be called prior to {@link #registerBeanDefinitions}:
 *
 * {@link ImportBeanDefinitionRegistrar}可以实现以下任何{@link org.springframework.beans.factory.Aware Aware}接口，并且它们各自的方法将在{@link #registerBeanDefinitions}之前调用：
 *
 * <ul>
 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}
 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}
 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}
 * </ul>
 *
 * <p>Alternatively, the class may provide a single constructor with one or more of
 * the following supported parameter types:
 *
 * 或者，该类可以为单个构造函数提供以下一个或多个受支持的参数类型：
 *
 * <ul>
 * <li>{@link org.springframework.core.env.Environment Environment}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactory BeanFactory}</li>
 * <li>{@link java.lang.ClassLoader ClassLoader}</li>
 * <li>{@link org.springframework.core.io.ResourceLoader ResourceLoader}</li>
 * </ul>
 *
 * <p>See implementations and associated unit tests for usage examples.
 *
 * 有关使用示例，请参阅实现和关联的单元测试。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see Import
 * @see ImportSelector
 * @see Configuration
 */
public interface ImportBeanDefinitionRegistrar {

	/**
	 * Register bean definitions as necessary based on the given annotation metadata of
	 * the importing {@code @Configuration} class.
	 * <p>Note that {@link BeanDefinitionRegistryPostProcessor} types may <em>not</em> be
	 * registered here, due to lifecycle constraints related to {@code @Configuration}
	 * class processing.
	 * <p>The default implementation delegates to
	 * {@link #registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry)}.
	 * --
	 * 根据导入的 {@code @Configuration} 类的注解元数据注册必要的 Bean 定义。
	 * 请注意，由于与 {@code @Configuration} 类处理相关的生命周期约束，
	 * 此处可能不会注册 {@link BeanDefinitionRegistryPostProcessor} 类型。
	 * 默认实现委托给 {@link #registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry)}。
	 *
	 * @param importingClassMetadata annotation metadata of the importing class -- 配置类的注解元数据
	 * @param registry current bean definition registry --  定义注册表
	 * @param importBeanNameGenerator the bean name generator strategy for imported beans:
	 * {@link ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR} by default, or a
	 * user-provided one if {@link ConfigurationClassPostProcessor#setBeanNameGenerator}
	 * has been set. In the latter case, the passed-in strategy will be the same used for
	 * component scanning in the containing application context (otherwise, the default
	 * component-scan naming strategy is {@link AnnotationBeanNameGenerator#INSTANCE}).
	 * --
	 * 导入的 Bean 的 bean 名称生成策略：
	 * 默认为 {@link ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR}，
	 * 或者如果设置了 {@link ConfigurationClassPostProcessor#setBeanNameGenerator}，则为用户提供的策略。
	 * 在后一种情况下，传递的策略将与包含应用程序上下文中的组件扫描使用相同
	 * （否则，默认组件扫描命名策略为 {@link AnnotationBeanNameGenerator#INSTANCE}）。
	 *
	 * @since 5.2
	 * @see ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR
	 * @see ConfigurationClassPostProcessor#setBeanNameGenerator
	 */
	default void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
			BeanNameGenerator importBeanNameGenerator) {

		registerBeanDefinitions(importingClassMetadata, registry);
	}

	/**
	 * Register bean definitions as necessary based on the given annotation metadata of
	 * the importing {@code @Configuration} class.
	 * <p>Note that {@link BeanDefinitionRegistryPostProcessor} types may <em>not</em> be
	 * registered here, due to lifecycle constraints related to {@code @Configuration}
	 * class processing.
	 * <p>The default implementation is empty.
	 *
	 * 根据需要根据导入 {@code @Configuration} 类的给定注解元数据注册 Bean 定义。
	 * 请注意，由于与 {@code @Configuration} 类处理相关的生命周期约束，可能未在此处注册 {@link BeanDefinitionRegistryPostProcessor} 类型。
	 * 默认实现为空。
	 *
	 * @param importingClassMetadata annotation metadata of the importing class
	 * @param registry current bean definition registry
	 */
	default void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
	}

}
