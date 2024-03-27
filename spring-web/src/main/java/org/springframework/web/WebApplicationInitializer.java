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

package org.springframework.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Interface to be implemented in Servlet 3.0+ environments in order to configure the
 * {@link ServletContext} programmatically -- as opposed to (or possibly in conjunction
 * with) the traditional {@code web.xml}-based approach.
 * <p>该接口用于在Servlet 3.0+环境中实现程序化配置{@link ServletContext}，作为传统{@code web.xml}配置方式的替代或补充。
 *
 * <p>Implementations of this SPI will be detected automatically by {@link
 * SpringServletContainerInitializer}, which itself is bootstrapped automatically
 * by any Servlet 3.0 container. See {@linkplain SpringServletContainerInitializer its
 * Javadoc} for details on this bootstrapping mechanism.
 * <p>实现此SPI的类将被{@link SpringServletContainerInitializer}自动检测到，而后者又会被任何Servlet 3.0容器
 * 自动引导。关于此引导机制的详细信息，请参阅{@link SpringServletContainerInitializer}的Javadoc。
 *
 * <h2>Example</h2>
 * <h3>The traditional, XML-based approach</h3>
 * Most Spring users building a web application will need to register Spring's {@code
 * DispatcherServlet}. For reference, in WEB-INF/web.xml, this would typically be done as
 * follows:
 * <h2>示例</h2>
 * <h3>传统的XML配置方式</h3>
 * <p>大多数Spring用户在构建Web应用时需要注册Spring的{@code DispatcherServlet}。以参考为目的，在WEB-INF/web.xml中，
 * 通常会按以下方式配置：
 * <pre class="code">
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;dispatcher&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;
 *     org.springframework.web.servlet.DispatcherServlet
 *   &lt;/servlet-class&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;contextConfigLocation&lt;/param-name&gt;
 *     &lt;param-value&gt;/WEB-INF/spring/dispatcher-config.xml&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 *   &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;dispatcher&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;/&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;</pre>
 *
 * <h3>The code-based approach with {@code WebApplicationInitializer}</h3>
 * Here is the equivalent {@code DispatcherServlet} registration logic,
 * {@code WebApplicationInitializer}-style:
 *  <h3>使用{@code WebApplicationInitializer}的代码配置方式</h3>
 *  以下是等效的{@code DispatcherServlet}注册逻辑，采用{@code WebApplicationInitializer}风格：
 * <pre class="code">
 * public class MyWebAppInitializer implements WebApplicationInitializer {
 *
 *    &#064;Override
 *    public void onStartup(ServletContext container) {
 *      XmlWebApplicationContext appContext = new XmlWebApplicationContext();
 *      appContext.setConfigLocation("/WEB-INF/spring/dispatcher-config.xml");
 *
 *      ServletRegistration.Dynamic dispatcher =
 *        container.addServlet("dispatcher", new DispatcherServlet(appContext));
 *      dispatcher.setLoadOnStartup(1);
 *      dispatcher.addMapping("/");
 *    }
 *
 * }</pre>
 *
 * As an alternative to the above, you can also extend from {@link
 * org.springframework.web.servlet.support.AbstractDispatcherServletInitializer}.
 * <p>作为上述示例的替代，您还可以扩展自{@link org.springframework.web.servlet.support.AbstractDispatcherServletInitializer}。
 *
 * As you can see, thanks to Servlet 3.0's new {@link ServletContext#addServlet} method
 * we're actually registering an <em>instance</em> of the {@code DispatcherServlet}, and
 * this means that the {@code DispatcherServlet} can now be treated like any other object
 * -- receiving constructor injection of its application context in this case.
 * <p>如您所见，借助Servlet 3.0新提供的{@link ServletContext#addServlet}方法，我们现在实际上注册了一个{@code DispatcherServlet}的
 * 实例，这意味着{@code DispatcherServlet}现在可以像其他对象一样被处理——在此例中，通过构造函数注入其应用上下文。
 *
 * <p>This style is both simpler and more concise. There is no concern for dealing with
 * init-params, etc, just normal JavaBean-style properties and constructor arguments. You
 * are free to create and work with your Spring application contexts as necessary before
 * injecting them into the {@code DispatcherServlet}.
 * <p>这种方式既更简单又更紧凑。无需处理init-params等，只需正常的JavaBean风格的属性和构造函数参数即可。您可以自由地创建
 * 和处理Spring应用上下文，然后将它们注入到{@code DispatcherServlet}中。
 *
 * <p>Most major Spring Web components have been updated to support this style of
 * registration.  You'll find that {@code DispatcherServlet}, {@code FrameworkServlet},
 * {@code ContextLoaderListener} and {@code DelegatingFilterProxy} all now support
 * constructor arguments. Even if a component (e.g. non-Spring, other third party) has not
 * been specifically updated for use within {@code WebApplicationInitializers}, they still
 * may be used in any case. The Servlet 3.0 {@code ServletContext} API allows for setting
 * init-params, context-params, etc programmatically.
 * <p>大多数Spring Web组件都已更新，以支持这种注册方式。您会发现{@code DispatcherServlet}、{@code FrameworkServlet}、
 * {@code ContextLoaderListener}和{@code DelegatingFilterProxy}现在都支持构造函数参数。即使某个组件（例如非Spring，或其他第三方）
 * 尚未被特别更新以在{@code WebApplicationInitializers}中使用，它们仍然可以以任何方式被使用。Servlet 3.0的{@code ServletContext} API
 * 允许程序化设置init-params、context-params等。
 *
 * <h2>A 100% code-based approach to configuration</h2>
 * In the example above, {@code WEB-INF/web.xml} was successfully replaced with code in
 * the form of a {@code WebApplicationInitializer}, but the actual
 * {@code dispatcher-config.xml} Spring configuration remained XML-based.
 * {@code WebApplicationInitializer} is a perfect fit for use with Spring's code-based
 * {@code @Configuration} classes. See @{@link
 * org.springframework.context.annotation.Configuration Configuration} Javadoc for
 * complete details, but the following example demonstrates refactoring to use Spring's
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext
 * AnnotationConfigWebApplicationContext} in lieu of {@code XmlWebApplicationContext}, and
 * user-defined {@code @Configuration} classes {@code AppConfig} and
 * {@code DispatcherConfig} instead of Spring XML files. This example also goes a bit
 * beyond those above to demonstrate typical configuration of the 'root' application
 * context and registration of the {@code ContextLoaderListener}:
 * <h2>纯代码配置方式</h2>
 * <p>在上面的示例中，{@code WEB-INF/web.xml}成功地被代码形式的{@code WebApplicationInitializer}替换，但实际的
 * {@code dispatcher-config.xml} Spring配置文件仍然基于XML。{@code WebApplicationInitializer}非常适合与Spring的
 * {@link org.springframework.context.annotation.Configuration @Configuration}类一起使用。请参阅{@link
 * org.springframework.context.annotation.Configuration Configuration}的Javadoc以获取完整细节，但以下示例演示了如何重构为使用Spring的
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext AnnotationConfigWebApplicationContext}，
 * 以及如何使用用户定义的{@code @Configuration}类{@code AppConfig}和{@code DispatcherConfig}替代Spring XML文件。此示例还进一步
 * 演示了典型地配置'root'应用上下文以及注册{@code ContextLoaderListener}的方式：
 *
 * <pre class="code">
 * public class MyWebAppInitializer implements WebApplicationInitializer {
 *
 *    &#064;Override
 *    public void onStartup(ServletContext container) {
 *      // Create the 'root' Spring application context
 *      AnnotationConfigWebApplicationContext rootContext =
 *        new AnnotationConfigWebApplicationContext();
 *      rootContext.register(AppConfig.class);
 *
 *      // Manage the lifecycle of the root application context
 *      container.addListener(new ContextLoaderListener(rootContext));
 *
 *      // Create the dispatcher servlet's Spring application context
 *      AnnotationConfigWebApplicationContext dispatcherContext =
 *        new AnnotationConfigWebApplicationContext();
 *      dispatcherContext.register(DispatcherConfig.class);
 *
 *      // Register and map the dispatcher servlet
 *      ServletRegistration.Dynamic dispatcher =
 *        container.addServlet("dispatcher", new DispatcherServlet(dispatcherContext));
 *      dispatcher.setLoadOnStartup(1);
 *      dispatcher.addMapping("/");
 *    }
 *
 * }</pre>
 *
 * As an alternative to the above, you can also extend from {@link
 * org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer}.
 * <p>作为上述示例的替代，您还可以扩展自{@link org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer}。
 *
 * Remember that {@code WebApplicationInitializer} implementations are <em>detected
 * automatically</em> -- so you are free to package them within your application as you
 * see fit.
 * <p>您可以自由地将{@code WebApplicationInitializer}实现打包到您的应用程序中适合的位置，因为它们会被自动检测到。
 *
 * <h2>Ordering {@code WebApplicationInitializer} execution</h2>
 * {@code WebApplicationInitializer} implementations may optionally be annotated at the
 * class level with Spring's @{@link org.springframework.core.annotation.Order Order}
 * annotation or may implement Spring's {@link org.springframework.core.Ordered Ordered}
 * interface. If so, the initializers will be ordered prior to invocation. This provides
 * a mechanism for users to ensure the order in which servlet container initialization
 * occurs. Use of this feature is expected to be rare, as typical applications will likely
 * centralize all container initialization within a single {@code WebApplicationInitializer}.
 * <h2>{@code WebApplicationInitializer}执行的排序</h2>
 * {@code WebApplicationInitializer}实现类可以选择在类级别上使用Spring的{@link
 * org.springframework.core.annotation.Order @Order}注解或实现Spring的{@link org.springframework.core.Ordered Ordered}
 * 接口。如果这样做，初始化器将在调用前按顺序排列。这为用户确保Servlet容器初始化发生的顺序提供了一种机制。使用此功能的情况
 * 预期较少，因为典型应用程序可能会将所有容器初始化集中在一个{@code WebApplicationInitializer}中。
 *
 * <h2>Caveats</h2>
 * <h2>注意事项</h2>
 *
 * <h3>web.xml versioning</h3>
 * <h3>web.xml版本控制</h3>
 * <p>{@code WEB-INF/web.xml} and {@code WebApplicationInitializer} use are not mutually
 * exclusive; for example, web.xml can register one servlet, and a {@code
 * WebApplicationInitializer} can register another. An initializer can even
 * <em>modify</em> registrations performed in {@code web.xml} through methods such as
 * {@link ServletContext#getServletRegistration(String)}. <strong>However, if
 * {@code WEB-INF/web.xml} is present in the application, its {@code version} attribute
 * must be set to "3.0" or greater, otherwise {@code ServletContainerInitializer}
 * bootstrapping will be ignored by the servlet container.</strong>
 * p>如果存在{@code WEB-INF/web.xml}，并且其{@code version}属性被设置为"3.0"或更高，否则Servlet容器将忽略对
 * {@code ServletContainerInitializer}的引导。</strong>
 *
 * <h3>Mapping to '/' under Tomcat</h3>
 * <p>Apache Tomcat maps its internal {@code DefaultServlet} to "/", and on Tomcat versions
 * &lt;= 7.0.14, this servlet mapping <em>cannot be overridden programmatically</em>.
 * 7.0.15 fixes this issue. Overriding the "/" servlet mapping has also been tested
 * successfully under GlassFish 3.1.<p>
 * <h3>在Tomcat下映射到'/'</h3>
 * <p>Apache Tomcat将内部的{@code DefaultServlet}映射到"/"，而在Tomcat版本小于等于7.0.14时，
 * 无法通过程序化方式覆盖此Servlet映射。7.0.15修复了此问题。在GlassFish 3.1上也成功测试了覆盖'/'的Servlet映射。</p>
 *
 * @author Chris Beams
 * @since 3.1
 * @see SpringServletContainerInitializer
 * @see org.springframework.web.context.AbstractContextLoaderInitializer
 * @see org.springframework.web.servlet.support.AbstractDispatcherServletInitializer
 * @see org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer
 */
public interface WebApplicationInitializer {

	/**
	 * Configure the given {@link ServletContext} with any servlets, filters, listeners
	 * context-params and attributes necessary for initializing this web application. See
	 * examples {@linkplain WebApplicationInitializer above}.
	 * 在web应用程序初始化时，配置给定的{@link ServletContext}，包括但不限于servlet、filter、listener、
	 * 上下文参数和属性。请参考上面的{@linkplain WebApplicationInitializer 示例}。
	 *
	 * @param servletContext the {@code ServletContext} to initialize
	 * @throws ServletException if any call against the given {@code ServletContext}
	 * throws a {@code ServletException}
	 */
	void onStartup(ServletContext servletContext) throws ServletException;

}
