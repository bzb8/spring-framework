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

package org.springframework.beans.factory.config;

import java.io.Serializable;

import org.springframework.lang.Nullable;

/**
 * Simple marker class for an individually autowired property value, to be added
 * to {@link BeanDefinition#getPropertyValues()} for a specific bean property.
 *
 * 单个自动注入属性值的简单标记类，要添加到特定 Bean 属性的 {@link BeanDefinition#getPropertyValues()} 中。
 *
 * <p>At runtime, this will be replaced with a {@link DependencyDescriptor}
 * for the corresponding bean property's write method, eventually to be resolved
 * through a {@link AutowireCapableBeanFactory#resolveDependency} step.
 *
 * 在运行时，这将替换为相应 Bean 属性的 write 方法的 {@link DependencyDescriptor}，最终将通过 {@link AutowireCapableBeanFactory#resolveDependency} 步骤进行解析。
 *
 * @author Juergen Hoeller
 * @since 5.2
 * @see AutowireCapableBeanFactory#resolveDependency
 * @see BeanDefinition#getPropertyValues()
 * @see org.springframework.beans.factory.support.BeanDefinitionBuilder#addAutowiredProperty
 */
@SuppressWarnings("serial")
public final class AutowiredPropertyMarker implements Serializable {

	/**
	 * The canonical instance for the autowired marker value.
	 * 自动注入标记值的规范实例。
	 */
	public static final Object INSTANCE = new AutowiredPropertyMarker();


	private AutowiredPropertyMarker() {
	}

	private Object readResolve() {
		return INSTANCE;
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		return (this == obj);
	}

	@Override
	public int hashCode() {
		return AutowiredPropertyMarker.class.hashCode();
	}

	@Override
	public String toString() {
		return "(autowired)";
	}

}
