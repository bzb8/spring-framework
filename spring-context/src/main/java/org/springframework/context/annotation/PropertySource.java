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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.io.support.PropertySourceFactory;

/**
 * Annotation providing a convenient and declarative mechanism for adding a
 * {@link org.springframework.core.env.PropertySource PropertySource} to Spring's
 * {@link org.springframework.core.env.Environment Environment}. To be used in
 * conjunction with @{@link Configuration} classes.
 *
 * 注解提供了一种方便的声明性机制，
 * 用于将PropertySource 添加到 Spring 的 Environment 环境。与Configuration类结合使用。
 *
 * <h3>Example usage</h3>
 *
 * <p>Given a file {@code app.properties} containing the key/value pair
 * {@code testbean.name=myTestBean}, the following {@code @Configuration} class
 * uses {@code @PropertySource} to contribute {@code app.properties} to the
 * {@code Environment}'s set of {@code PropertySources}.
 *
 * 给定一个包含键值对 {@code testbean.name=myTestBean} 的文件 {@code app.properties}，下面的 {@code @Configuration} 类使用
 * {@code @PropertySource} 将 {@code app.properties} 贡献给 {@code Environment} 的 {@code PropertySources} 集。
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/app.properties")
 * public class AppConfig {
 *
 *     &#064;Autowired
 *     Environment env;
 *
 *     &#064;Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * <p>Notice that the {@code Environment} object is
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired} into the
 * configuration class and then used when populating the {@code TestBean} object. Given
 * the configuration above, a call to {@code testBean.getName()} will return "myTestBean".
 *
 * 请注意，{@code Environment}对象是{@link org.springframework.beans.factory.annotation.Autowired @Autowired}到配置类中，
 * 然后在填充{@code TestBean}对象时使用。根据上面的配置，调用 {@code testBean.getName（）} 将返回 “myTestBean”。
 *
 * <h3>Resolving <code>${...}</code> placeholders in {@code <bean>} and {@code @Value} annotations</h3>
 *
 * 解析 ${...}  <bean>和 {@code @Value} 注解中的占位符
 *
 *
 * <p>In order to resolve ${...} placeholders in {@code <bean>} definitions or {@code @Value}
 * annotations using properties from a {@code PropertySource}, you must ensure that an
 * appropriate <em>embedded value resolver</em> is registered in the {@code BeanFactory}
 * used by the {@code ApplicationContext}. This happens automatically when using
 * {@code <context:property-placeholder>} in XML. When using {@code @Configuration} classes
 * this can be achieved by explicitly registering a {@code PropertySourcesPlaceholderConfigurer}
 * via a {@code static} {@code @Bean} method. Note, however, that explicit registration
 * of a {@code PropertySourcesPlaceholderConfigurer} via a {@code static} {@code @Bean}
 * method is typically only required if you need to customize configuration such as the
 * placeholder syntax, etc. See the "Working with externalized values" section of
 * {@link Configuration @Configuration}'s javadocs and "a note on
 * BeanFactoryPostProcessor-returning {@code @Bean} methods" of {@link Bean @Bean}'s
 * javadocs for details and examples.
 *
 * 为了使用 {@code PropertySource} 中的属性 解析 {@code <bean>} 定义或 {@code @Value} 注解中的 ${...} 占位符，
 * 必须确保在 {@code ApplicationContext} 使用的 {@code BeanFactory} 中注册了适当的嵌入值解析器。
 * 在 XML 中使用 {@code <context:property-placeholder>} 时，这会自动发生。使用 {@code @Configuration} 类时，
 * 可以通过 {@code static} {@code @Bean} 方法显式注册 {@code PropertySourcesPlaceholderConfigurer} 来实现此目的。但请注意，
 * 通常只有在需要自定义配置（如占位符语法等）时，才需要通过 {@code static} {@code @Bean} 方法显式注册 { PropertySourcesPlaceholderConfigurer}。
 * 有关详细信息和示例，请参阅 {@link Configuration @Configuration} 的 javadocs 的“使用外部化值”部分和 {@link Bean @Bean} 的 javadocs
 * 的“关于 BeanFactoryPostProcessor-returning {@code @Bean} 方法的注释”。
 *
 * <h3>Resolving ${...} placeholders within {@code @PropertySource} resource locations</h3>
 *
 * 解析 {@code @PropertySource} 个资源位置中的 ${...} 占位符
 *
 * <p>Any ${...} placeholders present in a {@code @PropertySource} {@linkplain #value()
 * resource location} will be resolved against the set of property sources already
 * registered against the environment. For example:
 *
 * {@code @PropertySource} {@linkplain #value() resource location} 中存在的任何 ${...} 占位符都将针对已针对环境注册的属性源集进行解析。例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/${my.placeholder:default/path}/app.properties")
 * public class AppConfig {
 *
 *     &#064;Autowired
 *     Environment env;
 *
 *     &#064;Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * <p>Assuming that "my.placeholder" is present in one of the property sources already
 * registered &mdash; for example, system properties or environment variables &mdash;
 * the placeholder will be resolved to the corresponding value. If not, then "default/path"
 * will be used as a default. Expressing a default value (delimited by colon ":") is
 * optional. If no default is specified and a property cannot be resolved, an {@code
 * IllegalArgumentException} will be thrown.
 *
 * 假设“my.placeholder”存在于已注册的属性源之一（例如，系统属性或环境变量）中，则占位符将解析为相应的值。
 * 如果没有，则“default/path”将用作默认值。表示默认值（用冒号 “:” 分隔）是可选的。如果未指定默认值且无法解析属性，则将引发 {@code IllegalArgumentException}。
 *
 * <h3>A note on property overriding with {@code @PropertySource}</h3>
 *
 * 关于用 {@code @PropertySource} 重写属性的说明
 *
 * <p>In cases where a given property key exists in more than one {@code .properties}
 * file, the last {@code @PropertySource} annotation processed will 'win' and override
 * any previous key with the same name.
 *
 * 如果给定的属性键存在于多个 {@code .properties} 文件中，则处理的最后一个 {@code @PropertySource} 注解将“获胜”并覆盖任何具有相同名称的先前键。
 *
 * <p>For example, given two properties files {@code a.properties} and
 * {@code b.properties}, consider the following two configuration classes
 * that reference them with {@code @PropertySource} annotations:
 *
 * 例如，给定两个属性文件 {@code a.properties} 和 {@code b.properties}，请考虑以下两个使用 {@code @PropertySource} 注解引用它们的配置类：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/a.properties")
 * public class ConfigA { }
 *
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/b.properties")
 * public class ConfigB { }
 * </pre>
 *
 * <p>The override ordering depends on the order in which these classes are registered
 * with the application context.
 *
 * 重写顺序取决于这些类在应用程序上下文中注册的顺序。
 *
 * <pre class="code">
 * AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 * ctx.register(ConfigA.class);
 * ctx.register(ConfigB.class);
 * ctx.refresh();
 * </pre>
 *
 * <p>In the scenario above, the properties in {@code b.properties} will override any
 * duplicates that exist in {@code a.properties}, because {@code ConfigB} was registered
 * last.
 *
 * 在上面的场景中，{@code b.properties} 中的属性将覆盖 {@code a.properties} 中存在的任何重复项，因为 {@code ConfigB} 是最后注册的。
 *
 * <p>In certain situations, it may not be possible or practical to tightly control
 * property source ordering when using {@code @PropertySource} annotations. For example,
 * if the {@code @Configuration} classes above were registered via component-scanning,
 * the ordering is difficult to predict. In such cases &mdash; and if overriding is important
 * &mdash; it is recommended that the user fall back to using the programmatic
 * {@code PropertySource} API. See {@link org.springframework.core.env.ConfigurableEnvironment
 * ConfigurableEnvironment} and {@link org.springframework.core.env.MutablePropertySources
 * MutablePropertySources} javadocs for details.
 *
 * 在某些情况下，在使用 {@code @PropertySource} 注解时，严格控制属性源排序可能是不可能的，也不切实际。
 * 例如，如果上面的 {@code @Configuration} 类是通过组件扫描注册的，则很难预测排序。在这种情况下，如果重写很重要，建议用户回退到使用编程 {@code PropertySource} API。
 * 有关详细信息，请参阅 {@link org.springframework.core.env.ConfigurableEnvironment ConfigurableEnvironment} 和
 * {@link org.springframework.core.env.MutablePropertySources} javadocs。
 *
 * <p><b>NOTE: This annotation is repeatable according to Java 8 conventions.</b>
 * However, all such {@code @PropertySource} annotations need to be declared at the same
 * level: either directly on the configuration class or as meta-annotations on the
 * same custom annotation. Mixing direct annotations and meta-annotations is not
 * recommended since direct annotations will effectively override meta-annotations.
 *
 * 注意：根据 Java 8 约定，此注解是可重复的。
 * 但是，所有此类 {@code @PropertySource} 注解都需要在同一级别声明：要么直接在配置类上声明，要么作为同一自定义注解上的元注解。
 * 不建议混合使用直接注解和元注解，因为直接注解将有效地覆盖元注解。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1
 * @see PropertySources
 * @see Configuration
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.core.env.ConfigurableEnvironment#getPropertySources()
 * @see org.springframework.core.env.MutablePropertySources
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(PropertySources.class)
public @interface PropertySource {

	/**
	 * Indicate the name of this property source. If omitted, the {@link #factory}
	 * will generate a name based on the underlying resource (in the case of
	 * {@link org.springframework.core.io.support.DefaultPropertySourceFactory}:
	 * derived from the resource description through a corresponding name-less
	 * {@link org.springframework.core.io.support.ResourcePropertySource} constructor).
	 * @see org.springframework.core.env.PropertySource#getName()
	 * @see org.springframework.core.io.Resource#getDescription()
	 * --
	 * 指示此属性源的名称。如果省略，{@link #factory}将基于底层资源生成一个名称
	 * （在{@link org.springframework.core.io.support.DefaultPropertySourceFactory}的情况下：
	 * 通过相应的无名称{@link org.springframework.core.io.support.ResourcePropertySource}构造函数从资源描述派生）。
	 */
	String name() default "";

	/**
	 * Indicate the resource location(s) of the properties file to be loaded.
	 * <p>Both traditional and XML-based properties file formats are supported
	 * &mdash; for example, {@code "classpath:/com/myco/app.properties"}
	 * or {@code "file:/path/to/file.xml"}.
	 * <p>Resource location wildcards (e.g. *&#42;/*.properties) are not permitted;
	 * each location must evaluate to exactly one {@code .properties} or {@code .xml}
	 * resource.
	 * <p>${...} placeholders will be resolved against any/all property sources already
	 * registered with the {@code Environment}. See {@linkplain PropertySource above}
	 * for examples.
	 * <p>Each location will be added to the enclosing {@code Environment} as its own
	 * property source, and in the order declared.
	 *
	 * 指示要加载的属性文件的资源位置。
	 * 支持传统属性文件格式和基于 XML 的属性文件格式，例如  {@code "classpath:/com/myco/app.properties"} or {@code "file:/path/to/file.xml"}。
	 * 不允许使用资源位置通配符（例如 &42;.properties）;每个位置的计算结果必须恰好为一个 {@code .properties} 或 {@code .xml} 资源。
	 * ${...} 占位符将针对已注册到 {@code Environment} 的所有属性源进行解
	 * 析。有关示例，请参阅 {@linkplain PropertySource above}。
	 * 每个位置都将作为其自己的属性源添加到封闭的 {@code Environment} 中，并按声明的顺序添加。
	 */
	String[] value();

	/**
	 * Indicate if a failure to find a {@link #value property resource} should be
	 * ignored.
	 * <p>{@code true} is appropriate if the properties file is completely optional.
	 * <p>Default is {@code false}.
	 * --
	 * 指示是否应忽略查找 {@link #value property resource} 的失败。
	 * 如果属性文件是完全可选的，则 {@code true} 是合适的。
	 * 默认值为 {@code false}。
	 *
	 * @since 4.0
	 */
	boolean ignoreResourceNotFound() default false;

	/**
	 * A specific character encoding for the given resources, e.g. "UTF-8".
	 * @since 4.3
	 */
	String encoding() default "";

	/**
	 * Specify a custom {@link PropertySourceFactory}, if any.
	 * <p>By default, a default factory for standard resource files will be used.
	 * --
	 * 指定自定义 {@link PropertySourceFactory}（如果有）。
	 * 默认情况下，将使用标准资源文件的默认工厂。
	 *
	 * @since 4.3
	 * @see org.springframework.core.io.support.DefaultPropertySourceFactory
	 * @see org.springframework.core.io.support.ResourcePropertySource
	 */
	Class<? extends PropertySourceFactory> factory() default PropertySourceFactory.class;

}
