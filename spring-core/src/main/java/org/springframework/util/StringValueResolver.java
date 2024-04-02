/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.util;

import org.springframework.lang.Nullable;

/**
 * Simple strategy interface for resolving a String value.
 * Used by {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}.
 *<p>用于解析 String 值的简单策略接口。由 {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} 使用。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#resolveAliases
 * @see org.springframework.beans.factory.config.BeanDefinitionVisitor#BeanDefinitionVisitor(StringValueResolver)
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 */
@FunctionalInterface
public interface StringValueResolver {

	/**
	 * Resolve the given String value, for example parsing placeholders.
	 * --
	 * 解析给定的字符串值，例如解析占位符。
	 *
	 * @param strVal the original String value (never {@code null})
	 * @return the resolved String value (may be {@code null} when resolved to a null
	 * value), possibly the original String value itself (in case of no placeholders
	 * to resolve or when ignoring unresolvable placeholders)
	 *
	 * 解析的字符串值（当解析为 null 值时，可能是 {@code null}），可能是原始字符串值本身（如果没有要解析的占位符或忽略无法解析的占位符）
	 *
	 * @throws IllegalArgumentException in case of an unresolvable String value
	 */
	@Nullable
	String resolveStringValue(String strVal);

}
