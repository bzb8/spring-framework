/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.multipart;

import javax.servlet.http.HttpServletRequest;

/**
 * A strategy interface for multipart file upload resolution in accordance
 * with <a href="https://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>.
 * Implementations are typically usable both within an application context
 * and standalone.
 * <p>一个用于按照<a href="https://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>标准解析多部分文件上传的策略接口。
 * 实现这个接口的类通常既可以在应用上下文中使用，也可以独立使用。
 *
 * <p>There are two concrete implementations included in Spring, as of Spring 3.1:
 * <ul>
 * <li>{@link org.springframework.web.multipart.commons.CommonsMultipartResolver}
 * for Apache Commons FileUpload
 * <li>{@link org.springframework.web.multipart.support.StandardServletMultipartResolver}
 * for the Servlet 3.0+ Part API
 * </ul>
 * <p>从Spring 3.1开始，Spring提供了两个具体的实现：
 * <ul>
 * <li>{@link org.springframework.web.multipart.commons.CommonsMultipartResolver} 用于Apache Commons FileUpload</li>
 * <li>{@link org.springframework.web.multipart.support.StandardServletMultipartResolver} 用于Servlet 3.0+ Part API</li>
 * </ul>
 *
 * <p>There is no default resolver implementation used for Spring
 * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlets},
 * as an application might choose to parse its multipart requests itself. To define
 * an implementation, create a bean with the id "multipartResolver" in a
 * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet's}
 * application context. Such a resolver gets applied to all requests handled
 * by that {@link org.springframework.web.servlet.DispatcherServlet}.
 * <p>Spring的{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlets}没有默认的解析器实现，
 * 因为应用可能选择自己解析多部分请求。要定义一个实现，需要在{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
 * 的应用上下文中创建一个id为"multipartResolver"的bean。
 * 这样的解析器将被应用于该{@link org.springframework.web.servlet.DispatcherServlet}处理的所有请求。
 *
 * <p>If a {@link org.springframework.web.servlet.DispatcherServlet} detects a
 * multipart request, it will resolve it via the configured {@link MultipartResolver}
 * and pass on a wrapped {@link javax.servlet.http.HttpServletRequest}. Controllers
 * can then cast their given request to the {@link MultipartHttpServletRequest}
 * interface, which allows for access to any {@link MultipartFile MultipartFiles}.
 * Note that this cast is only supported in case of an actual multipart request.
 * <p>如果{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}检测到一个多部分请求，
 * 它将通过配置的{@link MultipartResolver}来解析，并传递一个包装过的{@link javax.servlet.http.HttpServletRequest}。
 * 控制器可以将给定的请求转换为{@link MultipartHttpServletRequest}接口，从而访问任何{@link MultipartFile MultipartFiles}。
 * 请注意，只有在实际的多部分请求时才支持这种转换。
 *
 * <pre class="code">
 * public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
 *   MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
 *   MultipartFile multipartFile = multipartRequest.getFile("image");
 *   ...
 * }</pre>
 *
 * Instead of direct access, command or form controllers can register a
 * {@link org.springframework.web.multipart.support.ByteArrayMultipartFileEditor}
 * or {@link org.springframework.web.multipart.support.StringMultipartFileEditor}
 * with their data binder, to automatically apply multipart content to form
 * bean properties.
 * 说明了如何在Spring的命令或表单控制器中处理multipart/form-data类型的数据。
 * 相反于直接访问，控制器可以通过向其数据绑定器注册
 * {@link org.springframework.web.multipart.support.ByteArrayMultipartFileEditor} 或
 * {@link org.springframework.web.multipart.support.StringMultipartFileEditor}，
 * 来自动将multipart内容应用到表单bean的属性中。
 *
 * <p>As an alternative to using a {@link MultipartResolver} with a
 * {@link org.springframework.web.servlet.DispatcherServlet},
 * a {@link org.springframework.web.multipart.support.MultipartFilter} can be
 * registered in {@code web.xml}. It will delegate to a corresponding
 * {@link MultipartResolver} bean in the root application context. This is mainly
 * intended for applications that do not use Spring's own web MVC framework.
 * 作为使用{@link MultipartResolver}与{@link org.springframework.web.servlet.DispatcherServlet}的替代方案，
 * 可以在{@code web.xml}中注册一个{@link org.springframework.web.multipart.support.MultipartFilter}。
 * 它将委托给根应用上下文中相应的{@link MultipartResolver} bean。这主要适用于不使用Spring自己的Web MVC框架的应用。
 *
 * <p>Note: There is hardly ever a need to access the {@link MultipartResolver}
 * itself from application code. It will simply do its work behind the scenes,
 * making {@link MultipartHttpServletRequest MultipartHttpServletRequests}
 * available to controllers.
 * <p>注意：几乎从来不需要从应用代码中直接访问{@link MultipartResolver}本身。
 * 它会在后台默默工作，使{@link MultipartHttpServletRequest MultipartHttpServletRequests} 对控制器可用。
 *
 * @author Juergen Hoeller
 * @author Trevor D. Cook
 * @since 29.09.2003
 * @see MultipartHttpServletRequest
 * @see MultipartFile
 * @see org.springframework.web.multipart.commons.CommonsMultipartResolver
 * @see org.springframework.web.multipart.support.ByteArrayMultipartFileEditor
 * @see org.springframework.web.multipart.support.StringMultipartFileEditor
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public interface MultipartResolver {

	/**
	 * Determine if the given request contains multipart content.
	 * <p>Will typically check for content type "multipart/form-data", but the actually
	 * accepted requests might depend on the capabilities of the resolver implementation.
	 * 判断给定的请求是否包含multipart内容。
	 * <p>通常会检查内容类型是否为"multipart/form-data"，但实际接受的请求可能取决于解析器实现的能力。
	 *
	 * @param request the servlet request to be evaluated
	 *                需要被评估的Servlet请求
	 * @return whether the request contains multipart content
	 * 如果请求包含multipart内容则返回true，否则返回false
	 */
	boolean isMultipart(HttpServletRequest request);

	/**
	 * Parse the given HTTP request into multipart files and parameters,
	 * and wrap the request inside a
	 * {@link org.springframework.web.multipart.MultipartHttpServletRequest}
	 * object that provides access to file descriptors and makes contained
	 * parameters accessible via the standard ServletRequest methods.
	 * <p>将给定的HTTP请求解析为multipart文件和参数，并将请求封装在一个
	 * {@link org.springframework.web.multipart.MultipartHttpServletRequest}
	 * 对象中，该对象提供对文件描述符的访问，并通过标准的ServletRequest方法使包含的参数可访问。
	 *
	 * @param request the servlet request to wrap (must be of a multipart content type)
	 * @return the wrapped servlet request
	 * @throws MultipartException if the servlet request is not multipart, or if
	 * implementation-specific problems are encountered (such as exceeding file size limits)
	 * @see MultipartHttpServletRequest#getFile
	 * @see MultipartHttpServletRequest#getFileNames
	 * @see MultipartHttpServletRequest#getFileMap
	 * @see javax.servlet.http.HttpServletRequest#getParameter
	 * @see javax.servlet.http.HttpServletRequest#getParameterNames
	 * @see javax.servlet.http.HttpServletRequest#getParameterMap
	 */
	MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException;

	/**
	 * Clean up any resources used for the multipart handling,
	 * like a storage for the uploaded files.
	 * @param request the request to clean up resources for
	 */
	void cleanupMultipart(MultipartHttpServletRequest request);

}
