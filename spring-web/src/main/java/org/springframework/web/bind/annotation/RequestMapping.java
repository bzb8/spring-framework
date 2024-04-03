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

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Annotation for mapping web requests onto methods in request-handling classes
 * with flexible method signatures.
 *
 * <p>Both Spring MVC and Spring WebFlux support this annotation through a
 * {@code RequestMappingHandlerMapping} and {@code RequestMappingHandlerAdapter}
 * in their respective modules and package structure. For the exact list of
 * supported handler method arguments and return types in each, please use the
 * reference documentation links below:
 * <ul>
 * <li>Spring MVC
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-arguments">Method Arguments</a>
 * and
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-return-types">Return Values</a>
 * </li>
 * <li>Spring WebFlux
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-ann-arguments">Method Arguments</a>
 * and
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-ann-return-types">Return Values</a>
 * </li>
 * </ul>
 *
 * <p><strong>Note:</strong> This annotation can be used both at the class and
 * at the method level. In most cases, at the method level applications will
 * prefer to use one of the HTTP method specific variants
 * {@link GetMapping @GetMapping}, {@link PostMapping @PostMapping},
 * {@link PutMapping @PutMapping}, {@link DeleteMapping @DeleteMapping}, or
 * {@link PatchMapping @PatchMapping}.</p>
 *
 * <p><b>NOTE:</b> When using controller interfaces (e.g. for AOP proxying),
 * make sure to consistently put <i>all</i> your mapping annotations - such as
 * {@code @RequestMapping} and {@code @SessionAttributes} - on
 * the controller <i>interface</i> rather than on the implementation class.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 2.5
 * @see GetMapping
 * @see PostMapping
 * @see PutMapping
 * @see DeleteMapping
 * @see PatchMapping
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface RequestMapping {

	/**
	 * Assign a name to this mapping.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used on both levels, a combined name is derived by concatenation
	 * with "#" as separator.
	 * 为这个映射指定一个名称。
	 * <p><b>既支持在类型级别上，也支持在方法级别上！</b>
	 * 当在两个级别上都使用时，会通过以"#"为分隔符进行连接来派生一个组合名称。
	 * 可以参考{@link org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder}
	 * 和{@link org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy}。
	 *
	 * @see org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder
	 * @see org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy
	 */
	String name() default "";

	/**
	 * The primary mapping expressed by this annotation.
	 * <p>This is an alias for {@link #path}. For example,
	 * {@code @RequestMapping("/foo")} is equivalent to
	 * {@code @RequestMapping(path="/foo")}.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit
	 * this primary mapping, narrowing it for a specific handler method.
	 * <p><strong>NOTE</strong>: A handler method that is not mapped to any path
	 * explicitly is effectively mapped to an empty path.
	 */
	@AliasFor("path")
	String[] value() default {};

	/**
	 * The path mapping URIs (e.g. {@code "/profile"}).
	 * <p>Ant-style path patterns are also supported (e.g. {@code "/profile/**"}).
	 * At the method level, relative paths (e.g. {@code "edit"}) are supported
	 * within the primary mapping expressed at the type level.
	 * Path mapping URIs may contain placeholders (e.g. <code>"/${profile_path}"</code>).
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit
	 * this primary mapping, narrowing it for a specific handler method.
	 * <p><strong>NOTE</strong>: A handler method that is not mapped to any path
	 * explicitly is effectively mapped to an empty path.
	 * 定义请求的路径映射。
	 * 该注解支持 Ant 风格的路径模式（例如 {@code "/profile/**"}），以及在方法级别上的相对路径（例如 {@code "edit"}）。
	 * 路径映射的URI可以包含占位符（例如 {@code "/${profile_path}"}）。
	 * <p>此注解既支持在类型级别上使用，也支持在方法级别上使用。
	 * 当在类型级别使用时，所有方法级别的映射都会继承这个主要映射，从而为特定的处理方法缩小映射范围。
	 * <p><strong>注意</strong>：如果一个处理方法没有显式地映射到任何路径，则它实际上会被映射到一个空路径。
	 *
	 * @since 4.2
	 */
	@AliasFor("value")
	String[] path() default {};

	/**
	 * The HTTP request methods to map to, narrowing the primary mapping:
	 * GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit this
	 * HTTP method restriction.
	 * 用于映射HTTP请求方法的注解元素，用于细化主要映射关系。
	 * 支持的HTTP方法包括：GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE。
	 * <p><b>此注解既可在类型级别上使用，也可在方法级别上使用！</b>
	 * 当在类型级别上使用时，所有方法级别的映射都会继承此HTTP方法限制。
	 *
	 */
	RequestMethod[] method() default {};

	/**
	 * The parameters of the mapped request, narrowing the primary mapping.
	 * <p>Same format for any environment: a sequence of "myParam=myValue" style
	 * expressions, with a request only mapped if each such parameter is found
	 * to have the given value. Expressions can be negated by using the "!=" operator,
	 * as in "myParam!=myValue". "myParam" style expressions are also supported,
	 * with such parameters having to be present in the request (allowed to have
	 * any value). Finally, "!myParam" style expressions indicate that the
	 * specified parameter is <i>not</i> supposed to be present in the request.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit this
	 * parameter restriction.
	 * 定义映射请求的参数，缩小主要映射范围。
	 * <p>任何环境下都采用相同的格式：一系列 "myParam=myValue" 风格的表达式，
	 * 只有当每个这样的参数都被找到具有给定值时，请求才被映射。表达式可以通过使用 "!=" 运算符来否定，
	 * 例如 "myParam!=myValue"。"myParam" 风格的表达式也被支持，这类参数必须存在于请求中（允许拥有任何值）。
	 * 最后，"!myParam" 风格的表达式表示指定的参数不应该存在于请求中。
	 * <p><b>此注解支持在类型级别和方法级别上使用！</b>
	 * 当在类型级别上使用时，所有方法级别的映射都会继承此参数限制。
	 */
	String[] params() default {};

	/**
	 * The headers of the mapped request, narrowing the primary mapping.
	 * <p>Same format for any environment: a sequence of "My-Header=myValue" style
	 * expressions, with a request only mapped if each such header is found
	 * to have the given value. Expressions can be negated by using the "!=" operator,
	 * as in "My-Header!=myValue". "My-Header" style expressions are also supported,
	 * with such headers having to be present in the request (allowed to have
	 * any value). Finally, "!My-Header" style expressions indicate that the
	 * specified header is <i>not</i> supposed to be present in the request.
	 * <p>Also supports media type wildcards (*), for headers such as Accept
	 * and Content-Type. For instance,
	 * <pre class="code">
	 * &#064;RequestMapping(value = "/something", headers = "content-type=text/*")
	 * </pre>
	 * will match requests with a Content-Type of "text/html", "text/plain", etc.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit this
	 * header restriction.
	 * 映射请求的头部信息，缩小主要映射范围。
	 * <p>任何环境下都使用相同的格式：一系列 "My-Header=myValue" 风格的表达式，
	 * 只有当每个这样的头部被发现具有给定值时，请求才会被映射。表达式可以通过使用 "!=" 运算符来否定，
	 * 例如 "My-Header!=myValue"。支持 "My-Header" 风格的表达式，意味着请求中必须存在该头部（允许具有任何值）。
	 * 最后，"!My-Header" 风格的表达式表示请求中不应该存在指定的头部。
	 * <p>同时支持媒体类型的通配符（*），适用于如 Accept 和 Content-Type 等头部。
	 * 例如：
	 * <pre class="code">
	 * &#064;RequestMapping(value = "/something", headers = "content-type=text/*")
	 * </pre>
	 * 将匹配 Content-Type 为 "text/html"、"text/plain" 等的请求。
	 * <p><b>既支持在类型级别上使用，也支持在方法级别上使用！</b>
	 * 当在类型级别上使用时，所有方法级别的映射都会继承这个头部限制。
	 *
	 * @see org.springframework.http.MediaType
	 */
	String[] headers() default {};

	/**
	 * Narrows the primary mapping by media types that can be consumed by the
	 * mapped handler. Consists of one or more media types one of which must
	 * match to the request {@code Content-Type} header. Examples:
	 * <pre class="code">
	 * consumes = "text/plain"
	 * consumes = {"text/plain", "application/*"}
	 * consumes = MediaType.TEXT_PLAIN_VALUE
	 * </pre>
	 * Expressions can be negated by using the "!" operator, as in
	 * "!text/plain", which matches all requests with a {@code Content-Type}
	 * other than "text/plain".
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * If specified at both levels, the method level consumes condition overrides
	 * the type level condition.
	 * 通过指定处理器可以消费的媒体类型来缩小主要映射范围。必须有一个媒体类型与请求的{@code Content-Type}头匹配。
	 * 支持的格式示例:
	 * <pre class="code">
	 * consumes = "text/plain"
	 * consumes = {"text/plain", "application/*"}
	 * consumes = MediaType.TEXT_PLAIN_VALUE
	 * </pre>
	 * 通过使用"!"操作符，可以否定表达式，如"!text/plain"，匹配所有{@code Content-Type}不是"text/plain"的请求。
	 * <p><b>既支持在类型级别上，也支持在方法级别上使用！</b>
	 * 如果在两个级别上都指定了consumes条件，那么方法级别的consumes条件将覆盖类型级别的条件。
	 * @see org.springframework.http.MediaType
	 * @see javax.servlet.http.HttpServletRequest#getContentType()
	 */
	String[] consumes() default {};

	/**
	 * Narrows the primary mapping by media types that can be produced by the
	 * mapped handler. Consists of one or more media types one of which must
	 * be chosen via content negotiation against the "acceptable" media types
	 * of the request. Typically those are extracted from the {@code "Accept"}
	 * header but may be derived from query parameters, or other. Examples:
	 * <pre class="code">
	 * produces = "text/plain"
	 * produces = {"text/plain", "application/*"}
	 * produces = MediaType.TEXT_PLAIN_VALUE
	 * produces = "text/plain;charset=UTF-8"
	 * </pre>
	 * <p>If a declared media type contains a parameter (e.g. "charset=UTF-8",
	 * "type=feed", "type=entry") and if a compatible media type from the request
	 * has that parameter too, then the parameter values must match. Otherwise
	 * if the media type from the request does not contain the parameter, it is
	 * assumed the client accepts any value.
	 * <p>Expressions can be negated by using the "!" operator, as in "!text/plain",
	 * which matches all requests with a {@code Accept} other than "text/plain".
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * If specified at both levels, the method level produces condition overrides
	 * the type level condition.
	 * 通过指定处理器可以产生的媒体类型来缩小主要映射范围。这包括一个或多个媒体类型，其中必须通过内容协商选择一个与请求的"可接受"媒体类型相匹配。
	 * 通常，这些媒体类型从"Accept"头部提取，但也可能来自查询参数或其他来源。例如：
	 * <pre class="code">
	 * produces = "text/plain"
	 * produces = {"text/plain", "application/*"}
	 * produces = MediaType.TEXT_PLAIN_VALUE
	 * produces = "text/plain;charset=UTF-8"
	 * </pre>
	 * <p>如果声明的媒体类型包含参数（例如"charset=UTF-8"、"type=feed"、"type=entry"），且请求中的兼容媒体类型也包含该参数，则参数值必须匹配。否则，如果请求中的媒体类型不包含该参数，则假设客户端接受任何值。
	 * <p>可以使用"!"操作符对表达式进行否定，如"!text/plain"，这匹配除"text/plain"外的所有请求的{@code Accept}。
	 * <p><b>既支持在类型级别上，也支持在方法级别上！</b>如果在两个级别上都指定了，则方法级别的produces条件将覆盖类型级别的条件。
	 *
	 * @see org.springframework.http.MediaType
	 */
	String[] produces() default {};

}
