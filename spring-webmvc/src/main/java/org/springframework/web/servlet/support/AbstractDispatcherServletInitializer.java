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

package org.springframework.web.servlet.support;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.AbstractContextLoaderInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FrameworkServlet;

/**
 * Base class for {@link org.springframework.web.WebApplicationInitializer}
 * implementations that register a {@link DispatcherServlet} in the servlet context.
 * <p>{@link org.springframework.web.WebApplicationInitializer} 实现的基类，
 * 用于在 servlet 上下文中注册一个 {@link DispatcherServlet}。
 *
 * <p>Most applications should consider extending the Spring Java config subclass
 * {@link AbstractAnnotationConfigDispatcherServletInitializer}.
 * <p>大多数应用程序应考虑扩展 Spring Java 配置子类
 * {@link AbstractAnnotationConfigDispatcherServletInitializer}。
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.2
 */
public abstract class AbstractDispatcherServletInitializer extends AbstractContextLoaderInitializer {

	/**
	 * The default servlet name. Can be customized by overriding {@link #getServletName}.
	 */
	public static final String DEFAULT_SERVLET_NAME = "dispatcher";


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		// 初始化并注册根上下文
		super.onStartup(servletContext);
		registerDispatcherServlet(servletContext);
	}

	/**
	 * Register a {@link DispatcherServlet} against the given servlet context.
	 * <p>This method will create a {@code DispatcherServlet} with the name returned by
	 * {@link #getServletName()}, initializing it with the application context returned
	 * from {@link #createServletApplicationContext()}, and mapping it to the patterns
	 * returned from {@link #getServletMappings()}.
	 * <p>Further customization can be achieved by overriding {@link
	 * #customizeRegistration(ServletRegistration.Dynamic)} or
	 * {@link #createDispatcherServlet(WebApplicationContext)}.
	 * <p>在给定的servlet上下文中注册一个{@link DispatcherServlet}。
	 * <p>此方法将使用{@link #getServletName()}返回的名称创建一个{@code DispatcherServlet}，
	 * 并使用{@link #createServletApplicationContext()}返回的应用上下文对其进行初始化，
	 * 并将其映射到{@link #getServletMappings()}返回的模式。
	 * <p>可以通过重写{@link #customizeRegistration(ServletRegistration.Dynamic)}或
	 * {@link #createDispatcherServlet(WebApplicationContext)}进一步进行定制。
	 ** @param servletContext the context to register the servlet against
	 */
	protected void registerDispatcherServlet(ServletContext servletContext) {
		String servletName = getServletName();
		Assert.hasLength(servletName, "getServletName() must not return null or empty");

		// 创建servlet上下文AnnotationConfigWebApplicationContext
		WebApplicationContext servletAppContext = createServletApplicationContext();
		Assert.notNull(servletAppContext, "createServletApplicationContext() must not return null");

		// 创建DispatcherServlet
		FrameworkServlet dispatcherServlet = createDispatcherServlet(servletAppContext);
		Assert.notNull(dispatcherServlet, "createDispatcherServlet(WebApplicationContext) must not return null");
		dispatcherServlet.setContextInitializers(getServletApplicationContextInitializers());

		// 在servlet上下文注册DispatcherServlet
		ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, dispatcherServlet);
		if (registration == null) {
			throw new IllegalStateException("Failed to register servlet with name '" + servletName + "'. " +
					"Check if there is another servlet registered under the same name.");
		}

		registration.setLoadOnStartup(1);
		registration.addMapping(getServletMappings());
		// 设置异步支持
		registration.setAsyncSupported(isAsyncSupported());

		// 指定要应用于dispatcherServlet的过滤器
		Filter[] filters = getServletFilters();
		if (!ObjectUtils.isEmpty(filters)) {
			for (Filter filter : filters) {
				registerServletFilter(servletContext, filter);
			}
		}

		customizeRegistration(registration);
	}

	/**
	 * Return the name under which the {@link DispatcherServlet} will be registered.
	 * Defaults to {@link #DEFAULT_SERVLET_NAME}.
	 * 返回将注册 {@link DispatcherServlet} 的名称。默认为 {@link #DEFAULT_SERVLET_NAME}。
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected String getServletName() {
		return DEFAULT_SERVLET_NAME;
	}

	/**
	 * Create a servlet application context to be provided to the {@code DispatcherServlet}.
	 * <p>The returned context is delegated to Spring's
	 * {@link DispatcherServlet#DispatcherServlet(WebApplicationContext)}. As such,
	 * it typically contains controllers, view resolvers, locale resolvers, and other
	 * web-related beans.
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected abstract WebApplicationContext createServletApplicationContext();

	/**
	 * Create a {@link DispatcherServlet} (or other kind of {@link FrameworkServlet}-derived
	 * dispatcher) with the specified {@link WebApplicationContext}.
	 * <p>Note: This allows for any {@link FrameworkServlet} subclass as of 4.2.3.
	 * Previously, it insisted on returning a {@link DispatcherServlet} or subclass thereof.
	 * <p>创建一个 {@link DispatcherServlet}（或其他 {@link FrameworkServlet} 派生的分发器），并将其与指定的 {@link WebApplicationContext} 关联。
	 * <p>注意：此方法自 4.2.3 版本起允许任何 {@link FrameworkServlet} 子类作为返回类型。
	 * 之前，它坚持返回 {@link DispatcherServlet} 或其子类。
	 *
	 */
	protected FrameworkServlet createDispatcherServlet(WebApplicationContext servletAppContext) {
		return new DispatcherServlet(servletAppContext);
	}

	/**
	 * Specify application context initializers to be applied to the servlet-specific
	 * application context that the {@code DispatcherServlet} is being created with.
	 * @since 4.2
	 * @see #createServletApplicationContext()
	 * @see DispatcherServlet#setContextInitializers
	 * @see #getRootApplicationContextInitializers()
	 */
	@Nullable
	protected ApplicationContextInitializer<?>[] getServletApplicationContextInitializers() {
		return null;
	}

	/**
	 * Specify the servlet mapping(s) for the {@code DispatcherServlet} &mdash;
	 * for example {@code "/"}, {@code "/app"}, etc.
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected abstract String[] getServletMappings();

	/**
	 * Specify filters to add and map to the {@code DispatcherServlet}.
	 * @return an array of filters or {@code null}
	 * @see #registerServletFilter(ServletContext, Filter)
	 */
	@Nullable
	protected Filter[] getServletFilters() {
		return null;
	}

	/**
	 * Add the given filter to the ServletContext and map it to the
	 * {@code DispatcherServlet} as follows:
	 * <ul>
	 * <li>a default filter name is chosen based on its concrete type
	 * <li>the {@code asyncSupported} flag is set depending on the
	 * return value of {@link #isAsyncSupported() asyncSupported}
	 * <li>a filter mapping is created with dispatcher types {@code REQUEST},
	 * {@code FORWARD}, {@code INCLUDE}, and conditionally {@code ASYNC} depending
	 * on the return value of {@link #isAsyncSupported() asyncSupported}
	 * </ul>
	 * <p>If the above defaults are not suitable or insufficient, override this
	 * method and register filters directly with the {@code ServletContext}.
	 * @param servletContext the servlet context to register filters with
	 * @param filter the filter to be registered
	 * @return the filter registration
	 */
	/**
	 * 将给定的过滤器添加到ServletContext，并将其映射到{@code DispatcherServlet}，具体操作如下：
	 * <ul>
	 * <li>根据过滤器的具体类型选择一个默认的过滤器名称；
	 * <li>设置{@code asyncSupported}标志，其值取决于{@link #isAsyncSupported() asyncSupported}方法的返回值；
	 * <li>创建一个过滤器映射，其调度类型为{@code REQUEST}、{@code FORWARD}、{@code INCLUDE}，并且根据{@link #isAsyncSupported() asyncSupported}方法的返回值，条件性地添加{@code ASYNC}调度类型。
	 * </ul>
	 * <p>如果默认设置不适合或不充分，可以重写此方法，直接使用{@code ServletContext}注册过滤器。
	 * @param servletContext 用于注册过滤器的servlet上下文
	 * @param filter 要注册的过滤器
	 * @return 过滤器注册信息
	 */
	protected FilterRegistration.Dynamic registerServletFilter(ServletContext servletContext, Filter filter) {
		String filterName = Conventions.getVariableName(filter);
		// 添加过滤器
		Dynamic registration = servletContext.addFilter(filterName, filter);

		// 从100开始递增，避免冲突
		if (registration == null) {
			int counter = 0;
			while (registration == null) {
				if (counter == 100) {
					throw new IllegalStateException("Failed to register filter with name '" + filterName + "'. " +
							"Check if there is another filter registered under the same name.");
				}
				registration = servletContext.addFilter(filterName + "#" + counter, filter);
				counter++;
			}
		}

		registration.setAsyncSupported(isAsyncSupported());
		registration.addMappingForServletNames(getDispatcherTypes(), false, getServletName());
		return registration;
	}

	private EnumSet<DispatcherType> getDispatcherTypes() {
		return (isAsyncSupported() ?
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ASYNC) :
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE));
	}

	/**
	 * A single place to control the {@code asyncSupported} flag for the
	 * {@code DispatcherServlet} and all filters added via {@link #getServletFilters()}.
	 * <p>The default value is "true".
	 */
	protected boolean isAsyncSupported() {
		return true;
	}

	/**
	 * Optionally perform further registration customization once
	 * {@link #registerDispatcherServlet(ServletContext)} has completed.
	 * @param registration the {@code DispatcherServlet} registration to be customized
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {
	}

}
