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

package org.springframework.core.io.support;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Strategy interface for resolving a location pattern (for example,
 * an Ant-style path pattern) into {@link Resource} objects.
 * 用于将位置模式（例如，Ant 样式路径模式）解析为 {@link Resource} 对象的策略接口。
 *
 * <p>This is an extension to the {@link org.springframework.core.io.ResourceLoader}
 * interface. A passed-in {@code ResourceLoader} (for example, an
 * {@link org.springframework.context.ApplicationContext} passed in via
 * {@link org.springframework.context.ResourceLoaderAware} when running in a context)
 * can be checked whether it implements this extended interface too.
 *
 * <p>这是{@link org.springframework.core.io.ResourceLoader}接口的扩展。
 * 传入的{@code ResourceLoader}（例如，在上下文中运行时通过{@link org.springframework.context.ResourceLoaderAware}
 * 传入的{@link org.springframework.context.ApplicationContext}）可以检查它是否也实现了这个扩展接口。
 *
 * <p>{@link PathMatchingResourcePatternResolver} is a standalone implementation
 * that is usable outside an {@code ApplicationContext}, also used by
 * {@link ResourceArrayPropertyEditor} for populating {@code Resource} array bean
 * properties.
 * <p>{@link PathMatchingResourcePatternResolver} 是一个独立的实现，可在 {@code ApplicationContext} 外部使用，
 * {@link ResourceArrayPropertyEditor} 也使用它来填充 {@code Resource} 数组 Bean 属性。
 *
 * <p>Can be used with any sort of location pattern &mdash; for example,
 * {@code "/WEB-INF/*-context.xml"}. However, input patterns have to match the
 * strategy implementation. This interface just specifies the conversion method
 * rather than a specific pattern format.
 *
 * <p>可以与任何类型的位置模式一起使用，
 * 例如 {@code “/WEB-INF/*-context.xml”}。但是，输入模式必须与策略实现相匹配。此接口仅指定转换方法，而不是特定的模式格式。
 *
 * <p>This interface also defines a {@code "classpath*:"} resource prefix for all
 * matching resources from the class path. Note that the resource location may
 * also contain placeholders &mdash; for example {@code "/beans-*.xml"}. JAR files
 * or different directories in the class path can contain multiple files of the
 * same name.
 *
 * <p>此接口还为类路径中的所有匹配资源定义 {@code “classpath*：”} 资源前缀。
 * 请注意，资源位置也可能包含占位符，例如 {@code “beans-*.xml”}。类路径中的 JAR 文件或不同目录可以包含多个同名文件。
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 */
public interface ResourcePatternResolver extends ResourceLoader {

	/**
	 * Pseudo URL prefix for all matching resources from the class path: "classpath*:"
	 * <p>This differs from ResourceLoader's classpath URL prefix in that it
	 * retrieves all matching resources for a given name (e.g. "/beans.xml"),
	 * for example in the root of all deployed JAR files.
	 * @see org.springframework.core.io.ResourceLoader#CLASSPATH_URL_PREFIX
	 */
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

	/**
	 * Resolve the given location pattern into {@code Resource} objects.
	 * <p>Overlapping resource entries that point to the same physical
	 * resource should be avoided, as far as possible. The result should
	 * have set semantics.
	 * 将给定的位置模式解析为 {@code Resource} 对象。 <p>应尽可能避免指向同一物理资源的重叠资源条目。结果应该具有设定的语义。
	 * @param locationPattern the location pattern to resolve
	 * @return the corresponding {@code Resource} objects
	 * @throws IOException in case of I/O errors
	 */
	Resource[] getResources(String locationPattern) throws IOException;

}
