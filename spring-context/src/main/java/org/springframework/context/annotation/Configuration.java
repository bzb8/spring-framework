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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * Indicates that a class declares one or more {@link Bean @Bean} methods and
 * may be processed by the Spring container to generate bean definitions and
 * service requests for those beans at runtime, for example:
 *
 * 指示一个类声明一个或多个 {@link Bean @Bean} 方法，并且可以由 Spring 容器处理，以在运行时为这些 Bean 生成 Bean 定义和服务请求，例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // instantiate, configure and return bean ...
 *     }
 * }</pre>
 *
 * <h2>Bootstrapping {@code @Configuration} classes</h2>
 *
 * 引导 {@code @Configuration} 类
 *
 * <h3>Via {@code AnnotationConfigApplicationContext}</h3>
 *
 * <p>{@code @Configuration} classes are typically bootstrapped using either
 * {@link AnnotationConfigApplicationContext} or its web-capable variant,
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext
 * AnnotationConfigWebApplicationContext}. A simple example with the former follows:
 *
 * {@code @Configuration} 类通常使用 {@link AnnotationConfigApplicationContext} 或其支持 Web 的变体
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext AnnotationConfigWebApplicationContext} 进行引导。以下是前者的简单示例：
 *
 * <pre class="code">
 * AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 * ctx.register(AppConfig.class);
 * ctx.refresh();
 * MyBean myBean = ctx.getBean(MyBean.class);
 * // use myBean ...
 * </pre>
 *
 * <p>See the {@link AnnotationConfigApplicationContext} javadocs for further details, and see
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext
 * AnnotationConfigWebApplicationContext} for web configuration instructions in a
 * {@code Servlet} container.
 *
 * 有关详细信息，请参阅 {@link AnnotationConfigApplicationContext} javadocs，
 * 并参阅 {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext AnnotationConfigWebApplicationContext} 以获取 {@code Servlet} 容器中的 Web 配置说明。
 *
 * <h3>Via Spring {@code <beans>} XML</h3>
 *
 * <p>As an alternative to registering {@code @Configuration} classes directly against an
 * {@code AnnotationConfigApplicationContext}, {@code @Configuration} classes may be
 * declared as normal {@code <bean>} definitions within Spring XML files:
 *
 * 作为直接针对 {@code AnnotationConfigApplicationContext} 注册 {@code @Configuration} 类的替代方法，{@code @Configuration} 类可以在 Spring XML 文件中声明为正常的 {@code <bean>} 定义：
 *
 * <pre class="code">
 * &lt;beans&gt;
 *    &lt;context:annotation-config/&gt;
 *    &lt;bean class="com.acme.AppConfig"/&gt;
 * &lt;/beans&gt;
 * </pre>
 *
 * <p>In the example above, {@code <context:annotation-config/>} is required in order to
 * enable {@link ConfigurationClassPostProcessor} and other annotation-related
 * post processors that facilitate handling {@code @Configuration} classes.
 *
 * 在上面的示例中，需要 {@code <context:annotation-config/>} 才能启用 {@link ConfigurationClassPostProcessor} 和其他与注解相关的后处理器，以便于处理 {@code @Configuration} 类。
 *
 * <h3>Via component scanning</h3>
 *
 * <p>{@code @Configuration} is meta-annotated with {@link Component @Component}, therefore
 * {@code @Configuration} classes are candidates for component scanning (typically using
 * Spring XML's {@code <context:component-scan/>} element) and therefore may also take
 * advantage of {@link Autowired @Autowired}/{@link javax.inject.Inject @Inject}
 * like any regular {@code @Component}. In particular, if a single constructor is present
 * autowiring semantics will be applied transparently for that constructor:
 *
 * {@code @Configuration} 使用 {@link Component @Component} 进行元注解，因此 {@code @Configuration} 类是组件扫描的候选类（通常使用 Spring XML 的 {@code <context：component-scan>} 元素），
 * 因此也可以像任何常规 {@code @Component} 一样利用 {@link Autowired @Autowired}{@link javax.inject.Inject @Inject}。具体而言，如果存在单个构造函数，则会自动注入语义透明地应用于该构造函数：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     private final SomeBean someBean;
 *
 *     public AppConfig(SomeBean someBean) {
 *         this.someBean = someBean;
 *     }
 *
 *     // &#064;Bean definition using "SomeBean"
 *
 * }</pre>
 *
 * <p>{@code @Configuration} classes may not only be bootstrapped using
 * component scanning, but may also themselves <em>configure</em> component scanning using
 * the {@link ComponentScan @ComponentScan} annotation:
 *
 * {@code @Configuration} 类不仅可以使用组件扫描进行引导，还可以使用 {@link ComponentScan @ComponentScan} 注解来配置组件扫描：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan("com.acme.app.services")
 * public class AppConfig {
 *     // various &#064;Bean definitions ...
 * }</pre>
 *
 * <p>See the {@link ComponentScan @ComponentScan} javadocs for details.
 *
 * 有关详细信息，请参阅 {@link ComponentScan @ComponentScan} javadocs。
 *
 * <h2>Working with externalized values</h2>
 *
 * 使用外部化值
 *
 * <h3>Using the {@code Environment} API</h3>
 *
 * <p>Externalized values may be looked up by injecting the Spring
 * {@link org.springframework.core.env.Environment} into a {@code @Configuration}
 * class &mdash; for example, using the {@code @Autowired} annotation:
 *
 * 可以通过将 Spring {@link org.springframework.core.env.Environment} 注入 {@code @Configuration} 类来查找外部化值——例如，使用 {@code @Autowired} 注解：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Autowired Environment env;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         MyBean myBean = new MyBean();
 *         myBean.setName(env.getProperty("bean.name"));
 *         return myBean;
 *     }
 * }</pre>
 *
 * <p>Properties resolved through the {@code Environment} reside in one or more "property
 * source" objects, and {@code @Configuration} classes may contribute property sources to
 * the {@code Environment} object using the {@link PropertySource @PropertySource}
 * annotation:
 *
 * 通过 {@code Environment} 解析的属性驻留在一个或多个“属性源”对象中，并且 {@code @Configuration} 类可以使用
 * {@link PropertySource @PropertySource}注解将属性源提供给 {@code Environment} 对象：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/acme/app.properties")
 * public class AppConfig {
 *
 *     &#064;Inject Environment env;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(env.getProperty("bean.name"));
 *     }
 * }</pre>
 *
 * <p>See the {@link org.springframework.core.env.Environment Environment}
 * and {@link PropertySource @PropertySource} javadocs for further details.
 *
 * <h3>Using the {@code @Value} annotation</h3>
 *
 * <p>Externalized values may be injected into {@code @Configuration} classes using
 * the {@link Value @Value} annotation:
 *
 * 可以使用 {@link Value @Value} 注释将外部化值注入到 {@code @Configuration} 类中：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/acme/app.properties")
 * public class AppConfig {
 *
 *     &#064;Value("${bean.name}") String beanName;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(beanName);
 *     }
 * }</pre>
 *
 * <p>This approach is often used in conjunction with Spring's
 * {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * PropertySourcesPlaceholderConfigurer} that can be enabled <em>automatically</em>
 * in XML configuration via {@code <context:property-placeholder/>} or <em>explicitly</em>
 * in a {@code @Configuration} class via a dedicated {@code static} {@code @Bean} method
 * (see "a note on BeanFactoryPostProcessor-returning {@code @Bean} methods" of
 * {@link Bean @Bean}'s javadocs for details). Note, however, that explicit registration
 * of a {@code PropertySourcesPlaceholderConfigurer} via a {@code static} {@code @Bean}
 * method is typically only required if you need to customize configuration such as the
 * placeholder syntax, etc. Specifically, if no bean post-processor (such as a
 * {@code PropertySourcesPlaceholderConfigurer}) has registered an <em>embedded value
 * resolver</em> for the {@code ApplicationContext}, Spring will register a default
 * <em>embedded value resolver</em> which resolves placeholders against property sources
 * registered in the {@code Environment}. See the section below on composing
 * {@code @Configuration} classes with Spring XML using {@code @ImportResource}; see
 * the {@link Value @Value} javadocs; and see the {@link Bean @Bean} javadocs for details
 * on working with {@code BeanFactoryPostProcessor} types such as
 * {@code PropertySourcesPlaceholderConfigurer}.
 *
 * 这种方法通常与 Spring 的 {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer PropertySourcesPlaceholderConfigurer} 结合使用，
 * 该配置可以通过 {@code <context:property-placeholder/>} 在 XML 配置中自动启用，也可以通过专用的 {@code static} {@code @Bean} 方法
 * 在 {@code @Configuration} 类中显式启用（请参阅 {@link Bean @Bean} 的 javadocs 的“关于 BeanFactoryPostProcessor-returning {@code @Bean} 方法的注释”，以获取详细信息）。
 * 但请注意，通常只有在需要自定义配置（如占位符语法等）时，才需要通过 {@code static} {@code @Bean} 方法显式注册 { PropertySourcesPlaceholderConfigurer}。
 * 具体来说，如果没有 Bean 后处理器（例如 {@code PropertySourcesPlaceholderConfigurer}）为 {@code ApplicationContext} 注册嵌入式值解析器，Spring 将注册一个默认的嵌入式值解析器，
 * 该解析器针对 {@code Environment} 中注册的属性源解析占位符。请参阅下面有关使用 {@code @ImportResource} 使用 Spring XML 编写 {@code @Configuration} 类的部分;
 * 请参阅 {@link Value @Value} javadocs;并参阅 {@link Bean @Bean} javadocs，了解有关使用 {@code BeanFactoryPostProcessor} 类型（如 {@code PropertySourcesPlaceholderConfigurer}）
 * 的详细信息。
 *
 * <h2>Composing {@code @Configuration} classes</h2>
 *
 * <h3>With the {@code @Import} annotation</h3>
 *
 * <p>{@code @Configuration} classes may be composed using the {@link Import @Import} annotation,
 * similar to the way that {@code <import>} works in Spring XML. Because
 * {@code @Configuration} objects are managed as Spring beans within the container,
 * imported configurations may be injected &mdash; for example, via constructor injection:
 *
 * {@code @Configuration} 类可以使用 {@link Import @Import} 注解组成，类似于  {@code <import>}  在 Spring XML 中的工作方式。
 * 由于 {@code @Configuration} 对象在容器中作为 Spring Bean 进行管理，因此可以注入导入的配置，例如，通过构造函数注入：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class DatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return DataSource
 *     }
 * }
 *
 * &#064;Configuration
 * &#064;Import(DatabaseConfig.class)
 * public class AppConfig {
 *
 *     private final DatabaseConfig dataConfig;
 *
 *     public AppConfig(DatabaseConfig dataConfig) {
 *         this.dataConfig = dataConfig;
 *     }
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // reference the dataSource() bean method
 *         return new MyBean(dataConfig.dataSource());
 *     }
 * }</pre>
 *
 * <p>Now both {@code AppConfig} and the imported {@code DatabaseConfig} can be bootstrapped
 * by registering only {@code AppConfig} against the Spring context:
 *
 * 现在，{@code AppConfig} 和导入的 {@code DatabaseConfig} 都可以通过在 Spring 上下文中仅注册 {@code AppConfig} 来引导：
 *
 * <pre class="code">
 * new AnnotationConfigApplicationContext(AppConfig.class);</pre>
 *
 * <h3>With the {@code @Profile} annotation</h3>
 *
 * <p>{@code @Configuration} classes may be marked with the {@link Profile @Profile} annotation to
 * indicate they should be processed only if a given profile or profiles are <em>active</em>:
 *
 * {@code @Configuration} 类可以用 {@link Profile @Profile} 注释进行标记，以指示仅当给定的一个或多个配置文件处于活动状态时才应处理它们：
 *
 * <pre class="code">
 * &#064;Profile("development")
 * &#064;Configuration
 * public class EmbeddedDatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return embedded DataSource
 *     }
 * }
 *
 * &#064;Profile("production")
 * &#064;Configuration
 * public class ProductionDatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return production DataSource
 *     }
 * }</pre>
 *
 * <p>Alternatively, you may also declare profile conditions at the {@code @Bean} method level
 * &mdash; for example, for alternative bean variants within the same configuration class:
 *
 * 或者，您也可以在 {@code @Bean} 方法级别声明配置文件条件，例如，对于同一配置类中的替代 Bean 变体：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class ProfileDatabaseConfig {
 *
 *     &#064;Bean("dataSource")
 *     &#064;Profile("development")
 *     public DataSource embeddedDatabase() { ... }
 *
 *     &#064;Bean("dataSource")
 *     &#064;Profile("production")
 *     public DataSource productionDatabase() { ... }
 * }</pre>
 *
 * <p>See the {@link Profile @Profile} and {@link org.springframework.core.env.Environment}
 * javadocs for further details.
 *
 * 有关详细信息，请参阅 {@link Profile @Profile} 和 {@link org.springframework.core.env.Environment} javadocs。
 *
 * <h3>With Spring XML using the {@code @ImportResource} annotation</h3>
 *
 * 使用 Spring XML 的 {@code @ImportResource} 注解
 *
 * <p>As mentioned above, {@code @Configuration} classes may be declared as regular Spring
 * {@code <bean>} definitions within Spring XML files. It is also possible to
 * import Spring XML configuration files into {@code @Configuration} classes using
 * the {@link ImportResource @ImportResource} annotation. Bean definitions imported from
 * XML can be injected &mdash; for example, using the {@code @Inject} annotation:
 *
 * 如上所述，{@code @Configuration} 类可以在 Spring XML 文件中声明为常规{@code <bean>} 的 Spring {@code <bean>} 定义。
 * 也可以使用 {@link ImportResource @ImportResource} 注解将 Spring XML 配置文件导入 {@code @Configuration} 类。可以注入从 XML 导入的 Bean 定义，例如，使用 {@code @Inject} 注释：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ImportResource("classpath:/com/acme/database-config.xml")
 * public class AppConfig {
 *
 *     &#064;Inject DataSource dataSource; // from XML
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // inject the XML-defined dataSource bean
 *         return new MyBean(this.dataSource);
 *     }
 * }</pre>
 *
 * <h3>With nested {@code @Configuration} classes</h3>
 *
 * 使用嵌套的 {@code @Configuration} 类
 *
 * <p>{@code @Configuration} classes may be nested within one another as follows:
 *
 * {@code @Configuration} 类可以相互嵌套，如下所示：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Inject DataSource dataSource;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(dataSource);
 *     }
 *
 *     &#064;Configuration
 *     static class DatabaseConfig {
 *         &#064;Bean
 *         DataSource dataSource() {
 *             return new EmbeddedDatabaseBuilder().build();
 *         }
 *     }
 * }</pre>
 *
 * <p>When bootstrapping such an arrangement, only {@code AppConfig} need be registered
 * against the application context. By virtue of being a nested {@code @Configuration}
 * class, {@code DatabaseConfig} <em>will be registered automatically</em>. This avoids
 * the need to use an {@code @Import} annotation when the relationship between
 * {@code AppConfig} and {@code DatabaseConfig} is already implicitly clear.
 *
 * 引导此类安排时，只需针对应用程序上下文注册 {@code AppConfig}。由于是嵌套的 {@code @Configuration} 类，{@code DatabaseConfig} 将自动注册。
 * 这样就避免了在 {@code AppConfig} 和 {@code DatabaseConfig} 之间的关系已经隐式明确时使用 {@code @Import} 注解。
 *
 * <p>Note also that nested {@code @Configuration} classes can be used to good effect
 * with the {@code @Profile} annotation to provide two options of the same bean to the
 * enclosing {@code @Configuration} class.
 *
 * 另请注意，嵌套的 {@code @Configuration} 类可以很好地与 {@code @Profile} 注释一起使用，以便为封闭的 {@code @Configuration} 类提供同一 Bean 的两个选项。
 *
 * <h2>Configuring lazy initialization</h2>
 *
 * 配置延迟初始化
 *
 * <p>By default, {@code @Bean} methods will be <em>eagerly instantiated</em> at container
 * bootstrap time.  To avoid this, {@code @Configuration} may be used in conjunction with
 * the {@link Lazy @Lazy} annotation to indicate that all {@code @Bean} methods declared
 * within the class are by default lazily initialized. Note that {@code @Lazy} may be used
 * on individual {@code @Bean} methods as well.
 *
 * 默认情况下，{@code @Bean} 方法将在容器引导时快速实例化。为了避免这种情况，可以将 {@code @Configuration} 与 {@link Lazy @Lazy} 注释结合使用，
 * 以指示默认情况下类中声明的所有 {@code @Bean} 方法都是延迟初始化的。请注意，{@code @Lazy} 也可用于单个 {@code @Bean} 方法。
 *
 * <h2>Testing support for {@code @Configuration} classes</h2>
 *
 * <p>The Spring <em>TestContext framework</em> available in the {@code spring-test} module
 * provides the {@code @ContextConfiguration} annotation which can accept an array of
 * <em>component class</em> references &mdash; typically {@code @Configuration} or
 * {@code @Component} classes.
 *
 * {@code spring-test} 模块中提供的 Spring TestContext 框架提供了 {@code @ContextConfiguration} 注解，它可以接受组件类引用数组——
 * 通常是 {@code @Configuration} 或 {@code @Component} 类。
 *
 * <pre class="code">
 * &#064;RunWith(SpringRunner.class)
 * &#064;ContextConfiguration(classes = {AppConfig.class, DatabaseConfig.class})
 * public class MyTests {
 *
 *     &#064;Autowired MyBean myBean;
 *
 *     &#064;Autowired DataSource dataSource;
 *
 *     &#064;Test
 *     public void test() {
 *         // assertions against myBean ...
 *     }
 * }</pre>
 *
 * <p>See the
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#testcontext-framework">TestContext framework</a>
 * reference documentation for details.
 *
 * <h2>Enabling built-in Spring features using {@code @Enable} annotations</h2>
 *
 * 使用 {@code @Enable} 注解启用内置 Spring 功能
 *
 * <p>Spring features such as asynchronous method execution, scheduled task execution,
 * annotation driven transaction management, and even Spring MVC can be enabled and
 * configured from {@code @Configuration} classes using their respective "{@code @Enable}"
 * annotations. See
 * {@link org.springframework.scheduling.annotation.EnableAsync @EnableAsync},
 * {@link org.springframework.scheduling.annotation.EnableScheduling @EnableScheduling},
 * {@link org.springframework.transaction.annotation.EnableTransactionManagement @EnableTransactionManagement},
 * {@link org.springframework.context.annotation.EnableAspectJAutoProxy @EnableAspectJAutoProxy},
 * and {@link org.springframework.web.servlet.config.annotation.EnableWebMvc @EnableWebMvc}
 * for details.
 *
 * Spring 功能，如异步方法执行、计划任务执行、注解驱动的事务管理，甚至 Spring MVC，都可以使用它们各自的“{@code @Enable}”注解从 {@code @Configuration} 类启用和配置。
 * 请参阅 {@link org.springframework.scheduling.annotation.EnableAsync @EnableAsync}、
 * {@link org.springframework.scheduling.annotation.EnableScheduling @EnableScheduling}、
 * {@link org.springframework.transaction.annotation.EnableTransactionManagement @EnableTransactionManagement}、
 * {@link org.springframework.context.annotation.EnableAspectJAutoProxy @EnableAspectJAutoProxy} 和
 * {@link org.springframework.web.servlet.config.annotation.EnableWebMvc @EnableWebMvc}详。
 *
 * <h2>Constraints when authoring {@code @Configuration} classes</h2>
 *
 * 创作 {@code @Configuration} 类时的约束
 *
 * <ul>
 * <li>Configuration classes must be provided as classes (i.e. not as instances returned
 * from factory methods), allowing for runtime enhancements through a generated subclass.
 * <li>Configuration classes must be non-final (allowing for subclasses at runtime),
 * unless the {@link #proxyBeanMethods() proxyBeanMethods} flag is set to {@code false}
 * in which case no runtime-generated subclass is necessary.
 * <li>Configuration classes must be non-local (i.e. may not be declared within a method).
 * <li>Any nested configuration classes must be declared as {@code static}.
 * <li>{@code @Bean} methods may not in turn create further configuration classes
 * (any such instances will be treated as regular beans, with their configuration
 * annotations remaining undetected).
 * </ul>
 *
 * 配置类必须作为类提供（即不能作为从工厂方法返回的实例），从而允许通过生成的子类进行运行时增强。
 * 配置类必须是非最终的（允许在运行时使用子类），除非 {@link #proxyBeanMethods() proxyBeanMethods} 标志设置为 {@code false}，在这种情况下，不需要运行时生成的子类。
 * 配置类必须是非本地的（即不能在方法中声明）。
 * 任何嵌套的配置类都必须声明为 {@code static}。
 * {@code @Bean} 方法可能不会反过来创建进一步的配置类（任何此类实例都将被视为常规 bean，其配置注释仍未被检测到）。
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Bean
 * @see Profile
 * @see Import
 * @see ImportResource
 * @see ComponentScan
 * @see Lazy
 * @see PropertySource
 * @see AnnotationConfigApplicationContext
 * @see ConfigurationClassPostProcessor
 * @see org.springframework.core.env.Environment
 * @see org.springframework.test.context.ContextConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {

	/**
	 * Explicitly specify the name of the Spring bean definition associated with the
	 * {@code @Configuration} class. If left unspecified (the common case), a bean
	 * name will be automatically generated.
	 * <p>The custom name applies only if the {@code @Configuration} class is picked
	 * up via component scanning or supplied directly to an
	 * {@link AnnotationConfigApplicationContext}. If the {@code @Configuration} class
	 * is registered as a traditional XML bean definition, the name/id of the bean
	 * element will take precedence.
	 *
	 * 显式指定与 {@code @Configuration} 类关联的 Spring bean 定义的名称。如果未指定（常见情况），将自动生成一个 Bean 名称。
	 * 仅当 {@code @Configuration} 类通过组件扫描选取或直接提供给 {@link AnnotationConfigApplicationContext} 时，自定义名称才适用。
	 * 如果将 {@code @Configuration} 类注册为传统的 XML Bean 定义，则 Bean 元素的 name/id 将优先。
	 *
	 * @return the explicit component name, if any (or empty String otherwise)
	 * @see AnnotationBeanNameGenerator
	 */
	@AliasFor(annotation = Component.class)
	String value() default "";

	/**
	 * Specify whether {@code @Bean} methods should get proxied in order to enforce
	 * bean lifecycle behavior, e.g. to return shared singleton bean instances even
	 * in case of direct {@code @Bean} method calls in user code. This feature
	 * requires method interception, implemented through a runtime-generated CGLIB
	 * subclass which comes with limitations such as the configuration class and
	 * its methods not being allowed to declare {@code final}.
	 * <p>The default is {@code true}, allowing for 'inter-bean references' via direct
	 * method calls within the configuration class as well as for external calls to
	 * this configuration's {@code @Bean} methods, e.g. from another configuration class.
	 * If this is not needed since each of this particular configuration's {@code @Bean}
	 * methods is self-contained and designed as a plain factory method for container use,
	 * switch this flag to {@code false} in order to avoid CGLIB subclass processing.
	 * <p>Turning off bean method interception effectively processes {@code @Bean}
	 * methods individually like when declared on non-{@code @Configuration} classes,
	 * a.k.a. "@Bean Lite Mode" (see {@link Bean @Bean's javadoc}). It is therefore
	 * behaviorally equivalent to removing the {@code @Configuration} stereotype.
	 *
	 * 指定是否应该对 {@code @Bean} 方法进行代理，以强制执行 Bean 生命周期行为，例如，即使在用户代码中直接调用 {@code @Bean} 方法的情况下，也要返回共享的单例 Bean 实例。
	 * 此功能需要方法拦截，通过运行时生成的 CGLIB 子类实现，该子类具有限制，例如不允许配置类及其方法声明 {@code final}。
	 * 默认值为 {@code true}，允许通过配置类中的直接方法调用进行“bean 间引用”，以及对此配置的 {@code @Bean} 方法进行外部调用，例如从另一个配置类调用。
	 * 如果不需要这样做，因为此特定配置的每个 {@code @Bean} 方法都是独立的，并且设计为供容器使用的普通工厂方法，请将此标志切换为 {@code false} 以避免 CGLIB 子类处理。
	 * 关闭 Bean 方法拦截可以有效地单独处理 {@code @Bean} 个方法，就像在非 {@code @Configuration} 类上声明时一样，也称为“@Bean Lite 模式”（参见 {@link Bean @Bean的 javadoc}）。
	 * 因此，它在行为上等同于删除 {@code @Configuration} 构造型。
	 *
	 * @since 5.2
	 */
	boolean proxyBeanMethods() default true;

}
