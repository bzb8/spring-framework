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

package org.springframework.web.context.support;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link org.springframework.context.support.AbstractRefreshableApplicationContext}
 * subclass which implements the
 * {@link org.springframework.web.context.ConfigurableWebApplicationContext}
 * interface for web environments. Provides a "configLocations" property,
 * to be populated through the ConfigurableWebApplicationContext interface
 * on web application startup.
 * <p>AbstractRefreshableWebApplicationContext 是一个抽象类，继承自 AbstractRefreshableConfigApplicationContext，
 * 为web环境提供了ConfigurableWebApplicationContext接口的实现。它提供了"configLocations"属性，
 * 该属性可以通过ConfigurableWebApplicationContext接口在web应用程序启动时进行配置。
 *
 * <p>This class is as easy to subclass as AbstractRefreshableApplicationContext:
 * All you need to implement is the {@link #loadBeanDefinitions} method;
 * see the superclass javadoc for details. Note that implementations are supposed
 * to load bean definitions from the files specified by the locations returned
 * by the {@link #getConfigLocations} method.
 * 这个类和AbstractRefreshableApplicationContext一样容易进行子类化：你只需要实现 {@link #loadBeanDefinitions} 方法；
 * 有关详情，请参阅超类的javadoc。需要注意的是，实现类应该从{@link #getConfigLocations} 方法返回的位置加载 bean 定义。
 *
 * <p>Interprets resource paths as servlet context resources, i.e. as paths beneath
 * the web application root. Absolute paths, e.g. for files outside the web app root,
 * can be accessed via "file:" URLs, as implemented by
 * {@link org.springframework.core.io.DefaultResourceLoader}.
 * 该类将资源路径解释为servlet上下文资源，即web应用程序根目录下的路径。可以通过"file:" URL访问web应用根目录外的绝对路径，
 * 如 {@link org.springframework.core.io.DefaultResourceLoader} 所实现。
 *
 * <p>In addition to the special beans detected by
 * {@link org.springframework.context.support.AbstractApplicationContext},
 * this class detects a bean of type {@link org.springframework.ui.context.ThemeSource}
 * in the context, under the special bean name "themeSource".
 * <p>除了 {@link org.springframework.context.support.AbstractApplicationContext} 检测到的特殊bean外，
 * 该类还检测到上下文中类型为 {@link org.springframework.ui.context.ThemeSource} 的bean，
 * 该bean的特殊名称为"themeSource"。
 *
 * <p><b>This is the web context to be subclassed for a different bean definition format.</b>
 * Such a context implementation can be specified as "contextClass" context-param
 * for {@link org.springframework.web.context.ContextLoader} or as "contextClass"
 * init-param for {@link org.springframework.web.servlet.FrameworkServlet},
 * replacing the default {@link XmlWebApplicationContext}. It will then automatically
 * receive the "contextConfigLocation" context-param or init-param, respectively.
 * <b>这是为不同bean定义格式子类化的web上下文。</b>这样的上下文实现可以作为"contextClass" context-param
 * 为 {@link org.springframework.web.context.ContextLoader} 或作为"contextClass" init-param
 * 为 {@link org.springframework.web.servlet.FrameworkServlet} 指定，以替换默认的 {@link XmlWebApplicationContext}。
 * 然后，它将自动接收"contextConfigLocation" context-param 或 init-param，分别。
 *
 * <p>Note that WebApplicationContext implementations are generally supposed
 * to configure themselves based on the configuration received through the
 * {@link ConfigurableWebApplicationContext} interface. In contrast, a standalone
 * application context might allow for configuration in custom startup code
 * (for example, {@link org.springframework.context.support.GenericApplicationContext}).
 * 需要注意的是，WebApplicationContext的实现通常应该基于通过 {@link ConfigurableWebApplicationContext} 接口接收的配置进行配置。
 *  相反，一个独立的应用上下文可能允许在自定义启动代码中进行配置（例如，{@link org.springframework.context.support.GenericApplicationContext}）。
 * @author Juergen Hoeller
 * @since 1.1.3
 * @see #loadBeanDefinitions
 * @see org.springframework.web.context.ConfigurableWebApplicationContext#setConfigLocations
 * @see org.springframework.ui.context.ThemeSource
 * @see XmlWebApplicationContext
 */
public abstract class AbstractRefreshableWebApplicationContext extends AbstractRefreshableConfigApplicationContext
		implements ConfigurableWebApplicationContext, ThemeSource {

	/** Servlet context that this context runs in. */
	@Nullable
	private ServletContext servletContext;

	/** Servlet config that this context runs in, if any. */
	@Nullable
	private ServletConfig servletConfig;

	/** Namespace of this context, or {@code null} if root. */
	@Nullable
	private String namespace;

	/** the ThemeSource for this ApplicationContext. */
	@Nullable
	private ThemeSource themeSource;


	public AbstractRefreshableWebApplicationContext() {
		setDisplayName("Root WebApplicationContext");
	}


	@Override
	public void setServletContext(@Nullable ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	@Nullable
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public void setServletConfig(@Nullable ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
		if (servletConfig != null && this.servletContext == null) {
			setServletContext(servletConfig.getServletContext());
		}
	}

	@Override
	@Nullable
	public ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	@Override
	public void setNamespace(@Nullable String namespace) {
		this.namespace = namespace;
		if (namespace != null) {
			setDisplayName("WebApplicationContext for namespace '" + namespace + "'");
		}
	}

	@Override
	@Nullable
	public String getNamespace() {
		return this.namespace;
	}

	@Override
	public String[] getConfigLocations() {
		return super.getConfigLocations();
	}

	@Override
	public String getApplicationName() {
		return (this.servletContext != null ? this.servletContext.getContextPath() : "");
	}

	/**
	 * Create and return a new {@link StandardServletEnvironment}. Subclasses may override
	 * in order to configure the environment or specialize the environment type returned.
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * Register request/session scopes, a {@link ServletContextAwareProcessor}, etc.
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext, this.servletConfig));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		beanFactory.ignoreDependencyInterface(ServletConfigAware.class);

		WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
		WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext, this.servletConfig);
	}

	/**
	 * This implementation supports file paths beneath the root of the ServletContext.
	 * @see ServletContextResource
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		Assert.state(this.servletContext != null, "No ServletContext available");
		return new ServletContextResource(this.servletContext, path);
	}

	/**
	 * This implementation supports pattern matching in unexpanded WARs too.
	 * @see ServletContextResourcePatternResolver
	 */
	@Override
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new ServletContextResourcePatternResolver(this);
	}

	/**
	 * Initialize the theme capability.
	 */
	@Override
	protected void onRefresh() {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	/**
	 * {@inheritDoc}
	 * <p>Replace {@code Servlet}-related property sources.
	 */
	@Override
	protected void initPropertySources() {
		ConfigurableEnvironment env = getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(this.servletContext, this.servletConfig);
		}
	}

	@Override
	@Nullable
	public Theme getTheme(String themeName) {
		Assert.state(this.themeSource != null, "No ThemeSource available");
		return this.themeSource.getTheme(themeName);
	}

}
