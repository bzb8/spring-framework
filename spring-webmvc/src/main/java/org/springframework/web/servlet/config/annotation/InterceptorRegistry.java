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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;

/**
 * Helps with configuring a list of mapped interceptors.
 * 用于配置映射拦截器列表的帮助类。
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class InterceptorRegistry {
	// 拦截器
	private final List<InterceptorRegistration> registrations = new ArrayList<>();


	/**
	 * Adds the provided {@link HandlerInterceptor}.
	 * 添加提供的 {@link HandlerInterceptor}。
	 * @param interceptor the interceptor to add
	 * @return an {@link InterceptorRegistration} that allows you optionally configure the
	 * registered interceptor further for example adding URL patterns it should apply to.
	 * 一个 {@link InterceptorRegistration} 对象，它允许您进一步配置已注册的拦截器，
	 * 例如添加它应适用的 URL 模式。
	 */
	public InterceptorRegistration addInterceptor(HandlerInterceptor interceptor) {
		InterceptorRegistration registration = new InterceptorRegistration(interceptor);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * Adds the provided {@link WebRequestInterceptor}.
	 * @param interceptor the interceptor to add
	 * @return an {@link InterceptorRegistration} that allows you optionally configure the
	 * registered interceptor further for example adding URL patterns it should apply to.
	 */
	public InterceptorRegistration addWebRequestInterceptor(WebRequestInterceptor interceptor) {
		WebRequestHandlerInterceptorAdapter adapted = new WebRequestHandlerInterceptorAdapter(interceptor);
		InterceptorRegistration registration = new InterceptorRegistration(adapted);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * Return all registered interceptors.
	 */
	protected List<Object> getInterceptors() {
		return this.registrations.stream()
				.sorted(INTERCEPTOR_ORDER_COMPARATOR) // 按照order排序
				.map(InterceptorRegistration::getInterceptor)
				.collect(Collectors.toList());
	}

	// 按照order排序
	private static final Comparator<Object> INTERCEPTOR_ORDER_COMPARATOR =
			OrderComparator.INSTANCE.withSourceProvider(object -> {
				if (object instanceof InterceptorRegistration) {
					return (Ordered) ((InterceptorRegistration) object)::getOrder;
				}
				return null;
			});

}
