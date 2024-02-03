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

package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * Strategy interface for loading resources (e.g., class path or file system
 * resources). An {@link org.springframework.context.ApplicationContext}
 * is required to provide this functionality plus extended
 * {@link org.springframework.core.io.support.ResourcePatternResolver} support.
 *
 * 用于加载资源（例如，类路径或文件系统资源）的策略接口。
 * 需要{@link org.springframework.context.ApplicationContext}才能提供此功能
 * 以及扩展的{@link org.springframework.core.io.support.ResourcePatternResolver}支持。
 *
 * <p>{@link DefaultResourceLoader} is a standalone implementation that is·
 * usable outside an ApplicationContext and is also used by {@link ResourceEditor}.
 *
 * <p>{@link DefaultResourceLoader} 是一个独立的实现，可在 ApplicationContext 外部使用，也由 {@link ResourceEditor} 使用
 *
 * <p>Bean properties of type {@code Resource} and {@code Resource[]} can be populated
 * from Strings when running in an ApplicationContext, using the particular
 * context's resource loading strategy.
 *
 *  在 <p>ApplicationContext 中运行时，可以使用特定上下文的资源加载策略从 Strings 填充 {@code Resource} 和 {@code Resource[]} 类型的 Bean 属性。
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see Resource
 * @see org.springframework.core.io.support.ResourcePatternResolver
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 */
public interface ResourceLoader {

	/**
	 * Pseudo URL prefix for loading from the class path: "classpath:".
	 * 用于从类路径加载的伪 URL 前缀：“classpath:”。
	 */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;


	/**
	 * Return a {@code Resource} handle for the specified resource location.
	 * <p>The handle should always be a reusable resource descriptor,
	 * allowing for multiple {@link Resource#getInputStream()} calls.
	 * --
	 * 返回指定资源位置的 {@code Resource} 句柄。
	 * 句柄应始终是可重用的资源描述符，允许多个 {@link Resource#getInputStream()} 调用
	 *
	 * <p><ul>
	 * <li>Must support fully qualified URLs, e.g. "file:C:/test.dat".
	 * <li>Must support classpath pseudo-URLs, e.g. "classpath:test.dat".
	 * <li>Should support relative file paths, e.g. "WEB-INF/test.dat".
	 * (This will be implementation-specific, typically provided by an
	 * ApplicationContext implementation.)
	 * </ul>
	 *
	 * <p><ul> <li>必须支持完全限定的 URL，例如“file：C：test.dat”。<li>必须支持类路径伪 URL，例如“classpath：test.dat”。
	 * <li>应支持相对文件路径，例如“WEB-INFtest.dat”。（这将是特定于实现的，通常由 ApplicationContext 实现提供。
	 *
	 * <p>Note that a {@code Resource} handle does not imply an existing resource;
	 * you need to invoke {@link Resource#exists} to check for existence.
	 *
	 * <p>请注意，{@code Resource} 句柄并不意味着存在资源;您需要调用 {@link Resource#exists} 来检查是否存在。
	 *
	 * @param location the resource location
	 * @return a corresponding {@code Resource} handle (never {@code null})
	 * @see #CLASSPATH_URL_PREFIX
	 * @see Resource#exists()
	 * @see Resource#getInputStream()
	 */
	Resource getResource(String location);

	/**
	 * Expose the {@link ClassLoader} used by this {@code ResourceLoader}.
	 * <p>Clients which need to access the {@code ClassLoader} directly can do so
	 * in a uniform manner with the {@code ResourceLoader}, rather than relying
	 * on the thread context {@code ClassLoader}.
	 *
	 * 公开此 {@code ResourceLoader} 使用的 {@link ClassLoader}。
	 * 需要直接访问 {@code ClassLoader} 的<p>客户端可以使用 {@code ResourceLoader} 以统一的方式进行访问，
	 * 而不是依赖于线程上下文 {@code ClassLoader}。
	 *
	 * @return the {@code ClassLoader}
	 * (only {@code null} if even the system {@code ClassLoader} isn't accessible)
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getClassLoader();

}
