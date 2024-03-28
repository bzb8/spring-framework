/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.context.support;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoader;

/**
 * {@link org.springframework.web.context.WebApplicationContext WebApplicationContext}
 * implementation which accepts <em>component classes</em> as input &mdash; in particular
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * classes, but also plain {@link org.springframework.stereotype.Component @Component}
 * classes as well as JSR-330 compliant classes using {@code javax.inject} annotations.
 * <p>这是一个实现{@link org.springframework.web.context.WebApplicationContext}接口的类，
 * 它接受<em>组件类</em>作为输入——特别是使用了{@link org.springframework.context.annotation.Configuration @Configuration}注解的类，
 * 但也包括了使用了{@link org.springframework.stereotype.Component @Component}注解的普通类以及符合JSR-330标准、使用了{@code javax.inject}注解的类。
 *
 * <p>Allows for registering classes one by one (specifying class names as config
 * locations) as well as via classpath scanning (specifying base packages as config
 * locations).
 * <p>该类允许以逐个注册的方式（通过指定类名作为配置位置）以及通过类路径扫描的方式（通过指定基础包作为配置位置）来注册类。
 *
 * <p>This is essentially the equivalent of
 * {@link org.springframework.context.annotation.AnnotationConfigApplicationContext
 * AnnotationConfigApplicationContext} for a web environment. However, in contrast to
 * {@code AnnotationConfigApplicationContext}, this class does not extend
 * {@link org.springframework.context.support.GenericApplicationContext
 * GenericApplicationContext} and therefore does not provide some of the convenient
 * {@code registerBean(...)} methods available in a {@code GenericApplicationContext}.
 * If you wish to register annotated <em>component classes</em> with a
 * {@code GenericApplicationContext} in a web environment, you may use a
 * {@code GenericWebApplicationContext} with an
 * {@link org.springframework.context.annotation.AnnotatedBeanDefinitionReader
 * AnnotatedBeanDefinitionReader}. See the Javadoc for {@link GenericWebApplicationContext}
 * for details and an example.
 * <p>它基本上等同于非Web环境下的{@link org.springframework.context.annotation.AnnotationConfigApplicationContext AnnotationConfigApplicationContext}。
 * 但是，与{@code AnnotationConfigApplicationContext}不同的是，这个类并不扩展{@link org.springframework.context.support.GenericApplicationContext GenericApplicationContext}，
 * 因此不提供{@code GenericApplicationContext}中一些可用的便捷的{@code registerBean(...)}方法。
 * 如果希望在Web环境中使用{@code GenericApplicationContext}来注册注解的<em>组件类</em>，可以使用{@code GenericWebApplicationContext}和{@link org.springframework.context.annotation.AnnotatedBeanDefinitionReader AnnotatedBeanDefinitionReader}。
 * 有关详情和示例，请参阅{@link GenericWebApplicationContext}的Javadoc。
 *
 * <p>To make use of this application context, the
 * {@linkplain ContextLoader#CONTEXT_CLASS_PARAM "contextClass"} context-param for
 * ContextLoader and/or "contextClass" init-param for FrameworkServlet must be set to
 * the fully-qualified name of this class.
 * <p>要使用这个应用上下文，必须为ContextLoader和/or FrameworkServlet设置
 * {@linkplain ContextLoader#CONTEXT_CLASS_PARAM "contextClass"}上下文参数或"contextClass"初始化参数。
 *
 * <p>As of Spring 3.1, this class may also be directly instantiated and injected into
 * Spring's {@code DispatcherServlet} or {@code ContextLoaderListener} when using the
 * {@link org.springframework.web.WebApplicationInitializer WebApplicationInitializer}
 * code-based alternative to {@code web.xml}. See its Javadoc for details and usage examples.
 * <p>自Spring 3.1起，该类还可以直接实例化并注入到Spring的{@code DispatcherServlet}或{@code ContextLoaderListener}中，
 * 当使用基于代码的{@link org.springframework.web.WebApplicationInitializer WebApplicationInitializer}代替{@code web.xml}时。
 * 有关详情和用法示例，请参阅{@link WebApplicationInitializer}的Javadoc。
 *
 * <p>Unlike {@link XmlWebApplicationContext}, no default configuration class locations
 * are assumed. Rather, it is a requirement to set the
 * {@linkplain ContextLoader#CONFIG_LOCATION_PARAM "contextConfigLocation"}
 * context-param for {@link ContextLoader} and/or "contextConfigLocation" init-param for
 * FrameworkServlet.  The param-value may contain both fully-qualified
 * class names and base packages to scan for components. See {@link #loadBeanDefinitions}
 * for exact details on how these locations are processed.
 * <p>与{@link XmlWebApplicationContext}不同的是，该类没有默认的配置类位置。
 * 相反，设置{@linkplain ContextLoader#CONFIG_LOCATION_PARAM "contextConfigLocation"}上下文参数和/or FrameworkServlet的"contextConfigLocation"初始化参数是必需的。
 * 参数值可以包含要扫描组件的完全限定类名和基础包。有关这些位置如何处理的详细信息，请参阅{@link #loadBeanDefinitions}。
 *
 * <p>As an alternative to setting the "contextConfigLocation" parameter, users may
 * implement an {@link org.springframework.context.ApplicationContextInitializer
 * ApplicationContextInitializer} and set the
 * {@linkplain ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM "contextInitializerClasses"}
 * context-param / init-param. In such cases, users should favor the {@link #refresh()}
 * and {@link #scan(String...)} methods over the {@link #setConfigLocation(String)}
 * method, which is primarily for use by {@code ContextLoader}.
 * <p>作为设置"contextConfigLocation"参数的替代方案，用户可以实现{@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}，
 * 并设置{@linkplain ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM "contextInitializerClasses"}上下文参数/初始化参数。
 * 在这种情况下，用户应该更倾向于使用{@link #refresh()}和{@link #scan(String...)}方法，而不是主要为{@code ContextLoader}使用的{@link #setConfigLocation(String)}方法。
 *
 * <p>Note: In case of multiple {@code @Configuration} classes, later {@code @Bean}
 * definitions will override ones defined in earlier loaded files. This can be leveraged
 * to deliberately override certain bean definitions via an extra {@code @Configuration}
 * class.
 * <p>注意：在存在多个{@code @Configuration}类的情况下，后加载的{@code @Bean}定义将覆盖前面加载文件中定义的{@code @Bean}。
 * 这可以通过额外的{@code @Configuration}类来故意覆盖某些{@code @Bean}定义来利用。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 * @see org.springframework.web.context.support.GenericWebApplicationContext
 */
public class AnnotationConfigWebApplicationContext extends AbstractRefreshableWebApplicationContext
		implements AnnotationConfigRegistry {

	@Nullable
	private BeanNameGenerator beanNameGenerator;

	@Nullable
	private ScopeMetadataResolver scopeMetadataResolver;

	/**
	 * register(Class<?>... componentClasses)方法注册的组件类
	 */
	private final Set<Class<?>> componentClasses = new LinkedHashSet<>();

	/**
	 * 待扫描的表
	 * scan()方法赋值
	 */
	private final Set<String> basePackages = new LinkedHashSet<>();


	/**
	 * Set a custom {@link BeanNameGenerator} for use with {@link AnnotatedBeanDefinitionReader}
	 * and/or {@link ClassPathBeanDefinitionScanner}.
	 * <p>Default is {@link org.springframework.context.annotation.AnnotationBeanNameGenerator}.
	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = beanNameGenerator;
	}

	/**
	 * Return the custom {@link BeanNameGenerator} for use with {@link AnnotatedBeanDefinitionReader}
	 * and/or {@link ClassPathBeanDefinitionScanner}, if any.
	 */
	@Nullable
	protected BeanNameGenerator getBeanNameGenerator() {
		return this.beanNameGenerator;
	}

	/**
	 * Set a custom {@link ScopeMetadataResolver} for use with {@link AnnotatedBeanDefinitionReader}
	 * and/or {@link ClassPathBeanDefinitionScanner}.
	 * <p>Default is an {@link org.springframework.context.annotation.AnnotationScopeMetadataResolver}.
	 * @see AnnotatedBeanDefinitionReader#setScopeMetadataResolver
	 * @see ClassPathBeanDefinitionScanner#setScopeMetadataResolver
	 */
	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver = scopeMetadataResolver;
	}

	/**
	 * Return the custom {@link ScopeMetadataResolver} for use with {@link AnnotatedBeanDefinitionReader}
	 * and/or {@link ClassPathBeanDefinitionScanner}, if any.
	 */
	@Nullable
	protected ScopeMetadataResolver getScopeMetadataResolver() {
		return this.scopeMetadataResolver;
	}


	/**
	 * Register one or more component classes to be processed.
	 * <p>Note that {@link #refresh()} must be called in order for the context
	 * to fully process the new classes.
	 * <p>注册一个或多个组件类以进行处理。
	 * <p>请注意，必须调用{@link #refresh()}方法，才能使上下文完全处理这些新类。
	 * @param componentClasses one or more component classes,
	 * e.g. {@link org.springframework.context.annotation.Configuration @Configuration} classes
	 *                         一个或多个组件类，例如标注了{@link org.springframework.context.annotation.Configuration @Configuration}的类
	 * @see #scan(String...)
	 * @see #loadBeanDefinitions(DefaultListableBeanFactory)
	 * @see #setConfigLocation(String)
	 * @see #refresh()
	 */
	@Override
	public void register(Class<?>... componentClasses) {
		Assert.notEmpty(componentClasses, "At least one component class must be specified");
		Collections.addAll(this.componentClasses, componentClasses);
	}

	/**
	 * Perform a scan within the specified base packages.
	 * <p>Note that {@link #refresh()} must be called in order for the context
	 * to fully process the new classes.
	 * <p>在指定的基本包内进行扫描。
	 * <p>注意，必须调用{@link #refresh()}方法，才能使上下文完全处理新类。
	 *
	 * @param basePackages the packages to check for component classes
	 * @see #loadBeanDefinitions(DefaultListableBeanFactory)
	 * @see #register(Class...)
	 * @see #setConfigLocation(String)
	 * @see #refresh()
	 */
	@Override
	public void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		Collections.addAll(this.basePackages, basePackages);
	}


	/**
	 * Register a {@link org.springframework.beans.factory.config.BeanDefinition} for
	 * any classes specified by {@link #register(Class...)} and scan any packages
	 * specified by {@link #scan(String...)}.
	 * <p>For any values specified by {@link #setConfigLocation(String)} or
	 * {@link #setConfigLocations(String[])}, attempt first to load each location as a
	 * class, registering a {@code BeanDefinition} if class loading is successful,
	 * and if class loading fails (i.e. a {@code ClassNotFoundException} is raised),
	 * assume the value is a package and attempt to scan it for component classes.
	 * <p>Enables the default set of annotation configuration post processors, such that
	 * {@code @Autowired}, {@code @Required}, and associated annotations can be used.
	 * <p>Configuration class bean definitions are registered with generated bean
	 * definition names unless the {@code value} attribute is provided to the stereotype
	 * annotation.
	 * @param beanFactory the bean factory to load bean definitions into
	 * @see #register(Class...)
	 * @see #scan(String...)
	 * @see #setConfigLocation(String)
	 * @see #setConfigLocations(String[])
	 * @see AnnotatedBeanDefinitionReader
	 * @see ClassPathBeanDefinitionScanner
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
		AnnotatedBeanDefinitionReader reader = getAnnotatedBeanDefinitionReader(beanFactory);
		ClassPathBeanDefinitionScanner scanner = getClassPathBeanDefinitionScanner(beanFactory);

		BeanNameGenerator beanNameGenerator = getBeanNameGenerator();
		if (beanNameGenerator != null) {
			reader.setBeanNameGenerator(beanNameGenerator);
			scanner.setBeanNameGenerator(beanNameGenerator);
			beanFactory.registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
		}

		ScopeMetadataResolver scopeMetadataResolver = getScopeMetadataResolver();
		if (scopeMetadataResolver != null) {
			reader.setScopeMetadataResolver(scopeMetadataResolver);
			scanner.setScopeMetadataResolver(scopeMetadataResolver);
		}

		if (!this.componentClasses.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Registering component classes: [" +
						StringUtils.collectionToCommaDelimitedString(this.componentClasses) + "]");
			}
			reader.register(ClassUtils.toClassArray(this.componentClasses));
		}

		if (!this.basePackages.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Scanning base packages: [" +
						StringUtils.collectionToCommaDelimitedString(this.basePackages) + "]");
			}
			scanner.scan(StringUtils.toStringArray(this.basePackages));
		}

		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (String configLocation : configLocations) {
				try {
					Class<?> clazz = ClassUtils.forName(configLocation, getClassLoader());
					if (logger.isTraceEnabled()) {
						logger.trace("Registering [" + configLocation + "]");
					}
					reader.register(clazz);
				}
				catch (ClassNotFoundException ex) {
					if (logger.isTraceEnabled()) {
						logger.trace("Could not load class for config location [" + configLocation +
								"] - trying package scan. " + ex);
					}
					int count = scanner.scan(configLocation);
					if (count == 0 && logger.isDebugEnabled()) {
						logger.debug("No component classes found for specified class/package [" + configLocation + "]");
					}
				}
			}
		}
	}


	/**
	 * Build an {@link AnnotatedBeanDefinitionReader} for the given bean factory.
	 * <p>This should be pre-configured with the {@code Environment} (if desired)
	 * but not with a {@code BeanNameGenerator} or {@code ScopeMetadataResolver} yet.
	 * @param beanFactory the bean factory to load bean definitions into
	 * @since 4.1.9
	 * @see #getEnvironment()
	 * @see #getBeanNameGenerator()
	 * @see #getScopeMetadataResolver()
	 */
	protected AnnotatedBeanDefinitionReader getAnnotatedBeanDefinitionReader(DefaultListableBeanFactory beanFactory) {
		return new AnnotatedBeanDefinitionReader(beanFactory, getEnvironment());
	}

	/**
	 * Build a {@link ClassPathBeanDefinitionScanner} for the given bean factory.
	 * <p>This should be pre-configured with the {@code Environment} (if desired)
	 * but not with a {@code BeanNameGenerator} or {@code ScopeMetadataResolver} yet.
	 * @param beanFactory the bean factory to load bean definitions into
	 * @since 4.1.9
	 * @see #getEnvironment()
	 * @see #getBeanNameGenerator()
	 * @see #getScopeMetadataResolver()
	 */
	protected ClassPathBeanDefinitionScanner getClassPathBeanDefinitionScanner(DefaultListableBeanFactory beanFactory) {
		return new ClassPathBeanDefinitionScanner(beanFactory, true, getEnvironment());
	}

}
