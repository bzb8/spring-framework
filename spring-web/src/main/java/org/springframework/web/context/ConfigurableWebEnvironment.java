/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.context;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.lang.Nullable;

/**
 * Specialization of {@link ConfigurableEnvironment} allowing initialization of
 * servlet-related {@link org.springframework.core.env.PropertySource} objects at the
 * earliest moment that the {@link ServletContext} and (optionally) {@link ServletConfig}
 * become available.
 *
 * @author Chris Beams
 * @since 3.1.2
 * @see ConfigurableWebApplicationContext#getEnvironment()
 */
/**
 * {@link ConfigurableEnvironment}接口的特化版，允许在{@link ServletContext}和（可选地）{@link ServletConfig}
 * 变得可用的最早时刻初始化与Servlet相关的{@link org.springframework.core.env.PropertySource}对象。
 *
 * @author Chris Beams 表示该类的作者是Chris Beams。
 * @since 3.1.2 表示该类是从3.1.2版本开始提供的。
 * @see ConfigurableWebApplicationContext#getEnvironment() 提供了一个相关方法的引用说明。
 */

public interface ConfigurableWebEnvironment extends ConfigurableEnvironment {

	/**
	 * Replace any {@linkplain
	 * org.springframework.core.env.PropertySource.StubPropertySource stub property source}
	 * instances acting as placeholders with real servlet context/config property sources
	 * using the given parameters.
	 * @param servletContext the {@link ServletContext} (may not be {@code null})
	 * @param servletConfig the {@link ServletConfig} ({@code null} if not available)
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources(
	 * org.springframework.core.env.MutablePropertySources, ServletContext, ServletConfig)
	 */
	/**
	 * 使用给定的参数，将作为占位符的任何{@linkplain
	 * org.springframework.core.env.PropertySource.StubPropertySource stub property source}实例替换为真实的servlet上下文/配置属性源。
	 *
	 * @param servletContext the {@link ServletContext} (may not be {@code null})，表示servlet上下文，不能为空。
	 * @param servletConfig the {@link ServletConfig} ({@code null} if not available)，表示servlet配置，如果不可用可以为null。
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources(
	 * org.springframework.core.env.MutablePropertySources, ServletContext, ServletConfig)，
	 * 参考WebApplicationContextUtils类中的initServletPropertySources方法，用于初始化servlet属性源。
	 */
	void initPropertySources(@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig);

}
