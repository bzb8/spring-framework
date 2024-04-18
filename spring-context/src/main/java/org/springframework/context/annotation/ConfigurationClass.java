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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Represents a user-defined {@link Configuration @Configuration} class.
 * <p>Includes a set of {@link Bean} methods, including all such methods
 * defined in the ancestry of the class, in a 'flattened-out' manner.
 *
 * 表示用户定义的 {@link Configuration @Configuration} 类。
 * 包括一组 {@link Bean} 方法，包括在类的祖先中定义的所有此类方法，以“扁平化”方式定义。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 * @see BeanMethod
 * @see ConfigurationClassParser
 */
final class ConfigurationClass {

	private final AnnotationMetadata metadata;

	private final Resource resource;

	/**
	 * 配置类的beanName，如appConfig
	 */
	@Nullable
	private String beanName;

	/**
	 * 表示导入该配置类的配置类集
	 */
	private final Set<ConfigurationClass> importedBy = new LinkedHashSet<>(1);

	/**
	 * -- @Bean 标记的方法
	 */
	private final Set<BeanMethod> beanMethods = new LinkedHashSet<>();

	/**
	 * -- @ImportResource 的locations 解析后的某个值 -> @ImportResource的reader()属性值
	 */
	private final Map<String, Class<? extends BeanDefinitionReader>> importedResources =
			new LinkedHashMap<>();

	/**
	 * 将该ImportBeanDefinitionRegistrar缓存在configClass的importBeanDefinitionRegistrars
	 * 处理@Import注解缓存的, @Import注解的value值是ImportBeanDefinitionRegistrar类型，初始化它 -> 当前配置类
	 */
	private final Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> importBeanDefinitionRegistrars =
			new LinkedHashMap<>();

	/**
	 * 缓存跳过的@Bean方法，不应该生成其bean定义
	 */
	final Set<String> skippedBeanMethods = new HashSet<>();


	/**
	 * Create a new {@link ConfigurationClass} with the given name.
	 * @param metadataReader reader used to parse the underlying {@link Class}
	 * @param beanName must not be {@code null}
	 * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
	 */
	ConfigurationClass(MetadataReader metadataReader, String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		this.metadata = metadataReader.getAnnotationMetadata();
		this.resource = metadataReader.getResource();
		this.beanName = beanName;
	}

	/**
	 * Create a new {@link ConfigurationClass} representing a class that was imported
	 * using the {@link Import} annotation or automatically processed as a nested
	 * configuration class (if importedBy is not {@code null}).
	 * @param metadataReader reader used to parse the underlying {@link Class}
	 * @param importedBy the configuration class importing this one or {@code null}
	 * @since 3.1.1
	 */
	ConfigurationClass(MetadataReader metadataReader, @Nullable ConfigurationClass importedBy) {
		this.metadata = metadataReader.getAnnotationMetadata();
		this.resource = metadataReader.getResource();
		this.importedBy.add(importedBy);
	}

	/**
	 * Create a new {@link ConfigurationClass} with the given name.
	 * @param clazz the underlying {@link Class} to represent
	 * @param beanName name of the {@code @Configuration} class bean
	 * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
	 */
	ConfigurationClass(Class<?> clazz, String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		this.metadata = AnnotationMetadata.introspect(clazz);
		this.resource = new DescriptiveResource(clazz.getName());
		this.beanName = beanName;
	}

	/**
	 * Create a new {@link ConfigurationClass} representing a class that was imported
	 * using the {@link Import} annotation or automatically processed as a nested
	 * configuration class (if imported is {@code true}).
	 * --
	 * 创建一个新的 {@link ConfigurationClass}，表示使用 {@link Import} 注解导入的类，
	 * 或者作为自动处理的嵌套配置类（如果 imported 参数为 {@code true}）。
	 *
	 * @param clazz the underlying {@link Class} to represent -- 要表示的底层 {@link Class}
	 * @param importedBy the configuration class importing this one (or {@code null}) -- 导入该配置类的配置类（或 {@code null}）
	 * @since 3.1.1
	 */
	ConfigurationClass(Class<?> clazz, @Nullable ConfigurationClass importedBy) {
		this.metadata = AnnotationMetadata.introspect(clazz);
		this.resource = new DescriptiveResource(clazz.getName());
		this.importedBy.add(importedBy);
	}

	/**
	 * Create a new {@link ConfigurationClass} with the given name.
	 * @param metadata the metadata for the underlying class to represent 要表示的基础类的元数据
	 * @param beanName name of the {@code @Configuration} class bean {@code @Configuration} 类 Bean 的 NAME
	 * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
	 */
	ConfigurationClass(AnnotationMetadata metadata, String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		this.metadata = metadata;
		this.resource = new DescriptiveResource(metadata.getClassName());
		this.beanName = beanName;
	}


	AnnotationMetadata getMetadata() {
		return this.metadata;
	}

	Resource getResource() {
		return this.resource;
	}

	String getSimpleName() {
		return ClassUtils.getShortName(getMetadata().getClassName());
	}

	void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return whether this configuration class was registered via @{@link Import} or
	 * automatically registered due to being nested within another configuration class.
	 * --
	 * 返回此配置类是否是通过@{@link Import}注册的，或者由于嵌套在另一个配置类中而自动注册的。
	 *
	 * @since 3.1.1
	 * @see #getImportedBy()
	 *
	 *
	 */
	public boolean isImported() {
		return !this.importedBy.isEmpty();
	}

	/**
	 * Merge the imported-by declarations from the given configuration class into this one.
	 * @since 4.0.5
	 */
	void mergeImportedBy(ConfigurationClass otherConfigClass) {
		this.importedBy.addAll(otherConfigClass.importedBy);
	}

	/**
	 * Return the configuration classes that imported this class,
	 * or an empty Set if this configuration was not imported.
	 * @since 4.0.5
	 * @see #isImported()
	 */
	Set<ConfigurationClass> getImportedBy() {
		return this.importedBy;
	}

	void addBeanMethod(BeanMethod method) {
		this.beanMethods.add(method);
	}

	Set<BeanMethod> getBeanMethods() {
		return this.beanMethods;
	}

	void addImportedResource(String importedResource, Class<? extends BeanDefinitionReader> readerClass) {
		this.importedResources.put(importedResource, readerClass);
	}

	void addImportBeanDefinitionRegistrar(ImportBeanDefinitionRegistrar registrar, AnnotationMetadata importingClassMetadata) {
		this.importBeanDefinitionRegistrars.put(registrar, importingClassMetadata);
	}

	Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> getImportBeanDefinitionRegistrars() {
		return this.importBeanDefinitionRegistrars;
	}

	Map<String, Class<? extends BeanDefinitionReader>> getImportedResources() {
		return this.importedResources;
	}

	/**
	 * -- @Configuration 注解的proxyBeanMethods属性 == true的情况下，
	 * 则 1. 类不能是final的 2. @Bean 方法必须是可重写的，不能是final或private的
	 *
	 * @param problemReporter
	 */
	void validate(ProblemReporter problemReporter) {
		// A configuration class may not be final (CGLIB limitation) unless it declares proxyBeanMethods=false
		// 配置类可能不是最终的（CGLIB 限制），除非它声明了 proxyBeanMethods=false
		// 获取配置类的@Configuration注解的属性
		Map<String, Object> attributes = this.metadata.getAnnotationAttributes(Configuration.class.getName());
		// 它的proxyBeanMethods == true
		if (attributes != null && (Boolean) attributes.get("proxyBeanMethods")) {
			// 配置类不能是final
			if (this.metadata.isFinal()) {
				problemReporter.error(new FinalConfigurationProblem());
			}
			for (BeanMethod beanMethod : this.beanMethods) {
				beanMethod.validate(problemReporter);
			}
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ConfigurationClass &&
				getMetadata().getClassName().equals(((ConfigurationClass) other).getMetadata().getClassName())));
	}

	@Override
	public int hashCode() {
		return getMetadata().getClassName().hashCode();
	}

	@Override
	public String toString() {
		return "ConfigurationClass: beanName '" + this.beanName + "', " + this.resource;
	}


	/**
	 * Configuration classes must be non-final to accommodate CGLIB subclassing.
	 */
	private class FinalConfigurationProblem extends Problem {

		FinalConfigurationProblem() {
			super(String.format("@Configuration class '%s' may not be final. Remove the final modifier to continue.",
					getSimpleName()), new Location(getResource(), getMetadata()));
		}
	}

}
