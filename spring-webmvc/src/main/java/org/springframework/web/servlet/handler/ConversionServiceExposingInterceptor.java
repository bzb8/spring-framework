/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that places the configured {@link ConversionService} in request scope
 * so it's available during request processing. The request attribute name is
 * "org.springframework.core.convert.ConversionService", the value of
 * {@code ConversionService.class.getName()}.
 * <p>一个处理拦截器，用于将配置的{@link ConversionService}放置在请求作用域中，
 * 以便在请求处理期间可以使用。请求属性的名称是
 * "org.springframework.core.convert.ConversionService"，值为
 * {@code ConversionService.class.getName()}。
 *
 * <p>Mainly for use within JSP tags such as the spring:eval tag.
 * 主要在JSP标签如spring:eval标签内部使用。
 *
 * @author Keith Donald
 * @since 3.0.1
 */
public class ConversionServiceExposingInterceptor implements HandlerInterceptor {

	private final ConversionService conversionService;


	/**
	 * Creates a new {@link ConversionServiceExposingInterceptor}.
	 * @param conversionService the conversion service to export to request scope when this interceptor is invoked
	 */
	public ConversionServiceExposingInterceptor(ConversionService conversionService) {
		Assert.notNull(conversionService, "The ConversionService may not be null");
		this.conversionService = conversionService;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException, IOException {
		// 设置请求的属性：org.springframework.core.convert.ConversionService -> DefaultFormattingConversionService
		request.setAttribute(ConversionService.class.getName(), this.conversionService);
		return true;
	}

}
