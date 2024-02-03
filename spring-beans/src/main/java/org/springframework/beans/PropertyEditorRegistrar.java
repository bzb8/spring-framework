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

package org.springframework.beans;

/**
 * Interface for strategies that register custom
 * {@link java.beans.PropertyEditor property editors} with a
 * {@link org.springframework.beans.PropertyEditorRegistry property editor registry}.
 * --
 * 将自定义 {@link java.beans.PropertyEditor 属性编辑器} 注册到 {@link org.springframework.beans.PropertyEditorRegistry 属性编辑器注册表} 的策略的接口。
 *
 * <p>This is particularly useful when you need to use the same set of
 * property editors in several situations: write a corresponding
 * registrar and reuse that in each case.
 * --s
 * 当您需要在多种情况下使用同一组属性编辑器时，这特别有用：编写相应的注册器并在每种情况下重用该注册器。
 * 注册员
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see PropertyEditorRegistry
 * @see java.beans.PropertyEditor
 */
public interface PropertyEditorRegistrar {

	/**
	 * Register custom {@link java.beans.PropertyEditor PropertyEditors} with
	 * the given {@code PropertyEditorRegistry}.
	 * <p>The passed-in registry will usually be a {@link BeanWrapper} or a
	 * {@link org.springframework.validation.DataBinder DataBinder}.
	 * <p>It is expected that implementations will create brand new
	 * {@code PropertyEditors} instances for each invocation of this
	 * method (since {@code PropertyEditors} are not threadsafe).
	 * 00
	 * 将自定义 {@link java.beans.PropertyEditor PropertyEditors} 注册到给定的 {@code PropertyEditorRegistry}。
	 * 传入的注册表通常是{@link BeanWrapper}或{@link org.springframework.validation.DataBinder DataBinder}。
	 * 预计实现将为每次调用此方法创建全新的 {@code PropertyEditors} 实例（因为 {@code PropertyEditors} 不是线程安全的）。
	 *
	 * @param registry the {@code PropertyEditorRegistry} to register the
	 * custom {@code PropertyEditors} with
	 */
	void registerCustomEditors(PropertyEditorRegistry registry);

}
