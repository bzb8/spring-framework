/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Defines callback methods to customize the Java-based configuration for
 * Spring MVC enabled via {@code @EnableWebMvc}.
 * <p>定义回调方法，用于自定义通过{@code @EnableWebMvc}启用的Spring MVC的Java配置。
 *
 * <p>{@code @EnableWebMvc}-annotated configuration classes may implement
 * this interface to be called back and given a chance to customize the
 * default configuration.
 * <p>通过{@code @EnableWebMvc}注解配置的类可以实现此接口，以被回调并有机会自定义默认配置。
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author David Syer
 * @since 3.1
 */
public interface WebMvcConfigurer {

	/**
	 * Help with configuring {@link HandlerMapping} path matching options such as
	 * whether to use parsed {@code PathPatterns} or String pattern matching
	 * with {@code PathMatcher}, whether to match trailing slashes, and more.
	 * <p>配置{@link HandlerMapping}路径匹配选项，例如是否使用解析的{@code PathPatterns}或字符串匹配
	 * 与{@code PathMatcher}，是否匹配尾部斜杠等。
	 * @since 4.0.3
	 * @see PathMatchConfigurer
	 */
	default void configurePathMatch(PathMatchConfigurer configurer) {
	}

	/**
	 * Configure content negotiation options.
	 * <p>配置内容协商选项。
	 */
	default void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
	}

	/**
	 * Configure asynchronous request handling options.
	 * <p>配置异步请求处理选项。
	 */
	default void configureAsyncSupport(AsyncSupportConfigurer configurer) {
	}

	/**
	 * Configure a handler to delegate unhandled requests by forwarding to the
	 * Servlet container's "default" servlet. A common use case for this is when
	 * the {@link DispatcherServlet} is mapped to "/" thus overriding the
	 * Servlet container's default handling of static resources.
	 * <p>配置处理未处理请求的处理器，通过转发到Servlet容器的"default" servlet。
	 * 一个常见的用例是，当{@link DispatcherServlet}被映射到"/"时，会覆盖Servlet容器默认处理静态资源的方式。
	 */
	default void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
	}

	/**
	 * Add {@link Converter Converters} and {@link Formatter Formatters} in addition to the ones
	 * registered by default.
	 * <p>添加{@link Converter Converters}和{@link Formatter Formatters}，以补充默认注册的转换器和格式化器。
	 */
	default void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * Add Spring MVC lifecycle interceptors for pre- and post-processing of
	 * controller method invocations and resource handler requests.
	 * Interceptors can be registered to apply to all requests or be limited
	 * to a subset of URL patterns.
	 * <p>添加Spring MVC生命周期拦截器，用于控制器方法调用和资源处理器请求的预处理和后处理。
	 * 拦截器可以注册为应用于所有请求或限定于一组URL模式。
	 */
	default void addInterceptors(InterceptorRegistry registry) {
	}

	/**
	 * Add handlers to serve static resources such as images, js, and, css
	 * files from specific locations under web application root, the classpath,
	 * and others.
	 * <p>添加处理静态资源的处理器，如图片、js和css文件，从Web应用程序根目录下的特定位置、类路径等加载。
	 * @see ResourceHandlerRegistry
	 */
	default void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * Configure "global" cross-origin request processing. The configured CORS
	 * mappings apply to annotated controllers, functional endpoints, and static
	 * resources.
	 * <p>Annotated controllers can further declare more fine-grained config via
	 * {@link org.springframework.web.bind.annotation.CrossOrigin @CrossOrigin}.
	 * In such cases "global" CORS configuration declared here is
	 * {@link org.springframework.web.cors.CorsConfiguration#combine(CorsConfiguration) combined}
	 * with local CORS configuration defined on a controller method.
	 * <p>配置跨源请求处理的"全局"选项。
	 * 配置的CORS映射适用于注解控制器、功能端点和静态资源。
	 * <p>注解控制器可以进一步声明更细粒度的配置，通过{@link org.springframework.web.bind.annotation.CrossOrigin @CrossOrigin}。
	 * 在这种情况下，这里声明的“全局”CORS配置与控制器方法上定义的本地CORS配置
	 * {@link org.springframework.web.cors.CorsConfiguration#combine(CorsConfiguration) 组合}。
	 * @since 4.2
	 * @see CorsRegistry
	 * @see CorsConfiguration#combine(CorsConfiguration)
	 */
	default void addCorsMappings(CorsRegistry registry) {
	}

	/**
	 * Configure simple automated controllers pre-configured with the response
	 * status code and/or a view to render the response body. This is useful in
	 * cases where there is no need for custom controller logic -- e.g. render a
	 * home page, perform simple site URL redirects, return a 404 status with
	 * HTML content, a 204 with no content, and more.
	 * <p>配置简单自动控制器，预配置响应状态码和/或用于渲染响应体的视图。
	 * 这对于不需要自定义控制器逻辑的情况很有用——例如，渲染主页、执行简单站点URL重定向、返回404状态码和HTML内容、
	 * 204状态码无内容等。
	 * @see ViewControllerRegistry
	 */
	default void addViewControllers(ViewControllerRegistry registry) {
	}

	/**
	 * Configure view resolvers to translate String-based view names returned from
	 * controllers into concrete {@link org.springframework.web.servlet.View}
	 * implementations to perform rendering with.
	 * <p>配置视图解析器，将控制器返回的基于字符串的视图名称转换为具体的
	 * {@link org.springframework.web.servlet.View}实现进行渲染。
	 * @since 4.1
	 */
	default void configureViewResolvers(ViewResolverRegistry registry) {
	}

	/**
	 * Add resolvers to support custom controller method argument types.
	 * <p>This does not override the built-in support for resolving handler
	 * method arguments. To customize the built-in support for argument
	 * resolution, configure {@link RequestMappingHandlerAdapter} directly.
	 * <p>添加解析器以支持自定义控制器方法参数类型。
	 * <p>这不会覆盖解析器对处理方法参数的内置支持。要自定义参数解析的内置支持，
	 * 直接配置{@link RequestMappingHandlerAdapter}。
	 * @param resolvers initially an empty list
	 */
	default void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
	}

	/**
	 * Add handlers to support custom controller method return value types.
	 * <p>Using this option does not override the built-in support for handling
	 * return values. To customize the built-in support for handling return
	 * values, configure RequestMappingHandlerAdapter directly.
	 * <p>添加处理器以支持自定义控制器方法返回值类型。
	 * <p>使用此选项不会覆盖处理返回值的内置支持。要自定义处理返回值的内置支持，
	 * 直接配置RequestMappingHandlerAdapter。
	 * @param handlers initially an empty list
	 */
	default void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
	}

	/**
	 * Configure the {@link HttpMessageConverter HttpMessageConverter}s for
	 * reading from the request body and for writing to the response body.
	 * <p>By default, all built-in converters are configured as long as the
	 * corresponding 3rd party libraries such Jackson JSON, JAXB2, and others
	 * are present on the classpath.
	 * <p><strong>Note</strong> use of this method turns off default converter
	 * registration. Alternatively, use
	 * {@link #extendMessageConverters(java.util.List)} to modify that default
	 * list of converters.
	 * <p>配置用于从请求体中读取和向响应体中写入的{@link HttpMessageConverter HttpMessageConverter}s。
	 * <p>默认情况下，只要相应的第三方库（如Jackson JSON、JAXB2等）存在于类路径上，就会配置所有内置转换器。
	 * <p><strong>注意</strong>使用此方法会关闭默认转换器注册。或者，使用
	 * {@link #extendMessageConverters(java.util.List)}来修改默认转换器列表。
	 *
	 * @param converters initially an empty list of converters
	 */
	default void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * Extend or modify the list of converters after it has been, either
	 * {@link #configureMessageConverters(List) configured} or initialized with
	 * a default list.
	 * <p>Note that the order of converter registration is important. Especially
	 * in cases where clients accept {@link org.springframework.http.MediaType#ALL}
	 * the converters configured earlier will be preferred.
	 * <p>在已经配置或初始化为默认转换器列表之后，扩展或修改转换器列表。
	 * <p>注意转换器的注册顺序很重要。特别是在客户端接受{@link org.springframework.http.MediaType#ALL}的情况下，
	 * 较早配置的转换器将被优先考虑。
	 * @param converters the list of configured converters to be extended
	 * @since 4.1.3
	 */
	default void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * Configure exception resolvers.
	 * <p>The given list starts out empty. If it is left empty, the framework
	 * configures a default set of resolvers, see
	 * {@link WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List, org.springframework.web.accept.ContentNegotiationManager)}.
	 * Or if any exception resolvers are added to the list, then the application
	 * effectively takes over and must provide, fully initialized, exception
	 * resolvers.
	 * <p>Alternatively you can use
	 * {@link #extendHandlerExceptionResolvers(List)} which allows you to extend
	 * or modify the list of exception resolvers configured by default.
	 * <p> 配置异常解析器。
	 * <p>给定的列表最初为空。如果将其保留为空，框架将配置一组默认的解析器，参见
	 * {@link WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List, org.springframework.web.accept.ContentNegotiationManager)}。
	 * 或者，如果向列表中添加任何异常解析器，则应用程序实际上接管并必须提供完全初始化的异常解析器。
	 * <p>或者您可以使用{@link #extendHandlerExceptionResolvers(List)}，这允许您扩展或修改默认配置的异常解析器列表。
	 *
	 * @param resolvers initially an empty list
	 * @see #extendHandlerExceptionResolvers(List)
	 * @see WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List, org.springframework.web.accept.ContentNegotiationManager)
	 */
	default void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
	}

	/**
	 * Extending or modify the list of exception resolvers configured by default.
	 * This can be useful for inserting a custom exception resolver without
	 * interfering with default ones.
	 * <p>扩展或修改默认配置的异常解析器列表。
	 * 这对于在不干扰默认解析器的情况下插入自定义异常解析器很有用。
	 * @param resolvers the list of configured resolvers to extend
	 * @since 4.3
	 * @see WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List, org.springframework.web.accept.ContentNegotiationManager)
	 */
	default void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
	}

	/**
	 * Provide a custom {@link Validator} instead of the one created by default.
	 * The default implementation, assuming JSR-303 is on the classpath, is:
	 * {@link org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean}.
	 * Leave the return value as {@code null} to keep the default.
	 * <p>提供一个自定义的{@link Validator}，而不是默认创建的。
	 * 默认实现是，假设JSR-303在类路径上，是{@link org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean}。
	 * 将返回值保留为{@code null}以保持默认值。
	 */
	@Nullable
	default Validator getValidator() {
		return null;
	}

	/**
	 * Provide a custom {@link MessageCodesResolver} for building message codes
	 * from data binding and validation error codes. Leave the return value as
	 * {@code null} to keep the default.
	 * <p>提供自定义{@link MessageCodesResolver}，用于从数据绑定和验证错误代码构建消息代码。
	 */
	@Nullable
	default MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

}
