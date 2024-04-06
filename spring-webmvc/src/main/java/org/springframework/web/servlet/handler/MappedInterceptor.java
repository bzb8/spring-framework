/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

/**
 * Wraps a {@link HandlerInterceptor} and uses URL patterns to determine whether
 * it applies to a given request.
 * 包装一个{@link HandlerInterceptor}，并使用URL模式来确定它是否适用于给定的请求。
 *
 * <p>Pattern matching can be done with {@link PathMatcher} or with parsed
 * {@link PathPattern}. The syntax is largely the same with the latter being more
 * tailored for web usage and more efficient. The choice is driven by the
 * presence of a {@link UrlPathHelper#resolveAndCacheLookupPath resolved}
 * {@code String} lookupPath or a {@link ServletRequestPathUtils#parseAndCache
 * parsed} {@code RequestPath} which in turn depends on the
 * {@link HandlerMapping} that matched the current request.
 * <p>可以通过{@link PathMatcher}或解析的{@link PathPattern}进行模式匹配。其语法在很大程度上是相同的，后者更适用于web场景，并且效率更高。
 * 这一选择取决于是否存在通过{@link UrlPathHelper#resolveAndCacheLookupPath 解析并缓存的} {@code String} lookupPath 或者
 * 通过{@link ServletRequestPathUtils#parseAndCache 解析并缓存的} {@code RequestPath}，这反过来又取决于当前请求匹配的{@link HandlerMapping}。
 *
 * <p>{@code MappedInterceptor} is supported by subclasses of
 * {@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping
 * AbstractHandlerMethodMapping} which detect beans of type
 * {@code MappedInterceptor} and also check if interceptors directly registered
 * with it are of this type.
 * <p>{@code MappedInterceptor}被{@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping AbstractHandlerMethodMapping}
 * 的子类支持，它们可以检测到类型为{@code MappedInterceptor}的bean，并且还会检查直接注册的拦截器是否为这种类型。
 *
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.0
 */
public final class MappedInterceptor implements HandlerInterceptor {

	private static final PathMatcher defaultPathMatcher = new AntPathMatcher();


	@Nullable
	private final PatternAdapter[] includePatterns;

	@Nullable
	private final PatternAdapter[] excludePatterns;

	private PathMatcher pathMatcher = defaultPathMatcher;

	private final HandlerInterceptor interceptor;


	/**
	 * Create an instance with the given include and exclude patterns along with
	 * the target interceptor for the mappings.
	 * @param includePatterns patterns to which requests must match, or null to
	 * match all paths
	 * @param excludePatterns patterns to which requests must not match
	 * @param interceptor the target interceptor
	 * @param parser a parser to use to pre-parse patterns into {@link PathPattern};
	 * when not provided, {@link PathPatternParser#defaultInstance} is used.
	 * @since 5.3
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
			HandlerInterceptor interceptor, @Nullable PathPatternParser parser) {

		this.includePatterns = PatternAdapter.initPatterns(includePatterns, parser);
		this.excludePatterns = PatternAdapter.initPatterns(excludePatterns, parser);
		this.interceptor = interceptor;
	}


	/**
	 * Variant of
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
	 * with include patterns only.
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, HandlerInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**
	 * Variant of
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
	 * without a provided parser.
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
			HandlerInterceptor interceptor) {

		this(includePatterns, excludePatterns, interceptor, null);
	}

	/**
	 * Variant of
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
	 * with a {@link WebRequestInterceptor} as the target.
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, WebRequestInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**
	 * Variant of
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
	 * with a {@link WebRequestInterceptor} as the target.
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
			WebRequestInterceptor interceptor) {

		this(includePatterns, excludePatterns, new WebRequestHandlerInterceptorAdapter(interceptor));
	}


	/**
	 * Return the patterns this interceptor is mapped to.
	 */
	@Nullable
	public String[] getPathPatterns() {
		return (!ObjectUtils.isEmpty(this.includePatterns) ?
				Arrays.stream(this.includePatterns).map(PatternAdapter::getPatternString).toArray(String[]::new) :
				null);
	}

	/**
	 * The target {@link HandlerInterceptor} to invoke in case of a match.
	 */
	public HandlerInterceptor getInterceptor() {
		return this.interceptor;
	}

	/**
	 * Configure the PathMatcher to use to match URL paths with against include
	 * and exclude patterns.
	 * <p>This is an advanced property that should be used only when a
	 * customized {@link AntPathMatcher} or a custom PathMatcher is required.
	 * <p>By default this is {@link AntPathMatcher}.
	 * <p><strong>Note:</strong> Setting {@code PathMatcher} enforces use of
	 * String pattern matching even when a
	 * {@link ServletRequestPathUtils#parseAndCache parsed} {@code RequestPath}
	 * is available.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * The {@link #setPathMatcher(PathMatcher) configured} PathMatcher.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}


	/**
	 * Check whether this interceptor is mapped to the request.
	 * <p>The request mapping path is expected to have been resolved externally.
	 * See also class-level Javadoc.
	 * 检查当前拦截器是否被请求映射所指定。
	 * <p>预期请求映射路径已在外部解析。
	 * 请参见类级别的JavaDoc。
	 *
	 * @param request the request to match to 需要匹配的请求
	 * @return {@code true} if the interceptor should be applied to the request
	 */
	public boolean matches(HttpServletRequest request) {
		// 从请求属性中获取路径
		Object path = ServletRequestPathUtils.getCachedPath(request);
		if (this.pathMatcher != defaultPathMatcher) {
			// 如果路径匹配器不为默认值，则将路径转换为字符串
			path = path.toString();
		}
		boolean isPathContainer = (path instanceof PathContainer);
		// 遍历排除模式列表，如果路径匹配任何一个排除模式，则拦截器不应用于该请求
		if (!ObjectUtils.isEmpty(this.excludePatterns)) {
			for (PatternAdapter adapter : this.excludePatterns) {
				if (adapter.match(path, isPathContainer, this.pathMatcher)) {
					return false;
				}
			}
		}
		// 如果没有包含模式，则默认认为拦截器应应用于该请求
		if (ObjectUtils.isEmpty(this.includePatterns)) {
			return true;
		}
		// 遍历包含模式列表，如果路径匹配任何一个包含模式，则拦截器应用于该请求
		for (PatternAdapter adapter : this.includePatterns) {
			if (adapter.match(path, isPathContainer, this.pathMatcher)) {
				return true;
			}
		}
		// 如果路径既不匹配排除模式也不匹配包含模式，则拦截器不应用于该请求
		return false;
	}

	/**
	 * Determine a match for the given lookup path.
	 * @param lookupPath the current request path
	 * @param pathMatcher a path matcher for path pattern matching
	 * @return {@code true} if the interceptor applies to the given request path
	 * @deprecated as of 5.3 in favor of {@link #matches(HttpServletRequest)}
	 */
	@Deprecated
	public boolean matches(String lookupPath, PathMatcher pathMatcher) {
		pathMatcher = (this.pathMatcher != defaultPathMatcher ? this.pathMatcher : pathMatcher);
		if (!ObjectUtils.isEmpty(this.excludePatterns)) {
			for (PatternAdapter adapter : this.excludePatterns) {
				if (pathMatcher.match(adapter.getPatternString(), lookupPath)) {
					return false;
				}
			}
		}
		if (ObjectUtils.isEmpty(this.includePatterns)) {
			return true;
		}
		for (PatternAdapter adapter : this.includePatterns) {
			if (pathMatcher.match(adapter.getPatternString(), lookupPath)) {
				return true;
			}
		}
		return false;
	}


	// HandlerInterceptor delegation

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return this.interceptor.preHandle(request, response, handler);
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {

		this.interceptor.postHandle(request, response, handler, modelAndView);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {

		this.interceptor.afterCompletion(request, response, handler, ex);
	}


	/**
	 * Contains both the parsed {@link PathPattern} and the raw String pattern,
	 * and uses the former when the cached path is {@link PathContainer} or the
	 * latter otherwise. If the pattern cannot be parsed due to unsupported
	 * syntax, then {@link PathMatcher} is used for all requests.
	 * @since 5.3.6
	 */
	private static class PatternAdapter {
		// 原始的path
		private final String patternString;
		// 解析后的path
		@Nullable
		private final PathPattern pathPattern;


		public PatternAdapter(String pattern, @Nullable PathPatternParser parser) {
			this.patternString = pattern;
			this.pathPattern = initPathPattern(pattern, parser);
		}

		@Nullable
		private static PathPattern initPathPattern(String pattern, @Nullable PathPatternParser parser) {
			try {
				return (parser != null ? parser : PathPatternParser.defaultInstance).parse(pattern);
			}
			catch (PatternParseException ex) {
				return null;
			}
		}

		public String getPatternString() {
			return this.patternString;
		}

		public boolean match(Object path, boolean isPathContainer, PathMatcher pathMatcher) {
			if (isPathContainer) {
				PathContainer pathContainer = (PathContainer) path;
				if (this.pathPattern != null) {
					return this.pathPattern.matches(pathContainer);
				}
				String lookupPath = pathContainer.value();
				path = UrlPathHelper.defaultInstance.removeSemicolonContent(lookupPath);
			}
			return pathMatcher.match(this.patternString, (String) path);
		}

		@Nullable
		public static PatternAdapter[] initPatterns(
				@Nullable String[] patterns, @Nullable PathPatternParser parser) {

			if (ObjectUtils.isEmpty(patterns)) {
				return null;
			}
			return Arrays.stream(patterns)
					.map(pattern -> new PatternAdapter(pattern, parser))
					.toArray(PatternAdapter[]::new);
		}
	}

}
