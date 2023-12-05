/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a constructor, field, setter method, or config method as to be autowired by
 * Spring's dependency injection facilities. This is an alternative to the JSR-330
 * {@link javax.inject.Inject} annotation, adding required-vs-optional semantics.
 *
 * 将构造函数、字段、setter 方法或 config 方法标记为由 Spring 的依赖注入工具自动注入。这是 JSR-330 {@link javax.inject.Inject} 注解的替代方法，添加了必需语义与可选语义。
 *
 * <h3>Autowired Constructors</h3>
 *
 * Autowired 构造函数
 *
 * <p>Only one constructor of any given bean class may declare this annotation with the
 * {@link #required} attribute set to {@code true}, indicating <i>the</i> constructor
 * to autowire when used as a Spring bean. Furthermore, if the {@code required}
 * attribute is set to {@code true}, only a single constructor may be annotated
 * with {@code @Autowired}. If multiple <i>non-required</i> constructors declare the
 * annotation, they will be considered as candidates for autowiring. The constructor
 * with the greatest number of dependencies that can be satisfied by matching beans
 * in the Spring container will be chosen. If none of the candidates can be satisfied,
 * then a primary/default constructor (if present) will be used. Similarly, if a
 * class declares multiple constructors but none of them is annotated with
 * {@code @Autowired}, then a primary/default constructor (if present) will be used.
 * If a class only declares a single constructor to begin with, it will always be used,
 * even if not annotated. An annotated constructor does not have to be public.
 *
 * 任何给定 bean 类只有一个构造函数可以在 {@link #required} 属性设置为 {@code true} 的情况下声明此注解，指示构造函数在用作 Spring Bean 时自动注入。
 * 此外，如果 {@code required} 属性设置为 {@code true}，则只能使用 {@code @Autowired} 注解单个构造函数。
 * 如果多个非必需的构造函数声明了注释，则它们将被视为自动注入的候选项。将选择具有最多依赖项的构造函数，这些构造函数可以通过匹配 Spring 容器中的 bean 来满足。
 * 如果不能满足任何候选者，则将使用 primary/default 构造函数（如果存在）。同样，如果一个类声明了多个构造函数，但没有一个构造函数使用 {@code @Autowired} 进行注释，
 * 则将使用 primary/default 构造函数（如果存在）。如果一个类一开始只声明一个构造函数，那么即使没有注解，它也将始终被使用。带注解的构造函数不必是公共的。
 *
 * <h3>Autowired Fields</h3>
 * <p>Fields are injected right after construction of a bean, before any config methods
 * are invoked. Such a config field does not have to be public.
 *
 * 在构造 Bean 之后，在调用任何配置方法之前，立即注入字段。这样的配置字段不必是公共的。
 *
 * <h3>Autowired Methods</h3>
 * <p>Config methods may have an arbitrary name and any number of arguments; each of
 * those arguments will be autowired with a matching bean in the Spring container.
 * Bean property setter methods are effectively just a special case of such a general
 * config method. Such config methods do not have to be public.
 *
 * Config 方法可以具有任意名称和任意数量的参数;这些参数中的每一个都将与 Spring 容器中的匹配 bean 自动注入。
 * Bean 属性 setter 方法实际上只是这种通用配置方法的一个特例。此类配置方法不必是公共的。
 *
 * <h3>Autowired Parameters</h3>
 * <p>Although {@code @Autowired} can technically be declared on individual method
 * or constructor parameters since Spring Framework 5.0, most parts of the
 * framework ignore such declarations. The only part of the core Spring Framework
 * that actively supports autowired parameters is the JUnit Jupiter support in
 * the {@code spring-test} module (see the
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#testcontext-junit-jupiter-di">TestContext framework</a>
 * reference documentation for details).
 *
 * 尽管从 Spring Framework 5.0 开始，从技术上讲，{@code @Autowired} 可以在单个方法或构造函数参数上声明，但框架的大多数部分都忽略了此类声明。
 * 核心 Spring Framework 中唯一主动支持自动注入参数的部分是 {@code spring-test} 模块中的 JUnit Jupiter 支持
 * （有关详细信息，请参阅 <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#testcontext-junit-jupiter-di">TestContext 框架参考文档）。
 *
 * <h3>Multiple Arguments and 'required' Semantics</h3>
 *
 * 多个参数和“必需”语义
 *
 * <p>In the case of a multi-arg constructor or method, the {@link #required} attribute
 * is applicable to all arguments. Individual parameters may be declared as Java-8 style
 * {@link java.util.Optional} or, as of Spring Framework 5.0, also as {@code @Nullable}
 * or a not-null parameter type in Kotlin, overriding the base 'required' semantics.
 *
 * 对于多参数构造函数或方法，{@link #required} 属性适用于所有参数。单个参数可以声明为 Java-8 样式 {@link java.util.Optional}，
 * 或者从 Spring Framework 5.0 开始，也可以声明为 {@code @Nullable} 或 Kotlin 中的非 null 参数类型，覆盖基本的“必需”语义。
 *
 * <h3>Autowiring Arrays, Collections, and Maps</h3>
 * <p>In case of an array, {@link java.util.Collection}, or {@link java.util.Map}
 * dependency type, the container autowires all beans matching the declared value
 * type. For such purposes, the map keys must be declared as type {@code String}
 * which will be resolved to the corresponding bean names. Such a container-provided
 * collection will be ordered, taking into account
 * {@link org.springframework.core.Ordered Ordered} and
 * {@link org.springframework.core.annotation.Order @Order} values of the target
 * components, otherwise following their registration order in the container.
 * Alternatively, a single matching target bean may also be a generally typed
 * {@code Collection} or {@code Map} itself, getting injected as such.
 *
 * 如果是数组、{@link java.util.Collection} 或 {@link java.util.Map} 依赖项类型，容器会自动注入与声明的值类型匹配的所有 bean。
 * 为此，必须将映射键声明为类型 {@code String}，该类型将被解析为相应的 Bean 名称。
 * 考虑到目标组件的{@link org.springframework.core.Ordered Ordered}和{@link org.springframework.core.annotation.Order @Order}值，
 * 将对容器提供的此类集合进行排序，否则遵循它们在容器中的注册顺序。或者，单个匹配的目标 Bean 也可以是通常类型的 {@code Collection} 或 {@code Map} 本身，并以这种方式注入。
 *
 * <h3>Not supported in {@code BeanPostProcessor} or {@code BeanFactoryPostProcessor}</h3>
 * <p>Note that actual injection is performed through a
 * {@link org.springframework.beans.factory.config.BeanPostProcessor
 * BeanPostProcessor} which in turn means that you <em>cannot</em>
 * use {@code @Autowired} to inject references into
 * {@link org.springframework.beans.factory.config.BeanPostProcessor
 * BeanPostProcessor} or
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * types. Please consult the javadoc for the {@link AutowiredAnnotationBeanPostProcessor}
 * class (which, by default, checks for the presence of this annotation).
 *
 * 请注意，实际注入是通过{@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor}执行的，
 * 这反过来意味着您不能使用{@code @Autowired}将引用注入到{@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor}
 * 或{@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}类型中。
 * 请查阅 javadoc 以获取 {@link AutowiredAnnotationBeanPostProcessor} 类（默认情况下，该类会检查此注解是否存在）。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 2.5
 * @see AutowiredAnnotationBeanPostProcessor
 * @see Qualifier
 * @see Value
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

	/**
	 * Declares whether the annotated dependency is required.
	 * <p>Defaults to {@code true}.
	 */
	boolean required() default true;

}
