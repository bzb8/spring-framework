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

package org.springframework.web;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * Servlet 3.0 {@link ServletContainerInitializer} designed to support code-based
 * configuration of the servlet container using Spring's {@link WebApplicationInitializer}
 * SPI as opposed to (or possibly in combination with) the traditional
 * {@code web.xml}-based approach.
 *  Servlet 3.0 {@link ServletContainerInitializer} 旨在支持使用 Spring 的 {@link WebApplicationInitializer} SPI
 *  以代码方式配置Servlet容器，而不是（或者可能与）传统的{@code web.xml}方式。
 *
 * <h2>Mechanism of Operation</h2>
 * This class will be loaded and instantiated and have its {@link #onStartup}
 * method invoked by any Servlet 3.0-compliant container during container startup assuming
 * that the {@code spring-web} module JAR is present on the classpath. This occurs through
 * the JAR Services API {@link ServiceLoader#load(Class)} method detecting the
 * {@code spring-web} module's {@code META-INF/services/javax.servlet.ServletContainerInitializer}
 * service provider configuration file. See the
 * <a href="https://download.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Service%20Provider">
 * JAR Services API documentation</a> as well as section <em>8.2.4</em> of the Servlet 3.0
 * Final Draft specification for complete details.
 * <h2>操作机制</h2>
 * 此类将在容器启动时由任何符合Servlet 3.0规范的容器加载、实例化并调用其{@link #onStartup}方法，
 * 前提是{@code spring-web}模块JAR存在于类路径中。这通过JAR Services API的{@link ServiceLoader#load(Class)}方法
 * 检测到{@code spring-web}模块的{@code META-INF/services/javax.servlet.ServletContainerInitializer}服务提供者配置文件实现。
 * 有关完整细节，请参阅<a href="https://download.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Service%20Provider">JAR Services API 文档</a>
 * 以及Servlet 3.0最终规范草案第<em>8.2.4</em>节。
 *
 * <h3>In combination with {@code web.xml}</h3>
 * A web application can choose to limit the amount of classpath scanning the Servlet
 * container does at startup either through the {@code metadata-complete} attribute in
 * {@code web.xml}, which controls scanning for Servlet annotations or through an
 * {@code <absolute-ordering>} element also in {@code web.xml}, which controls which
 * web fragments (i.e. jars) are allowed to perform a {@code ServletContainerInitializer}
 * scan. When using this feature, the {@link SpringServletContainerInitializer}
 * can be enabled by adding "spring_web" to the list of named web fragments in
 * {@code web.xml} as follows:
 * <h3>与{@code web.xml}结合使用</h3>
 * Web应用程序可以选择通过在{@code web.xml}中设置{@code metadata-complete}属性来限制容器在启动时进行的类路径扫描，
 * 以控制Servlet注解的扫描，或者通过在{@code web.xml}中添加{@code <absolute-ordering>}元素来控制允许进行
 * {@code ServletContainerInitializer}扫描的web片段（即jar）。使用此功能，可以通过在{@code web.xml}中将"spring_web"添加到
 * 名称web片段列表中来启用{@link SpringServletContainerInitializer}，如下所示：
 *
 * <pre class="code">
 * &lt;absolute-ordering&gt;
 *   &lt;name&gt;some_web_fragment&lt;/name&gt;
 *   &lt;name&gt;spring_web&lt;/name&gt;
 * &lt;/absolute-ordering&gt;
 * </pre>
 *
 * <h2>Relationship to Spring's {@code WebApplicationInitializer}</h2>
 * Spring's {@code WebApplicationInitializer} SPI consists of just one method:
 * {@link WebApplicationInitializer#onStartup(ServletContext)}. The signature is intentionally
 * quite similar to {@link ServletContainerInitializer#onStartup(Set, ServletContext)}:
 * simply put, {@code SpringServletContainerInitializer} is responsible for instantiating
 * and delegating the {@code ServletContext} to any user-defined
 * {@code WebApplicationInitializer} implementations. It is then the responsibility of
 * each {@code WebApplicationInitializer} to do the actual work of initializing the
 * {@code ServletContext}. The exact process of delegation is described in detail in the
 * {@link #onStartup onStartup} documentation below.
 * <h2>与Spring的{@code WebApplicationInitializer}的关系</h2>
 * Spring的{@code WebApplicationInitializer} SPI仅包括一个方法：{@link WebApplicationInitializer#onStartup(ServletContext)}。
 * 其签名故意与{@link ServletContainerInitializer#onStartup(Set, ServletContext)}非常相似：
 * 简而言之，{@code SpringServletContainerInitializer}负责实例化并将{@code ServletContext}委托给任何用户定义的
 * {@code WebApplicationInitializer}实现。然后，每个{@code WebApplicationInitializer}负责实际初始化{@code ServletContext}的工作。
 * 委托的确切过程在下面的{@link #onStartup onStartup}文档中详细描述。
 *
 * <h2>General Notes</h2>
 * In general, this class should be viewed as <em>supporting infrastructure</em> for
 * the more important and user-facing {@code WebApplicationInitializer} SPI. Taking
 * advantage of this container initializer is also completely <em>optional</em>: while
 * it is true that this initializer will be loaded and invoked under all Servlet 3.0+
 * runtimes, it remains the user's choice whether to make any
 * {@code WebApplicationInitializer} implementations available on the classpath. If no
 * {@code WebApplicationInitializer} types are detected, this container initializer will
 * have no effect.
 * <h2>一般注意事项</h2>
 * 通常，应将此类视为{@code WebApplicationInitializer}的<em>支持基础设施</em>。
 * 使用此容器初始化程序也是<em>可选的</em>：虽然此初始化程序将在所有Servlet 3.0+运行时加载并调用，
 * 但是否将任何{@code WebApplicationInitializer}实现放在类路径上仍由用户选择。如果没有检测到{@code WebApplicationInitializer}类型，
 * 此容器初始化程序将没有任何效果。
 *
 * <p>Note that use of this container initializer and of {@code WebApplicationInitializer}
 * is not in any way "tied" to Spring MVC other than the fact that the types are shipped
 * in the {@code spring-web} module JAR. Rather, they can be considered general-purpose
 * in their ability to facilitate convenient code-based configuration of the
 * {@code ServletContext}. In other words, any servlet, listener, or filter may be
 * registered within a {@code WebApplicationInitializer}, not just Spring MVC-specific
 * components.
 * <p>请注意，使用此容器初始化程序和{@code WebApplicationInitializer}并不以任何方式“绑定”到Spring MVC，
 * 除了这些类型是在{@code spring-web}模块JAR中提供的事实。相反，可以将它们视为方便地以代码方式配置
 * {@code ServletContext}的通用目的工具。换句话说，任何servlet、listener或filter都可在{@code WebApplicationInitializer}中注册，
 * 而不仅仅是Spring MVC特定的组件。
 *
 * <p>This class is neither designed for extension nor intended to be extended.
 * It should be considered an internal type, with {@code WebApplicationInitializer}
 * being the public-facing SPI.
 * <p>此类既不设计为可扩展，也不打算被扩展。应将其视为内部类型，而{@code WebApplicationInitializer}是面向公众的SPI。
 *
 * <h2>See Also</h2>
 * See {@link WebApplicationInitializer} Javadoc for examples and detailed usage
 * recommendations.<p>
 * <h2>参见</h2>
 * 请参阅{@link WebApplicationInitializer} Javadoc，了解示例和详细的使用建议。<p>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see #onStartup(Set, ServletContext)
 * @see WebApplicationInitializer
 */
@HandlesTypes(WebApplicationInitializer.class)
public class SpringServletContainerInitializer implements ServletContainerInitializer {

	/**
	 * Delegate the {@code ServletContext} to any {@link WebApplicationInitializer}
	 * implementations present on the application classpath.
	 * <p>Because this class declares @{@code HandlesTypes(WebApplicationInitializer.class)},
	 * Servlet 3.0+ containers will automatically scan the classpath for implementations
	 * of Spring's {@code WebApplicationInitializer} interface and provide the set of all
	 * such types to the {@code webAppInitializerClasses} parameter of this method.
	 * <p>If no {@code WebApplicationInitializer} implementations are found on the classpath,
	 * this method is effectively a no-op. An INFO-level log message will be issued notifying
	 * the user that the {@code ServletContainerInitializer} has indeed been invoked but that
	 * no {@code WebApplicationInitializer} implementations were found.
	 * <p>Assuming that one or more {@code WebApplicationInitializer} types are detected,
	 * they will be instantiated (and <em>sorted</em> if the @{@link
	 * org.springframework.core.annotation.Order @Order} annotation is present or
	 * the {@link org.springframework.core.Ordered Ordered} interface has been
	 * implemented). Then the {@link WebApplicationInitializer#onStartup(ServletContext)}
	 * method will be invoked on each instance, delegating the {@code ServletContext} such
	 * that each instance may register and configure servlets such as Spring's
	 * {@code DispatcherServlet}, listeners such as Spring's {@code ContextLoaderListener},
	 * or any other Servlet API componentry such as filters.
	 * --
	 * {@code ServletContext} 委派给应用程序类路径上存在的任何 {@link WebApplicationInitializer} 实现。
	 * <p>由于此类声明了 @{@code HandlesTypes(WebApplicationInitializer.class)}，
	 * Servlet 3.0+ 容器会自动扫描类路径上 {@code WebApplicationInitializer} 接口的实现，
	 * 并将所有此类类型的集合提供给这个方法的 {@code webAppInitializerClasses} 参数。
	 * <p>如果在类路径上找不到 {@code WebApplicationInitializer} 实现，
	 * 则此方法实际上是一个空操作。将记录一条 INFO 级别的日志消息，
	 * 通知用户 {@code ServletContainerInitializer} 确实被调用，但未找到任何 {@code WebApplicationInitializer} 实现。
	 * <p>假设检测到一个或多个 {@code WebApplicationInitializer} 类型，
	 * 它们将被实例化（如果存在 @{@link org.springframework.core.annotation.Order @Order} 注解或
	 * 实现了 {@link org.springframework.core.Ordered Ordered} 接口，则进行 <em>排序</em>）。
	 * 然后，将调用每个实例的 {@link WebApplicationInitializer#onStartup(ServletContext)} 方法，
	 * 委派 {@code ServletContext}，以便每个实例可以注册和配置如Spring的 {@code DispatcherServlet}，
	 * 如Spring的 {@code ContextLoaderListener} 的监听器，或任何其他Servlet API组件，如过滤器。
	 *
	 * @param webAppInitializerClasses all implementations of
	 * {@link WebApplicationInitializer} found on the application classpath
	 *                                 应用程序类路径上找到的所有 {@link WebApplicationInitializer} 实现
	 * @param servletContext the servlet context to be initialized -- 初始化的 servlet 上下文,
	 *                       代表当前Web应用的ServletContext；一个Web应用一个ServletContext
	 * @see WebApplicationInitializer#onStartup(ServletContext)
	 * @see AnnotationAwareOrderComparator
	 * --
	 * 由tomcat负责加载ServletContainerInitializer的实现类（通过SPI的方式，加载META-INF/services/下的文件），
	 * 通过 @HandlesTypes 注解告知Servlet容器筛选加载它的value()属性指定的classes，并作为第1个参数传入 onStartup
	 *
	 */
	@Override
	public void onStartup(@Nullable Set<Class<?>> webAppInitializerClasses, ServletContext servletContext)
			throws ServletException {
		// 初始化WebApplicationInitializer实现类的集合
		List<WebApplicationInitializer> initializers = Collections.emptyList();

		if (webAppInitializerClasses != null) {
			initializers = new ArrayList<>(webAppInitializerClasses.size());
			for (Class<?> waiClass : webAppInitializerClasses) {
				// Be defensive: Some servlet containers provide us with invalid classes,
				// no matter what @HandlesTypes says...
				// 防御：无论@HandlesTypes说什么，一些 servlet 容器都为我们提供了无效的类......
				// WebApplicationInitializer类不是接口 && 它不是抽象类 && 它是WebApplicationInitializer派生类
				if (!waiClass.isInterface() && !Modifier.isAbstract(waiClass.getModifiers()) &&
						WebApplicationInitializer.class.isAssignableFrom(waiClass)) {
					try {
						// 使用无参构造函数实例化
						initializers.add((WebApplicationInitializer)
								ReflectionUtils.accessibleConstructor(waiClass).newInstance());
					}
					catch (Throwable ex) {
						throw new ServletException("Failed to instantiate WebApplicationInitializer class", ex);
					}
				}
			}
		}

		if (initializers.isEmpty()) {
			servletContext.log("No Spring WebApplicationInitializer types detected on classpath");
			return;
		}

		servletContext.log(initializers.size() + " Spring WebApplicationInitializers detected on classpath");
		// 按照Order排序
		AnnotationAwareOrderComparator.sort(initializers);
		// 调用WebApplicationInitializer的onStartup方法
		for (WebApplicationInitializer initializer : initializers) {
			initializer.onStartup(servletContext);
		}
	}

}
