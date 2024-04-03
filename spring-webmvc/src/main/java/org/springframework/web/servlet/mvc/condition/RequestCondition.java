/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

/**
 * Contract for request mapping conditions.
 * 请求映射条件的合约接口。
 *
 * <p>Request conditions can be combined via {@link #combine(Object)}, matched to
 * a request via {@link #getMatchingCondition(HttpServletRequest)}, and compared
 * to each other via {@link #compareTo(Object, HttpServletRequest)} to determine
 * which is a closer match for a given request.
 * <p>请求条件可以通过{@link #combine(Object)}方法进行组合，通过{@link #getMatchingCondition(HttpServletRequest)}方法与请求进行匹配，
 * 并通过{@link #compareTo(Object, HttpServletRequest)}方法相互比较，以确定哪个条件更贴近给定的请求。
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 3.1
 * @param <T> the type of objects that this RequestCondition can be combined
 * with and compared to
 */
public interface RequestCondition<T> {

	/**
	 * Combine this condition with another such as conditions from a
	 * type-level and method-level {@code @RequestMapping} annotation.
	 * 用于组合当前请求条件与其他条件，生成新的请求条件。
	 * @param other the condition to combine with.
	 * @return a request condition instance that is the result of combining
	 * the two condition instances.
	 */
	T combine(T other);

	/**
	 * Check if the condition matches the request returning a potentially new
	 * instance created for the current request. For example a condition with
	 * multiple URL patterns may return a new instance only with those patterns
	 * that match the request.
	 * <p>For CORS pre-flight requests, conditions should match to the would-be,
	 * actual request (e.g. URL pattern, query parameters, and the HTTP method
	 * from the "Access-Control-Request-Method" header). If a condition cannot
	 * be matched to a pre-flight request it should return an instance with
	 * empty content thus not causing a failure to match.
	 * 检查条件是否与请求匹配，并返回可能为当前请求创建的新实例。例如，具有多个URL模式的条件可能仅返回具有匹配请求的那些模式的新实例。
	 * <p>对于CORS预检请求，条件应匹配将要进行的实际请求（例如URL模式、查询参数以及"Access-Control-Request-Method"头中的HTTP方法）。
	 * 如果条件无法匹配预检请求，则应返回一个内容为空的实例，从而不会导致匹配失败。
	 *
	 * @return a condition instance in case of a match or {@code null} otherwise.如果匹配成功，则返回一个条件实例；否则返回{@code null}。
	 *
	 */
	@Nullable
	T getMatchingCondition(HttpServletRequest request);

	/**
	 * Compare this condition to another condition in the context of
	 * a specific request. This method assumes both instances have
	 * been obtained via {@link #getMatchingCondition(HttpServletRequest)}
	 * to ensure they have content relevant to current request only.
	 * 将当前条件与特定请求中的另一个条件进行比较。此方法假定两个实例都是通过
	 * {@link #getMatchingCondition(HttpServletRequest)}方法获取的，以确保它们只包含与当前请求相关的内容。
	 * 比较当前请求条件与另一个条件对象，以确定哪个更贴近给定的请求。
	 * 返回值表示比较的结果，负数表示当前条件更贴近，零表示两者相等，正数表示另一个条件更贴近。
	 */
	int compareTo(T other, HttpServletRequest request);

}
