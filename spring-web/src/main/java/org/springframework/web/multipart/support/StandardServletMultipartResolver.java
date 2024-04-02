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

package org.springframework.web.multipart.support;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.logging.LogFactory;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

/**
 * Standard implementation of the {@link MultipartResolver} interface,
 * based on the Servlet 3.0 {@link javax.servlet.http.Part} API.
 * To be added as "multipartResolver" bean to a Spring DispatcherServlet context,
 * without any extra configuration at the bean level (see below).
 *
 * <p>This resolver variant uses your Servlet container's multipart parser as-is,
 * potentially exposing the application to container implementation differences.
 * See {@link org.springframework.web.multipart.commons.CommonsMultipartResolver}
 * for an alternative implementation using a local Commons FileUpload library
 * within the application, providing maximum portability across Servlet containers.
 * Also, see this resolver's configuration option for
 * {@link #setStrictServletCompliance strict Servlet compliance}, narrowing the
 * applicability of Spring's {@link MultipartHttpServletRequest} to form data only.
 *
 * <p><b>Note:</b> In order to use Servlet 3.0 based multipart parsing,
 * you need to mark the affected servlet with a "multipart-config" section in
 * {@code web.xml}, or with a {@link javax.servlet.MultipartConfigElement}
 * in programmatic servlet registration, or (in case of a custom servlet class)
 * possibly with a {@link javax.servlet.annotation.MultipartConfig} annotation
 * on your servlet class. Configuration settings such as maximum sizes or
 * storage locations need to be applied at that servlet registration level;
 * Servlet 3.0 does not allow for them to be set at the MultipartResolver level.
 *
 * <pre class="code">
 * public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
 *	 // ...
 *	 &#064;Override
 *	 protected void customizeRegistration(ServletRegistration.Dynamic registration) {
 *     // Optionally also set maxFileSize, maxRequestSize, fileSizeThreshold
 *     registration.setMultipartConfig(new MultipartConfigElement("/tmp"));
 *   }
 * }
 * </pre>
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setResolveLazily
 * @see #setStrictServletCompliance
 * @see HttpServletRequest#getParts()
 * @see org.springframework.web.multipart.commons.CommonsMultipartResolver
 */
public class StandardServletMultipartResolver implements MultipartResolver {

	private boolean resolveLazily = false;

	private boolean strictServletCompliance = false;


	/**
	 * Set whether to resolve the multipart request lazily at the time of
	 * file or parameter access.
	 * <p>Default is "false", resolving the multipart elements immediately, throwing
	 * corresponding exceptions at the time of the {@link #resolveMultipart} call.
	 * Switch this to "true" for lazy multipart parsing, throwing parse exceptions
	 * once the application attempts to obtain multipart files or parameters.
	 * <p>设置是否在访问文件或参数时懒惰地解析多部分请求。
	 * <p>默认为 "false"，会在调用 {@link #resolveMultipart} 时立即解析多部分元素，并在那时抛出相应的异常。
	 * 将此设置为 "true" 可实现懒惰的多部分解析，只有在应用程序尝试获取多部分文件或参数时才会抛出解析异常。
	 * @param resolveLazily 指定是否懒惰解析多部分请求。如果为 true，则延迟解析；如果为 false，则立即解析。
	 * @since 3.2.9
	 */
	public void setResolveLazily(boolean resolveLazily) {
		this.resolveLazily = resolveLazily;
	}

	/**
	 * Specify whether this resolver should strictly comply with the Servlet
	 * specification, only kicking in for "multipart/form-data" requests.
	 * <p>Default is "false", trying to process any request with a "multipart/"
	 * content type as far as the underlying Servlet container supports it
	 * (which works on e.g. Tomcat but not on Jetty). For consistent portability
	 * and in particular for consistent custom handling of non-form multipart
	 * request types outside of Spring's {@link MultipartResolver} mechanism,
	 * switch this flag to "true": Only "multipart/form-data" requests will be
	 * wrapped with a {@link MultipartHttpServletRequest} then; other kinds of
	 * requests will be left as-is, allowing for custom processing in user code.
	 * <p>Note that Commons FileUpload and therefore
	 * {@link org.springframework.web.multipart.commons.CommonsMultipartResolver}
	 * supports any "multipart/" request type. However, it restricts processing
	 * to POST requests which standard Servlet multipart parsers might not do.
	 * <p>设置此解析器是否应严格遵守Servlet规范，仅在"multipart/form-data"请求时启动。
	 * <p>默认为"false"，尝试处理任何具有"multipart/"内容类型的请求，只要底层Servlet容器支持它
	 * （例如在Tomcat上工作，但在Jetty上不工作）。为了实现一致的可移植性，特别是为了在Spring的{@link MultipartResolver}机制之外
	 * 一致地自定义处理非表单multipart请求类型，将此标志切换为"true"：此时仅将"multipart/form-data"请求包装为{@link MultipartHttpServletRequest}；
	 * 其他类型的请求将保持原样，允许在用户代码中进行自定义处理。
	 * <p>请注意，Commons FileUpload和因此{@link org.springframework.web.multipart.commons.CommonsMultipartResolver}
	 * 支持任何"multipart/"请求类型。然而，它将处理限制为POST请求，而标准Servlet多部分解析器可能不会这样做。
	 *
	 * @since 5.3.9
	 */
	public void setStrictServletCompliance(boolean strictServletCompliance) {
		this.strictServletCompliance = strictServletCompliance;
	}


	@Override
	public boolean isMultipart(HttpServletRequest request) {
		return StringUtils.startsWithIgnoreCase(request.getContentType(),
				(this.strictServletCompliance ? MediaType.MULTIPART_FORM_DATA_VALUE : "multipart/"));
	}

	@Override
	public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
		return new StandardMultipartHttpServletRequest(request, this.resolveLazily);
	}

	@Override
	public void cleanupMultipart(MultipartHttpServletRequest request) {
		if (!(request instanceof AbstractMultipartHttpServletRequest) ||
				((AbstractMultipartHttpServletRequest) request).isResolved()) {
			// To be on the safe side: explicitly delete the parts,
			// but only actual file parts (for Resin compatibility)
			try {
				for (Part part : request.getParts()) {
					if (request.getFile(part.getName()) != null) {
						part.delete();
					}
				}
			}
			catch (Throwable ex) {
				LogFactory.getLog(getClass()).warn("Failed to perform cleanup of multipart items", ex);
			}
		}
	}

}
