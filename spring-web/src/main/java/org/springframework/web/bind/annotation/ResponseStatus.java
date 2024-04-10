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

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;

/**
 * Marks a method or exception class with the status {@link #code} and
 * {@link #reason} that should be returned.
 * <p>用指定的状态码{@link #code}和原因{@link #reason}标记方法或异常类。
 *
 * <p>The status code is applied to the HTTP response when the handler
 * method is invoked and overrides status information set by other means,
 * like {@code ResponseEntity} or {@code "redirect:"}.
 * <p>当处理器方法被调用时，该状态码会被应用到HTTP响应中，并且会覆盖通过其他方式（如{@code ResponseEntity}或"redirect:"）设置的状态信息。
 *
 * <p><strong>Warning</strong>: when using this annotation on an exception
 * class, or when setting the {@code reason} attribute of this annotation,
 * the {@code HttpServletResponse.sendError} method will be used.
 * <p><strong>警告</strong>：当将此注解用于异常类上，或当设置此注解的{@code reason}属性时，会使用{@code HttpServletResponse.sendError}方法。
 *
 * <p>With {@code HttpServletResponse.sendError}, the response is considered
 * complete and should not be written to any further. Furthermore, the Servlet
 * container will typically write an HTML error page therefore making the
 * use of a {@code reason} unsuitable for REST APIs. For such cases it is
 * preferable to use a {@link org.springframework.http.ResponseEntity} as
 * a return type and avoid the use of {@code @ResponseStatus} altogether.
 * <p>使用{@code HttpServletResponse.sendError}方法时，响应被认为已完成，不应再进行任何写入操作。此外，Servlet容器通常会写入一个HTML错误页面，
 * 因此在REST API中使用{@code reason}并不合适。对于此类情况，建议使用{@link org.springframework.http.ResponseEntity}作为返回类型，
 * 并避免完全使用{@code @ResponseStatus}。
 *
 * <p>Note that a controller class may also be annotated with
 * {@code @ResponseStatus} which is then inherited by all {@code @RequestMapping}
 * and {@code @ExceptionHandler} methods in that class and its subclasses unless
 * overridden by a local {@code @ResponseStatus} declaration on the method.
 * <p>注意，控制器类也可以被注解{@code @ResponseStatus}标记，该注解会被其所有{@code @RequestMapping}和{@code @ExceptionHandler}方法以及子类继承，
 * 除非方法上有本地{@code @ResponseStatus}声明进行覆盖。
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver
 * @see javax.servlet.http.HttpServletResponse#sendError(int, String)
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseStatus {

	/**
	 * Alias for {@link #code}.
	 */
	@AliasFor("code")
	HttpStatus value() default HttpStatus.INTERNAL_SERVER_ERROR;

	/**
	 * The status <em>code</em> to use for the response.
	 * <p>Default is {@link HttpStatus#INTERNAL_SERVER_ERROR}, which should
	 * typically be changed to something more appropriate.
	 * 用于响应的状态码。
	 * <p>默认为{@link HttpStatus#INTERNAL_SERVER_ERROR}，通常应该更改为更合适的状态码。
	 *
	 * @since 4.2
	 * @see javax.servlet.http.HttpServletResponse#setStatus(int)
	 * @see javax.servlet.http.HttpServletResponse#sendError(int)
	 */
	@AliasFor("value")
	HttpStatus code() default HttpStatus.INTERNAL_SERVER_ERROR;

	/**
	 * The <em>reason</em> to be used for the response.
	 * <p>Defaults to an empty string which will be ignored. Set the reason to a
	 * non-empty value to have it used for the response.
	 * 用于响应的"<em>原因</em>"。
	 * <p>默认为空字符串，将被忽略。将原因设置为非空值，以便在响应中使用。
	 *
	 * @see javax.servlet.http.HttpServletResponse#sendError(int, String)
	 */
	String reason() default "";

}
