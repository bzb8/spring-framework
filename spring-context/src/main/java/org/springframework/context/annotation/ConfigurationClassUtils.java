/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.event.EventListenerFactory;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Utilities for identifying {@link Configuration} classes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
abstract class ConfigurationClassUtils {

	public static final String CONFIGURATION_CLASS_FULL = "full";

	public static final String CONFIGURATION_CLASS_LITE = "lite";

	/**
	 * 表示它是一个配置类
	 * org.springframework.context.annotation.ConfigurationClassPostProcessor.configurationClass
	 */
	public static final String CONFIGURATION_CLASS_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");

	private static final String ORDER_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "order");


	private static final Log logger = LogFactory.getLog(ConfigurationClassUtils.class);

	private static final Set<String> candidateIndicators = new HashSet<>(8);

	static {
		candidateIndicators.add(Component.class.getName());
		candidateIndicators.add(ComponentScan.class.getName());
		candidateIndicators.add(Import.class.getName());
		candidateIndicators.add(ImportResource.class.getName());
	}


	/**
	 * Check whether the given bean definition is a candidate for a configuration class
	 * (or a nested component class declared within a configuration/component class,
	 * to be auto-registered as well), and mark it accordingly.
	 * --
	 * 检查给定的 Bean 定义是否是配置类的候选者（或在 configuration/component 类中声明的嵌套组件类，也要自动注册），并相应地对其进行标记。
	 * 具有@Configuration注解 && 带有Component.class, ComponentScan.class, Import.class,
	 * ImportResource.class注解或有@Bean注解的方法就是配置候选类，则return true
	 *
	 * --
	 * 在checkConfigurationClassCandidate中，配置类的类型分为两种，Full 和 Lite，即完整的配置类和精简的配置类
	 * Full : 即类被 @Configuration 注解修饰 && proxyBeanMethods属性为true (默认为 true)
	 * Full 配置类就是我们常规使用的配置类
	 * Lite : 被 @Component、@ComponentScan、@Import、@ImportResource 修饰的类 或者 类中有被@Bean修饰的方法
	 * Lite 配置类就是一些需要其他操作引入一些bean 的类
	 *
	 * @param beanDef the bean definition to check
	 * @param metadataReaderFactory the current factory in use by the caller -- 调用方正在使用的当前工厂(元数据读取工厂)
	 * @return whether the candidate qualifies as (any kind of) configuration class
	 * 候选者是否有资格成为（任何类型的）配置类
	 */
	public static boolean checkConfigurationClassCandidate(
			BeanDefinition beanDef, MetadataReaderFactory metadataReaderFactory) {

		// bean定义的类名
		String className = beanDef.getBeanClassName();
		// className == null || beanDefinition的工厂方法不为空，则返回false
		if (className == null || beanDef.getFactoryMethodName() != null) {
			return false;
		}

		// 解析关于当前被解析类的 注解元数据
		AnnotationMetadata metadata;
		// 如果当前BeanDefinition是AnnotatedBeanDefinition直接获取注解元数据即可
		if (beanDef instanceof AnnotatedBeanDefinition &&
				className.equals(((AnnotatedBeanDefinition) beanDef).getMetadata().getClassName())) {
			// Can reuse the pre-parsed metadata from the given BeanDefinition...
			// 可以重用给定 BeanDefinition 中预解析的元数据...
			metadata = ((AnnotatedBeanDefinition) beanDef).getMetadata();
		}
		else if (beanDef instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) beanDef).hasBeanClass()) {
			// Check already loaded Class if present...
			// since we possibly can't even load the class file for this Class.
			// 检查已加载的类（如果存在）...因为我们甚至可能无法加载这个类的类文件。
			Class<?> beanClass = ((AbstractBeanDefinition) beanDef).getBeanClass();
			// 如果当前类是 BeanFactoryPostProcessor、BeanPostProcessor
			// AopInfrastructureBean、EventListenerFactory 类型不当做配置类处理，返回false
			if (BeanFactoryPostProcessor.class.isAssignableFrom(beanClass) ||
					BeanPostProcessor.class.isAssignableFrom(beanClass) ||
					AopInfrastructureBean.class.isAssignableFrom(beanClass) ||
					EventListenerFactory.class.isAssignableFrom(beanClass)) {
				return false;
			}
			metadata = AnnotationMetadata.introspect(beanClass);
		}
		else {
			// 按照默认规则解析
			try {
				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
				metadata = metadataReader.getAnnotationMetadata();
			}
			catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not find class file for introspecting configuration annotations: " +
							className, ex);
				}
				return false;
			}
		}

		// 获取bean上的Configuration 注解的属性。如果没有被 @Configuration 修饰 config 则为null
		Map<String, Object> config = metadata.getAnnotationAttributes(Configuration.class.getName());
		// 如果被 @Configuration 修饰 &&  proxyBeanMethods 属性为 不是false
		// @Configuration 的 proxyBeanMethods  属性默认值即为 true。
		if (config != null && !Boolean.FALSE.equals(config.get("proxyBeanMethods"))) {
			// 设置 CONFIGURATION_CLASS_ATTRIBUTE 为 full
			// Full 配置类就是我们常规使用的配置类
			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
		}
		else if (config != null || isConfigurationCandidate(metadata)) {
			// 设置 CONFIGURATION_CLASS_ATTRIBUTE 为 lite
			// Lite 配置类就是一些需要其他操作引入一些bean 的类
			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_LITE);
		}
		else {
			return false;
		}

		// It's a full or lite configuration candidate... Let's determine the order value, if any.
		// 这是一个完整或精简的配置候选者......让我们确定其order值（如果有）。
		// 按照@Order注解设置bean定义的排序属性值
		Integer order = getOrder(metadata);
		if (order != null) {
			beanDef.setAttribute(ORDER_ATTRIBUTE, order);
		}

		return true;
	}

	/**
	 * Check the given metadata for a configuration class candidate
	 * (or nested component class declared within a configuration/component class).
	 * --
	 * 检查给定元数据中是否有候选配置类（或在 configuration/component 类中声明的嵌套组件类）。
	 * 带有Component.class, ComponentScan.class, Import.class, ImportResource.class注解或有@Bean注解的方法就是配置候选类
	 * Lite配置类筛选
	 *
	 * @param metadata the metadata of the annotated class 带注解的类的元数据
	 * @return {@code true} if the given class is to be registered for
	 * configuration class processing; {@code false} otherwise
	 *
	 * {@code true} 如果要注册给定的类以进行配置类处理;{@code false} 否则
	 *
	 */
	public static boolean isConfigurationCandidate(AnnotationMetadata metadata) {
		// Do not consider an interface or an annotation...
		// 不要考虑接口或注解......
		if (metadata.isInterface()) {
			return false;
		}

		// Any of the typical annotations found?
		// 找到任何典型的注解吗？
		// Component.class, ComponentScan.class, Import.class, ImportResource.class
		// 被 candidateIndicators 中的其中一个注解修饰。其中 candidateIndicators  注解在静态代码块中加载了
		for (String indicator : candidateIndicators) {
			if (metadata.isAnnotated(indicator)) {
				return true;
			}
		}

		// Finally, let's look for @Bean methods...
		// 最后，让我们寻找 @Bean 方法......
		return hasBeanMethods(metadata);
	}

	static boolean hasBeanMethods(AnnotationMetadata metadata) {
		try {
			return metadata.hasAnnotatedMethods(Bean.class.getName());
		}
		catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to introspect @Bean methods on class [" + metadata.getClassName() + "]: " + ex);
			}
			return false;
		}
	}

	/**
	 * Determine the order for the given configuration class metadata.
	 * @param metadata the metadata of the annotated class
	 * @return the {@code @Order} annotation value on the configuration class,
	 * or {@code Ordered.LOWEST_PRECEDENCE} if none declared
	 * @since 5.0
	 */
	@Nullable
	public static Integer getOrder(AnnotationMetadata metadata) {
		Map<String, Object> orderAttributes = metadata.getAnnotationAttributes(Order.class.getName());
		return (orderAttributes != null ? ((Integer) orderAttributes.get(AnnotationUtils.VALUE)) : null);
	}

	/**
	 * Determine the order for the given configuration class bean definition,
	 * as set by {@link #checkConfigurationClassCandidate}.
	 * --
	 * 确定给定配置类 Bean 定义的顺序，由 {@link #checkConfigurationClassCandidate} 设置。
	 *
	 * @param beanDef the bean definition to check
	 * @return the {@link Order @Order} annotation value on the configuration class,
	 * or {@link Ordered#LOWEST_PRECEDENCE} if none declared
	 * @since 4.2
	 */
	public static int getOrder(BeanDefinition beanDef) {
		Integer order = (Integer) beanDef.getAttribute(ORDER_ATTRIBUTE);
		return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
	}

}
