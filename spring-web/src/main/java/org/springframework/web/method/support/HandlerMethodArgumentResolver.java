/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Strategy interface for resolving method parameters into argument values in
 * the context of a given request.
 * 用于在给定请求的上下文中将方法参数解析为参数值的策略接口。
 *
 * @author Arjen Poutsma
 * @since 3.1
 * @see HandlerMethodReturnValueHandler
 */
public interface HandlerMethodArgumentResolver {

	/**
	 * Whether the given {@linkplain MethodParameter method parameter} is
	 * supported by this resolver.
	 * 判断给定的 {@linkplain MethodParameter 方法参数} 是否被此解析器支持。
	 * @param parameter the method parameter to check
	 *                  需要检查的方法参数
	 * @return {@code true} if this resolver supports the supplied parameter;
	 * {@code false} otherwise
	 * 如果此解析器支持提供的参数，则返回 {@code true}；否则返回 {@code false}
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * Resolves a method parameter into an argument value from a given request.
	 * A {@link ModelAndViewContainer} provides access to the model for the
	 * request. A {@link WebDataBinderFactory} provides a way to create
	 * a {@link WebDataBinder} instance when needed for data binding and
	 * type conversion purposes.
	 * 从给定的请求解析方法参数为一个参数值。
	 * {@link ModelAndViewContainer} 提供对请求的模型的访问。 {@link WebDataBinderFactory} 提供一种方式来创建
	 * {@link WebDataBinder} 实例，用于数据绑定和类型转换的目的。
	 * @param parameter the method parameter to resolve. This parameter must
	 * have previously been passed to {@link #supportsParameter} which must
	 * have returned {@code true}.
	 *                  需要解析的方法参数。此参数必须先前被传递给 {@link #supportsParameter} 方法，并且该方法必须返回 {@code true}。
	 * @param mavContainer the ModelAndViewContainer for the current request
	 *                     当前请求的ModelAndViewContainer
	 * @param webRequest the current request
	 * @param binderFactory a factory for creating {@link WebDataBinder} instances
	 *                      用于创建 {@link WebDataBinder} 实例的工厂
	 * @return the resolved argument value, or {@code null} if not resolvable
	 * 解析后的参数值，如果不可解析则返回 {@code null}
	 * @throws Exception in case of errors with the preparation of argument values
	 */
	@Nullable
	Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception;

}
