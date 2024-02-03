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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.annotation.AliasFor;

/**
 * Indicates that a method produces a bean to be managed by the Spring container.
 *
 * 指示方法生成要由 Spring 容器管理的 bean。
 *
 * <h3>Overview</h3>
 *
 * 概述
 *
 * <p>The names and semantics of the attributes to this annotation are intentionally
 * similar to those of the {@code <bean/>} element in the Spring XML schema. For
 * example:
 *
 * 此注解的属性的名称和语义有意类似于 Spring XML 模式中 {@code <bean/>} 元素的名称和语义。例如：
 *
 * <pre class="code">
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Bean Names</h3>
 *
 * Bean 名称
 *
 * <p>While a {@link #name} attribute is available, the default strategy for
 * determining the name of a bean is to use the name of the {@code @Bean} method.
 * This is convenient and intuitive, but if explicit naming is desired, the
 * {@code name} attribute (or its alias {@code value}) may be used. Also note
 * that {@code name} accepts an array of Strings, allowing for multiple names
 * (i.e. a primary bean name plus one or more aliases) for a single bean.
 *
 * 虽然 {@link #name} 属性可用，但确定 Bean 名称的默认策略是使用 {@code @Bean} 方法的名称。
 * 这既方便又直观，但如果需要显式命名，则可以使用 {@code name} 属性（或其别名 {@code value}）。
 * 另请注意，{@code name} 接受一个字符串数组，允许单个 Bean 使用多个名称（即主 Bean 名称加上一个或多个别名）。
 *
 * <pre class="code">
 *     &#064;Bean({"b1", "b2"}) // bean available as 'b1' and 'b2', but not 'myBean'
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Profile, Scope, Lazy, DependsOn, Primary, Order</h3>
 *
 * <p>Note that the {@code @Bean} annotation does not provide attributes for profile,
 * scope, lazy, depends-on or primary. Rather, it should be used in conjunction with
 * {@link Scope @Scope}, {@link Lazy @Lazy}, {@link DependsOn @DependsOn} and
 * {@link Primary @Primary} annotations to declare those semantics. For example:
 *
 * 请注意，{@code @Bean} 注解不提供 profile、scope、lazy、depends-on 或 primary 的属性。
 * 相反，它应该与 {@link Scope @Scope}、{@link Lazy @Lazy}、{@link DependsOn @DependsOn} 和 {@link Primary @Primary} 注解结合使用来声明这些语义。例如：
 *
 * <pre class="code">
 *     &#064;Bean
 *     &#064;Profile("production")
 *     &#064;Scope("prototype")
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * The semantics of the above-mentioned annotations match their use at the component
 * class level: {@code @Profile} allows for selective inclusion of certain beans.
 * {@code @Scope} changes the bean's scope from singleton to the specified scope.
 * {@code @Lazy} only has an actual effect in case of the default singleton scope.
 * {@code @DependsOn} enforces the creation of specific other beans before this
 * bean will be created, in addition to any dependencies that the bean expressed
 * through direct references, which is typically helpful for singleton startup.
 * {@code @Primary} is a mechanism to resolve ambiguity at the injection point level
 * if a single target component needs to be injected but several beans match by type.
 *
 * 上述注解的语义与它们在组件类级别的使用相匹配：{@code @Profile} 允许选择性地包含某些 bean。
 * {@code @Scope} 将 Bean 的作用域从 SINGLETON 更改为指定作用域。
 * {@code @Lazy} 仅在默认单一实例作用域的情况下具有实际效果。
 * {@code @DependsOn} 在创建此 Bean 之前强制创建特定的其他 bean，以及 bean 通过直接引用表达的任何依赖关系，这通常有助于单例启动。
 * {@code @Primary} 是一种机制，用于在需要注入单个目标组分但多个 Bean 按类型匹配时在注入点级别解决歧义。
 *
 * <p>Additionally, {@code @Bean} methods may also declare qualifier annotations
 * and {@link org.springframework.core.annotation.Order @Order} values, to be
 * taken into account during injection point resolution just like corresponding
 * annotations on the corresponding component classes but potentially being very
 * individual per bean definition (in case of multiple definitions with the same
 * bean class). Qualifiers narrow the set of candidates after the initial type match;
 * order values determine the order of resolved elements in case of collection
 * injection points (with several target beans matching by type and qualifier).
 *
 * 此外，{@code @Bean} 方法还可以声明限定符注解和 {@link org.springframework.core.annotation.Order @Order} 值，
 * 这些值将在注入点解析期间考虑在内，就像相应组件类上的相应主角儿一样，但每个 Bean 定义可能非常个性化（如果具有同一 Bean 类的多个定义）。
 * 限定符在初始类型匹配后缩小候选者集的范围;顺序值决定了在集合注入点的情况下解析元素的顺序（多个目标 Bean 按类型和限定符匹配）。
 *
 * <p><b>NOTE:</b> {@code @Order} values may influence priorities at injection points,
 * but please be aware that they do not influence singleton startup order which is an
 * orthogonal concern determined by dependency relationships and {@code @DependsOn}
 * declarations as mentioned above. Also, {@link javax.annotation.Priority} is not
 * available at this level since it cannot be declared on methods; its semantics can
 * be modeled through {@code @Order} values in combination with {@code @Primary} on
 * a single bean per type.
 *
 * 注意：{@code @Order} 值可能会影响注入点的优先级，但请注意，它们不会影响单例启动顺序，单例启动顺序是由上述依赖关系和 {@code @DependsOn} 声明确定的正交关注点。
 * 此外，{@link javax.annotation.Priority} 在此级别不可用，因为它不能在方法上声明;它的语义可以通过 {@code @Order} 值和 {@code @Primary} 组合在每个类型的单个 Bean 上建模。
 *
 * <h3>{@code @Bean} Methods in {@code @Configuration} Classes</h3>
 *
 * <p>Typically, {@code @Bean} methods are declared within {@code @Configuration}
 * classes. In this case, bean methods may reference other {@code @Bean} methods in the
 * same class by calling them <i>directly</i>. This ensures that references between beans
 * are strongly typed and navigable. Such so-called <em>'inter-bean references'</em> are
 * guaranteed to respect scoping and AOP semantics, just like {@code getBean()} lookups
 * would. These are the semantics known from the original 'Spring JavaConfig' project
 * which require CGLIB subclassing of each such configuration class at runtime. As a
 * consequence, {@code @Configuration} classes and their factory methods must not be
 * marked as final or private in this mode. For example:
 *
 * 通常，{@code @Bean} 方法在 {@code @Configuration} 类中声明。在这种情况下，Bean 方法可以通过直接调用它们来引用同一类中的其他 {@code @Bean} 方法。
 * 这确保了 Bean 之间的引用是强类型和可导航的。这种所谓的“bean 间引用”保证尊重范围和 AOP 语义，就像 {@code getBean()} 查找一样。
 * 这些是原始“Spring JavaConfig”项目中已知的语义，需要在运行时对每个此类配置类进行 CGLIB 子类化。因此，在此模式下，不得将 {@code @Configuration} 类及其工厂方法标记为 final 或 private。例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService(fooRepository());
 *     }
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     // ...
 * }</pre>
 *
 * <h3>{@code @Bean} <em>Lite</em> Mode</h3>
 *
 * {@code @Bean}精简模式
 *
 * <p>{@code @Bean} methods may also be declared within classes that are <em>not</em>
 * annotated with {@code @Configuration}. For example, bean methods may be declared
 * in a {@code @Component} class or even in a <em>plain old class</em>. In such cases,
 * a {@code @Bean} method will get processed in a so-called <em>'lite'</em> mode.
 *
 * {@code @Bean} 方法也可以在未使用 {@code @Configuration} 注解的类中声明。例如，Bean 方法可以在 {@code @Component} 类中声明，甚至可以在普通的旧类中声明。
 * 在这种情况下，{@code @Bean} 方法将以所谓的“精简”模式进行处理。
 *
 * <p>Bean methods in <em>lite</em> mode will be treated as plain <em>factory
 * methods</em> by the container (similar to {@code factory-method} declarations
 * in XML), with scoping and lifecycle callbacks properly applied. The containing
 * class remains unmodified in this case, and there are no unusual constraints for
 * the containing class or the factory methods.
 *
 * 精简模式下的 Bean 方法将被容器视为普通工厂方法（类似于 XML 中的 {@code factory-method} 声明），并正确应用范围和生命周期回调。在这种情况下，包含类保持不变，并且包含类或工厂方法没有异常约束。
 *
 * <p>In contrast to the semantics for bean methods in {@code @Configuration} classes,
 * <em>'inter-bean references'</em> are not supported in <em>lite</em> mode. Instead,
 * when one {@code @Bean}-method invokes another {@code @Bean}-method in <em>lite</em>
 * mode, the invocation is a standard Java method invocation; Spring does not intercept
 * the invocation via a CGLIB proxy. This is analogous to inter-{@code @Transactional}
 * method calls where in proxy mode, Spring does not intercept the invocation &mdash;
 * Spring does so only in AspectJ mode.
 *
 * 与 {@code @Configuration} 类中 bean 方法的语义相比， lite 模式不支持“bean 间引用”。
 * 相反，当一个 {@code @Bean} 方法在精简模式下调用另一个 {@code @Bean} 方法时，调用是标准的 Java 方法调用;
 * Spring 不会通过 CGLIB 代理拦截调用。这类似于 {@code @Transactional} 之间的方法调用，在代理模式下，Spring 不会拦截调用——Spring 仅在 AspectJ 模式下拦截调用。
 *
 * <p>For example:
 *
 * <pre class="code">
 * &#064;Component
 * public class Calculator {
 *     public int sum(int a, int b) {
 *         return a+b;
 *     }
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean();
 *     }
 * }</pre>
 *
 * <h3>Bootstrapping</h3>
 *
 * <p>See the @{@link Configuration} javadoc for further details including how to bootstrap
 * the container using {@link AnnotationConfigApplicationContext} and friends.
 *
 * 有关更多详细信息，包括如何使用 {@link AnnotationConfigApplicationContext} 和 friends 引导容器，请参阅 @{@link Configuration} javadoc。
 *
 * <h3>{@code BeanFactoryPostProcessor}-returning {@code @Bean} methods</h3>
 *
 * {@code BeanFactoryPostProcessor} 返回 {@code @Bean} 方法
 *
 * <p>Special consideration must be taken for {@code @Bean} methods that return Spring
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * ({@code BFPP}) types. Because {@code BFPP} objects must be instantiated very early in the
 * container lifecycle, they can interfere with processing of annotations such as {@code @Autowired},
 * {@code @Value}, and {@code @PostConstruct} within {@code @Configuration} classes. To avoid these
 * lifecycle issues, mark {@code BFPP}-returning {@code @Bean} methods as {@code static}. For example:
 *
 * 必须特别考虑返回 Spring {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor} （{@code BFPP}） 类型的 {@code @Bean} 方法。
 * 由于 {@code BFPP} 对象必须在容器生命周期的早期实例化，因此它们可能会干扰 {@code @Configuration} 类中 {@code @Autowired}、
 * {@code @Value} 和 {@code @PostConstruct} 等注解的处理。为避免这些生命周期问题，请将返回 {@code @Bean} 方法的 {@code BFPP} 标记为 {@code static}。例如：
 *
 * <pre class="code">
 *     &#064;Bean
 *     public static PropertySourcesPlaceholderConfigurer pspc() {
 *         // instantiate, configure and return pspc ...
 *     }
 * </pre>
 *
 * By marking this method as {@code static}, it can be invoked without causing instantiation of its
 * declaring {@code @Configuration} class, thus avoiding the above-mentioned lifecycle conflicts.
 * Note however that {@code static} {@code @Bean} methods will not be enhanced for scoping and AOP
 * semantics as mentioned above. This works out in {@code BFPP} cases, as they are not typically
 * referenced by other {@code @Bean} methods. As a reminder, an INFO-level log message will be
 * issued for any non-static {@code @Bean} methods having a return type assignable to
 * {@code BeanFactoryPostProcessor}.
 *
 * 通过将此方法标记为 {@code static}，可以在不导致其声明 {@code @Configuration} 类实例化的情况下调用它，从而避免上述生命周期冲突。
 * 但请注意，{@code static} {@code @Bean} 方法不会针对上述范围和 AOP 语义进行增强。
 * 这在 {@code BFPP} 情况下有效，因为它们通常不会被其他 {@code @Bean} 方法引用。
 * 提醒一下，对于返回类型可分配给 {@code BeanFactoryPostProcessor} 的任何非静态 {@code @Bean} 方法，将发出 INFO 级别的日志消息。
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see Configuration
 * @see Scope
 * @see DependsOn
 * @see Lazy
 * @see Primary
 * @see org.springframework.stereotype.Component
 * @see org.springframework.beans.factory.annotation.Autowired
 * @see org.springframework.beans.factory.annotation.Value
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

	/**
	 * Alias for {@link #name}.
	 * <p>Intended to be used when no other attributes are needed, for example:
	 * {@code @Bean("customBeanName")}.
	 * @since 4.3.3
	 * @see #name
	 */
	@AliasFor("name")
	String[] value() default {};

	/**
	 * The name of this bean, or if several names, a primary bean name plus aliases.
	 * <p>If left unspecified, the name of the bean is the name of the annotated method.
	 * If specified, the method name is ignored.
	 * <p>The bean name and aliases may also be configured via the {@link #value}
	 * attribute if no other attributes are declared.
	 * @see #value
	 * --
	 * 该 bean 的名称，或者如果有多个名称，则为主 bean 名称加上别名。
	 * 如果未指定，bean 的名称就是带注解的方法的名称。如果指定，则忽略方法名称。
	 * 如果没有声明其他属性，bean 名称和别名也可以通过 {@link #value} 属性进行配置。
	 */
	@AliasFor("value")
	String[] name() default {};

	/**
	 * Are dependencies to be injected via convention-based autowiring by name or type?
	 * <p>Note that this autowire mode is just about externally driven autowiring based
	 * on bean property setter methods by convention, analogous to XML bean definitions.
	 * <p>The default mode does allow for annotation-driven autowiring. "no" refers to
	 * externally driven autowiring only, not affecting any autowiring demands that the
	 * bean class itself expresses through annotations.
	 * @see Autowire#BY_NAME
	 * @see Autowire#BY_TYPE
	 * @deprecated as of 5.1, since {@code @Bean} factory method argument resolution and
	 * {@code @Autowired} processing supersede name/type-based bean property injection
	 */
	@Deprecated
	Autowire autowire() default Autowire.NO;

	/**
	 * Is this bean a candidate for getting autowired into some other bean?
	 * <p>Default is {@code true}; set this to {@code false} for internal delegates
	 * that are not meant to get in the way of beans of the same type in other places.
	 * @since 5.1
	 * 此 Bean 是否是自动装配的候选项？
	 * <p>默认值为 {@code true}；将其设置为 {@code false} 用于内部委托，
	 * 这些委托不应该妨碍在其他位置的相同类型的 Beans。
	 * 如果将其设置为 false，则表明此 Bean 不应该作为自动装配的候选项。
	 */
	boolean autowireCandidate() default true;

	/**
	 * The optional name of a method to call on the bean instance during initialization.
	 * Not commonly used, given that the method may be called programmatically directly
	 * within the body of a Bean-annotated method.
	 * <p>The default value is {@code ""}, indicating no init method to be called.
	 * @see org.springframework.beans.factory.InitializingBean
	 * @see org.springframework.context.ConfigurableApplicationContext#refresh()
	 */
	String initMethod() default "";

	/**
	 * The optional name of a method to call on the bean instance upon closing the
	 * application context, for example a {@code close()} method on a JDBC
	 * {@code DataSource} implementation, or a Hibernate {@code SessionFactory} object.
	 * The method must have no arguments but may throw any exception.
	 * <p>As a convenience to the user, the container will attempt to infer a destroy
	 * method against an object returned from the {@code @Bean} method. For example, given
	 * an {@code @Bean} method returning an Apache Commons DBCP {@code BasicDataSource},
	 * the container will notice the {@code close()} method available on that object and
	 * automatically register it as the {@code destroyMethod}. This 'destroy method
	 * inference' is currently limited to detecting only public, no-arg methods named
	 * 'close' or 'shutdown'. The method may be declared at any level of the inheritance
	 * hierarchy and will be detected regardless of the return type of the {@code @Bean}
	 * method (i.e., detection occurs reflectively against the bean instance itself at
	 * creation time).
	 * <p>To disable destroy method inference for a particular {@code @Bean}, specify an
	 * empty string as the value, e.g. {@code @Bean(destroyMethod="")}. Note that the
	 * {@link org.springframework.beans.factory.DisposableBean} callback interface will
	 * nevertheless get detected and the corresponding destroy method invoked: In other
	 * words, {@code destroyMethod=""} only affects custom close/shutdown methods and
	 * {@link java.io.Closeable}/{@link java.lang.AutoCloseable} declared close methods.
	 * <p>Note: Only invoked on beans whose lifecycle is under the full control of the
	 * factory, which is always the case for singletons but not guaranteed for any
	 * other scope.
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	String destroyMethod() default AbstractBeanDefinition.INFER_METHOD;

}
