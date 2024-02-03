/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Reads a given fully-populated set of ConfigurationClass instances, registering bean
 * definitions with the given {@link BeanDefinitionRegistry} based on its contents.
 *
 * 读取一组给定的完全填充的 ConfigurationClass 实例，并根据其内容向给定的 {@link BeanDefinitionRegistry} 注册 Bean 定义。
 *
 * <p>This class was modeled after the {@link BeanDefinitionReader} hierarchy, but does
 * not implement/extend any of its artifacts as a set of configuration classes is not a
 * {@link Resource}.
 *
 * 此类是按照 {@link BeanDefinitionReader} 层次结构建模的，但未实现扩展其任何工件，因为一组配置类不是 {@link Resource}。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @since 3.0
 * @see ConfigurationClassParser
 */
class ConfigurationClassBeanDefinitionReader {

	private static final Log logger = LogFactory.getLog(ConfigurationClassBeanDefinitionReader.class);

	private static final ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	/**
	 * Boolean flag controlled by a {@code spring.xml.ignore} system property that instructs Spring to
	 * ignore XML, i.e. to not initialize the XML-related infrastructure.
	 * <p>The default is "false".
	 *
	 * 布尔标志由 {@code spring.xml.ignore} 系统属性控制，指示 Spring 忽略 XML，即不初始化与 XML 相关的基础设施。
	 * 默认值为“false”。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	private final BeanDefinitionRegistry registry;

	private final SourceExtractor sourceExtractor;

	private final ResourceLoader resourceLoader;

	private final Environment environment;

	private final BeanNameGenerator importBeanNameGenerator;

	private final ImportRegistry importRegistry;

	private final ConditionEvaluator conditionEvaluator;


	/**
	 * Create a new {@link ConfigurationClassBeanDefinitionReader} instance
	 * that will be used to populate the given {@link BeanDefinitionRegistry}.
	 */
	ConfigurationClassBeanDefinitionReader(BeanDefinitionRegistry registry, SourceExtractor sourceExtractor,
			ResourceLoader resourceLoader, Environment environment, BeanNameGenerator importBeanNameGenerator,
			ImportRegistry importRegistry) {

		this.registry = registry;
		this.sourceExtractor = sourceExtractor;
		this.resourceLoader = resourceLoader;
		this.environment = environment;
		this.importBeanNameGenerator = importBeanNameGenerator;
		this.importRegistry = importRegistry;
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, resourceLoader);
	}


	/**
	 * Read {@code configurationModel}, registering bean definitions
	 * with the registry based on its contents.
	 * --
	 * 读取 {@code configurationModel}，根据其内容向注册表注册 bean 定义。
	 *
	 */
	public void loadBeanDefinitions(Set<ConfigurationClass> configurationModel) {
		TrackedConditionEvaluator trackedConditionEvaluator = new TrackedConditionEvaluator();
		for (ConfigurationClass configClass : configurationModel) {
			loadBeanDefinitionsForConfigurationClass(configClass, trackedConditionEvaluator);
		}
	}

	/**
	 * Read a particular {@link ConfigurationClass}, registering bean definitions
	 * for the class itself and all of its {@link Bean} methods.
	 * --
	 * 读取特定的 {@link ConfigurationClass}，为该类本身及其所有 {@link Bean} 方法注册 bean 定义。
	 */
	private void loadBeanDefinitionsForConfigurationClass(
			ConfigurationClass configClass, TrackedConditionEvaluator trackedConditionEvaluator) {

		// 1. 如果该配置类被跳过，则从注册表里移除它
		if (trackedConditionEvaluator.shouldSkip(configClass)) {
			String beanName = configClass.getBeanName();
			// 包含该bean定义则从注册表里移除它
			if (StringUtils.hasLength(beanName) && this.registry.containsBeanDefinition(beanName)) {
				this.registry.removeBeanDefinition(beanName);
			}
			// 从 importRegistry 中移除它
			this.importRegistry.removeImportingClass(configClass.getMetadata().getClassName());
			return;
		}

		// 2. 如果该配置类是被其他配置类导入的，则注册该配置类为bean定义
		if (configClass.isImported()) {
			registerBeanDefinitionForImportedConfigurationClass(configClass);
		}

		for (BeanMethod beanMethod : configClass.getBeanMethods()) {
			// 3. 从@Configuration标记的类中读取@Bean标记的方法，并生成ConfigurationClassBeanDefinition类型的BeanDefinition
			loadBeanDefinitionsForBeanMethod(beanMethod);
		}

		// 4. 通过解析@ImportResource注解引入的资源文件，获取到BeanDefinition 并注册
		loadBeanDefinitionsFromImportedResources(configClass.getImportedResources());
		// 5. 调用registerBeanDefinitions方法注册bean定义
		loadBeanDefinitionsFromRegistrars(configClass.getImportBeanDefinitionRegistrars());
	}

	/**
	 * Register the {@link Configuration} class itself as a bean definition.
	 * --
	 * 将 {@link Configuration} 类本身注册为 bean 定义。
	 */
	private void registerBeanDefinitionForImportedConfigurationClass(ConfigurationClass configClass) {
		AnnotationMetadata metadata = configClass.getMetadata();
		AnnotatedGenericBeanDefinition configBeanDef = new AnnotatedGenericBeanDefinition(metadata);
		// 解析bean的作用域
		ScopeMetadata scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(configBeanDef);
		configBeanDef.setScope(scopeMetadata.getScopeName());

		// 生成bean名称
		String configBeanName = this.importBeanNameGenerator.generateBeanName(configBeanDef, this.registry);
		AnnotationConfigUtils.processCommonDefinitionAnnotations(configBeanDef, metadata);

		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(configBeanDef, configBeanName);

		// TODO
		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);

		// 注册该bean定义
		this.registry.registerBeanDefinition(definitionHolder.getBeanName(), definitionHolder.getBeanDefinition());
		configClass.setBeanName(configBeanName);

		if (logger.isTraceEnabled()) {
			logger.trace("Registered bean definition for imported class '" + configBeanName + "'");
		}
	}

	/**
	 * Read the given {@link BeanMethod}, registering bean definitions
	 * with the BeanDefinitionRegistry based on its contents.
	 * --
	 * 读取给定的 {@link BeanMethod}，根据其内容向 BeanDefinitionRegistry 注册 bean 定义。
	 */
	@SuppressWarnings("deprecation")  // for RequiredAnnotationBeanPostProcessor.SKIP_REQUIRED_CHECK_ATTRIBUTE
	private void loadBeanDefinitionsForBeanMethod(BeanMethod beanMethod) {
		ConfigurationClass configClass = beanMethod.getConfigurationClass();
		MethodMetadata metadata = beanMethod.getMetadata();
		String methodName = metadata.getMethodName();

		// Do we need to mark the bean as skipped by its condition?
		// 我们是否需要将 Bean 标记为其条件已跳过？
		// 1. 该@Bean方法被逃过，直接返回
		if (this.conditionEvaluator.shouldSkip(metadata, ConfigurationPhase.REGISTER_BEAN)) {
			configClass.skippedBeanMethods.add(methodName);
			return;
		}

		// 2. 缓存中该@Bean方法被标记为跳过，直接返回
		if (configClass.skippedBeanMethods.contains(methodName)) {
			return;
		}

		// 获取@Bean注解的所有属性集
		AnnotationAttributes bean = AnnotationConfigUtils.attributesFor(metadata, Bean.class);
		Assert.state(bean != null, "No @Bean annotation attributes");

		// Consider name and any aliases
		// 考虑姓名和任何别名
		List<String> names = new ArrayList<>(Arrays.asList(bean.getStringArray("name")));
		// beanName = name属性不为空则获取第一个，否则就是方法名称
		String beanName = (!names.isEmpty() ? names.remove(0) : methodName);

		// Register aliases even when overridden
		// 即使被覆盖也注册别名
		for (String alias : names) {
			this.registry.registerAlias(beanName, alias);
		}

		// Has this effectively been overridden before (e.g. via XML)?
		// 之前是否已有效地覆盖此设置（例如通过 XML）？
		// 判断是否已经被定义过
		if (isOverriddenByExistingDefinition(beanMethod, beanName)) {
			if (beanName.equals(beanMethod.getConfigurationClass().getBeanName())) {
				// @Bean 的beanName和它的配置类的beanName 一样，抛出异常
				throw new BeanDefinitionStoreException(beanMethod.getConfigurationClass().getResource().getDescription(),
						beanName, "Bean name derived from @Bean method '" + beanMethod.getMetadata().getMethodName() +
						"' clashes with bean name for containing configuration class; please make those names unique!");
			}
			return;
		}

		ConfigurationClassBeanDefinition beanDef = new ConfigurationClassBeanDefinition(configClass, metadata, beanName);
		beanDef.setSource(this.sourceExtractor.extractSource(metadata, configClass.getResource()));

		// 设置bean定义的唯一的工厂方法
		// 1. 如果它是静态方法，则设置它的beanClass为声明它的配置类
		// 2. 如果它是静态方法，则设置它的FactoryBeanName为声明它的配置类
		if (metadata.isStatic()) {
			// static @Bean method
			if (configClass.getMetadata() instanceof StandardAnnotationMetadata) {
				beanDef.setBeanClass(((StandardAnnotationMetadata) configClass.getMetadata()).getIntrospectedClass());
			}
			else {
				beanDef.setBeanClassName(configClass.getMetadata().getClassName());
			}
			beanDef.setUniqueFactoryMethodName(methodName);
		}
		else {
			// instance @Bean method
			beanDef.setFactoryBeanName(configClass.getBeanName());
			beanDef.setUniqueFactoryMethodName(methodName);
		}

		// 设置它的factoryMethodToIntrospect
		if (metadata instanceof StandardMethodMetadata) {
			beanDef.setResolvedFactoryMethod(((StandardMethodMetadata) metadata).getIntrospectedMethod());
		}

		// 设置它的autowireMode = AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR
		beanDef.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);

		// 设置它的skipRequiredCheck = true
		// 设置跳过属性检查
		beanDef.setAttribute(org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor.
				SKIP_REQUIRED_CHECK_ATTRIBUTE, Boolean.TRUE);

		// 处理通用的注解： @Lazy、@Primary、@DependsOn、@Role、@Description。设置到 BeanDefinition 中
		AnnotationConfigUtils.processCommonDefinitionAnnotations(beanDef, metadata);

		Autowire autowire = bean.getEnum("autowire");
		// 如果 BY_NAME || BY_TYPE，则重新设置
		if (autowire.isAutowire()) {
			beanDef.setAutowireMode(autowire.value());
		}

		boolean autowireCandidate = bean.getBoolean("autowireCandidate");
		if (!autowireCandidate) {
			beanDef.setAutowireCandidate(false);
		}

		// 设置初始化方法
		String initMethodName = bean.getString("initMethod");
		if (StringUtils.hasText(initMethodName)) {
			beanDef.setInitMethodName(initMethodName);
		}

		// 设置销毁方法
		String destroyMethodName = bean.getString("destroyMethod");
		beanDef.setDestroyMethodName(destroyMethodName);

		// Consider scoping
		// 处理方法上的 @Scope 注解
		ScopedProxyMode proxyMode = ScopedProxyMode.NO;
		AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(metadata, Scope.class);
		if (attributes != null) {
			beanDef.setScope(attributes.getString("value"));
			proxyMode = attributes.getEnum("proxyMode");
			if (proxyMode == ScopedProxyMode.DEFAULT) {
				proxyMode = ScopedProxyMode.NO;
			}
		}

		// Replace the original bean definition with the target one, if necessary
		// 如有必要，将原始 bean 定义替换为目标 bean 定义
		BeanDefinition beanDefToRegister = beanDef;
		if (proxyMode != ScopedProxyMode.NO) {
			BeanDefinitionHolder proxyDef = ScopedProxyCreator.createScopedProxy(
					new BeanDefinitionHolder(beanDef, beanName), this.registry,
					proxyMode == ScopedProxyMode.TARGET_CLASS);
			// 生成作用域代理bean定义
			beanDefToRegister = new ConfigurationClassBeanDefinition(
					(RootBeanDefinition) proxyDef.getBeanDefinition(), configClass, metadata, beanName);
		}

		if (logger.isTraceEnabled()) {
			logger.trace(String.format("Registering bean definition for @Bean method %s.%s()",
					configClass.getMetadata().getClassName(), beanName));
		}

		// 注册BeanDefinition
		this.registry.registerBeanDefinition(beanName, beanDefToRegister);
	}

	protected boolean isOverriddenByExistingDefinition(BeanMethod beanMethod, String beanName) {
		if (!this.registry.containsBeanDefinition(beanName)) {
			return false;
		}
		// 注册表中已存在该bean定义了
		BeanDefinition existingBeanDef = this.registry.getBeanDefinition(beanName);

		// Is the existing bean definition one that was created from a configuration class?
		// -> allow the current bean method to override, since both are at second-pass level.
		// However, if the bean method is an overloaded case on the same configuration class,
		// preserve the existing bean definition.
		if (existingBeanDef instanceof ConfigurationClassBeanDefinition) {
			ConfigurationClassBeanDefinition ccbd = (ConfigurationClassBeanDefinition) existingBeanDef;
			// 配置类相同 && @Bean 工厂方法名称相同，则设置它的工厂方法不唯一
			if (ccbd.getMetadata().getClassName().equals(
					beanMethod.getConfigurationClass().getMetadata().getClassName())) {
				if (ccbd.getFactoryMethodMetadata().getMethodName().equals(ccbd.getFactoryMethodName())) {
					ccbd.setNonUniqueFactoryMethodName(ccbd.getFactoryMethodMetadata().getMethodName());
				}
				return true;
			}
			else {
				return false;
			}
		}

		// A bean definition resulting from a component scan can be silently overridden
		// by an @Bean method, as of 4.2...
		// 从 4.2 开始，组件扫描生成的 Bean 定义可以被 @Bean 方法静默覆盖......
		if (existingBeanDef instanceof ScannedGenericBeanDefinition) {
			return false;
		}

		// Has the existing bean definition bean marked as a framework-generated bean?
		// -> allow the current bean method to override it, since it is application-level
		// 现有的 Bean 定义 Bean 是否标记为框架生成的 Bean？->允许当前的 Bean 方法覆盖它，因为它是应用程序级的
		if (existingBeanDef.getRole() > BeanDefinition.ROLE_APPLICATION) {
			return false;
		}

		// At this point, it's a top-level override (probably XML), just having been parsed
		// before configuration class processing kicks in...
		// 在这一点上，它是一个顶级覆盖（可能是 XML），只是在配置类处理开始之前被解析......
		if (this.registry instanceof DefaultListableBeanFactory &&
				!((DefaultListableBeanFactory) this.registry).isAllowBeanDefinitionOverriding()) {
			throw new BeanDefinitionStoreException(beanMethod.getConfigurationClass().getResource().getDescription(),
					beanName, "@Bean definition illegally overridden by existing bean definition: " + existingBeanDef);
		}
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Skipping bean definition for %s: a definition for bean '%s' " +
					"already exists. This top-level bean definition is considered as an override.",
					beanMethod, beanName));
		}
		return true;
	}

	/**
	 * 从导入的资源加载Bean定义
	 * 即通过解析@ImportResource注解引入的资源文件，获取到BeanDefinition 并注册
	 *
	 * @param importedResources
	 */
	private void loadBeanDefinitionsFromImportedResources(
			Map<String, Class<? extends BeanDefinitionReader>> importedResources) {

		/**
		 * BeanDefinitionReader类 -> 初始化之后的BeanDefinitionReader类对象
		 */
		Map<Class<?>, BeanDefinitionReader> readerInstanceCache = new HashMap<>();

		importedResources.forEach((resource, readerClass) -> {
			// Default reader selection necessary?

			// 初始化BeanDefinitionReader

			// 是否需要选择默认读取器？
			if (BeanDefinitionReader.class == readerClass) {
				if (StringUtils.endsWithIgnoreCase(resource, ".groovy")) {
					// When clearly asking for Groovy, that's what they'll get...
					// 当明确要求 Groovy 时，这就是他们会得到的......
					readerClass = GroovyBeanDefinitionReader.class;
				}
				else if (shouldIgnoreXml) {
					throw new UnsupportedOperationException("XML support disabled");
				}
				else {
					// Primarily ".xml" files but for any other extension as well
					// 主要是“.xml”文件，但也适用于任何其他扩展名
					readerClass = XmlBeanDefinitionReader.class;
				}
			}

			BeanDefinitionReader reader = readerInstanceCache.get(readerClass);
			if (reader == null) {
				try {
					// Instantiate the specified BeanDefinitionReader
					// 实例化指定的 BeanDefinitionReader
					reader = readerClass.getConstructor(BeanDefinitionRegistry.class).newInstance(this.registry);
					// Delegate the current ResourceLoader to it if possible
					// 如果可能，将当前 ResourceLoader 委托给它
					if (reader instanceof AbstractBeanDefinitionReader) {
						AbstractBeanDefinitionReader abdr = ((AbstractBeanDefinitionReader) reader);
						abdr.setResourceLoader(this.resourceLoader);
						abdr.setEnvironment(this.environment);
					}
					readerInstanceCache.put(readerClass, reader);
				}
				catch (Throwable ex) {
					throw new IllegalStateException(
							"Could not instantiate BeanDefinitionReader class [" + readerClass.getName() + "]");
				}
			}

			// TODO SPR-6310: qualify relative path locations as done in AbstractContextLoader.modifyLocations
			// SPR-6310：按照 AbstractContextLoader.modifyLocations 中的规定限定相对路径位置
			reader.loadBeanDefinitions(resource);
		});
	}

	/**
	 * 调用registerBeanDefinitions方法注册bean定义
	 * @param registrars
	 */
	private void loadBeanDefinitionsFromRegistrars(Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> registrars) {
		registrars.forEach((registrar, metadata) ->
				registrar.registerBeanDefinitions(metadata, this.registry, this.importBeanNameGenerator));
	}


	/**
	 * {@link RootBeanDefinition} marker subclass used to signify that a bean definition
	 * was created from a configuration class as opposed to any other configuration source.
	 * Used in bean overriding cases where it's necessary to determine whether the bean
	 * definition was created externally.
	 *
	 * {@link RootBeanDefinition} 标记子类，用于表示 Bean 定义是从配置类创建的，而不是从任何其他配置源创建的。用于需要确定 Bean 定义是否在外部创建的 Bean 覆盖情况。
	 *
	 */
	@SuppressWarnings("serial")
	private static class ConfigurationClassBeanDefinition extends RootBeanDefinition implements AnnotatedBeanDefinition {
		/**
		 * 配置类的元数据
		 */
		private final AnnotationMetadata annotationMetadata;

		/**
		 * -- @Bean 方法元数据
		 */
		private final MethodMetadata factoryMethodMetadata;

		private final String derivedBeanName;

		public ConfigurationClassBeanDefinition(
				ConfigurationClass configClass, MethodMetadata beanMethodMetadata, String derivedBeanName) {

			this.annotationMetadata = configClass.getMetadata();
			this.factoryMethodMetadata = beanMethodMetadata;
			this.derivedBeanName = derivedBeanName;
			setResource(configClass.getResource());
			setLenientConstructorResolution(false);
		}

		public ConfigurationClassBeanDefinition(RootBeanDefinition original,
				ConfigurationClass configClass, MethodMetadata beanMethodMetadata, String derivedBeanName) {

			super(original);
			this.annotationMetadata = configClass.getMetadata();
			this.factoryMethodMetadata = beanMethodMetadata;
			this.derivedBeanName = derivedBeanName;
		}

		private ConfigurationClassBeanDefinition(ConfigurationClassBeanDefinition original) {
			super(original);
			this.annotationMetadata = original.annotationMetadata;
			this.factoryMethodMetadata = original.factoryMethodMetadata;
			this.derivedBeanName = original.derivedBeanName;
		}

		@Override
		public AnnotationMetadata getMetadata() {
			return this.annotationMetadata;
		}

		@Override
		@NonNull
		public MethodMetadata getFactoryMethodMetadata() {
			return this.factoryMethodMetadata;
		}

		@Override
		public boolean isFactoryMethod(Method candidate) {
			return (super.isFactoryMethod(candidate) && BeanAnnotationHelper.isBeanAnnotated(candidate) &&
					BeanAnnotationHelper.determineBeanNameFor(candidate).equals(this.derivedBeanName));
		}

		@Override
		public ConfigurationClassBeanDefinition cloneBeanDefinition() {
			return new ConfigurationClassBeanDefinition(this);
		}
	}


	/**
	 * Evaluate {@code @Conditional} annotations, tracking results and taking into
	 * account 'imported by'.
	 *
	 * 评估 {@code @Conditional} 注解，跟踪结果并考虑“导入者”。
	 */
	private class TrackedConditionEvaluator {

		/**
		 * 缓存该配置类是否被跳过
		 */
		private final Map<ConfigurationClass, Boolean> skipped = new HashMap<>();

		/**
		 * 判断该配置类是否被跳过
		 * 1. 如果该配置类是被其他的配置类导入的，则 除非导入它的所有配置类都被跳过，则返回true
		 * 2. 否则判断它的@Condition注解条件是否匹配
		 * @param configClass
		 * @return
		 */
		public boolean shouldSkip(ConfigurationClass configClass) {
			Boolean skip = this.skipped.get(configClass);
			if (skip == null) {
				// 该配置类是被引入的（被 @Import 或者其他配置类引入），自身注入方式优先
				if (configClass.isImported()) {

					// 引入该配置类的配置类，有一个没有被跳过，说明引入该配置的所有配置类没有全部跳过
					boolean allSkipped = true;
					for (ConfigurationClass importedBy : configClass.getImportedBy()) {
						// 引入该配置类的配置类 不是跳过的，则 allSkipped = false
						if (!shouldSkip(importedBy)) {
							allSkipped = false;
							break;
						}
					}
					if (allSkipped) {
						// The config classes that imported this one were all skipped, therefore we are skipped...
						// 导入这个的配置类全部被跳过，因此我们被跳过......
						skip = true;
					}
				}
				if (skip == null) {
					skip = conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN);
				}
				this.skipped.put(configClass, skip);
			}
			return skip;
		}
	}

}
