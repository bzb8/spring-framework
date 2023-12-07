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

package org.springframework.beans;

import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by bean metadata elements
 * that carry a configuration source object.
 *
 * 由携带配置源对象的 Bean 元数据元素实现的接口。
 * BeanMetadataElement 接口主要定义了一个方法 getSource()，该方法返回与当前 bean 元素相关的源对象。源对象通常用于提供关于配置元素来源的信息，例如从哪个配置文件加载、从哪个类中读取等。
 * @Bean 则是@Configuration注解所在的类
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public interface BeanMetadataElement {

	/**
	 * Return the configuration source {@code Object} for this metadata element
	 * (may be {@code null}).
	 *
	 * 返回此元数据元素的配置源 {@code Object}（可能是 {@code null}）。
	 */
	@Nullable
	default Object getSource() {
		return null;
	}

}
