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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/**
 * Indicates that a component is eligible for registration when one or more
 * {@linkplain #value specified profiles} are active.
 *
 * 指示当一个或多个 {@linkplain value specified profiles} 处于活动状态时，组件符合注册条件。
 *
 * <p>A <em>profile</em> is a named logical grouping that may be activated
 * programmatically via {@link ConfigurableEnvironment#setActiveProfiles} or declaratively
 * by setting the {@link AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 * spring.profiles.active} property as a JVM system property, as an
 * environment variable, or as a Servlet context parameter in {@code web.xml}
 * for web applications. Profiles may also be activated declaratively in
 * integration tests via the {@code @ActiveProfiles} annotation.
 * <p><em>配置文件<em>是一个命名的逻辑分组，可以通过 {@link ConfigurableEnvironmentsetActiveProfiles} 以编程方式激活，
 * 也可以通过将 {@link AbstractEnvironmentACTIVE_PROFILES_PROPERTY_NAME spring.profiles.active} 属性设置为 JVM 系统属性、
 * 环境变量或 Web 应用程序的 {@code web.xml} 中的 Servlet 上下文参数来声明性地激活。
 * 配置文件也可以通过 {@code @ActiveProfiles} 注释在集成测试中以声明方式激活。
 *
 * <p>The {@code @Profile} annotation may be used in any of the following ways:
 * {@code @Profile} 注解可以以下列任何一种方式使用：
 * <ul>
 * <li>as a type-level annotation on any class directly or indirectly annotated with
 * {@code @Component}, including {@link Configuration @Configuration} classes</li>
 * 作为直接或间接使用 {@code @Component} 批注的任何类的类型级批注，包括 {@link Configuration @Configuration} 类
 * <li>as a meta-annotation, for the purpose of composing custom stereotype annotations</li>
 * 作为元注释，用于编写自定义构造型注释
 * <li>as a method-level annotation on any {@link Bean @Bean} method</li>
 * 作为任何 {@link Bean @Bean} 方法的方法级注释
 * </ul>
 *
 * <p>If a {@code @Configuration} class is marked with {@code @Profile}, all of the
 * {@code @Bean} methods and {@link Import @Import} annotations associated with that class
 * will be bypassed unless one or more of the specified profiles are active. A profile
 * string may contain a simple profile name (for example {@code "p1"}) or a profile
 * expression. A profile expression allows for more complicated profile logic to be
 * expressed, for example {@code "p1 & p2"}. See {@link Profiles#of(String...)} for more
 * details about supported formats.
 * <p>如果 {@code @Configuration} 类标有 {@code @Profile}，则将绕过与该类关联的所有 {@code @Bean} 方法和
 * {@link Import @Import} 注解，除非一个或多个指定的配置文件处于活动状态。profile字符串可以包含简单的配置文件名称（例如 {@code “p1”}）
 * 或profile表达式。配置文件表达式允许表达更复杂的配置文件逻辑，例如 {@code “p1 & p2”}。
 * 有关支持的格式的更多详细信息，请参阅 {@link Profiles.of（String...）}。
 *
 * <p>This is analogous to the behavior in Spring XML: if the {@code profile} attribute of
 * the {@code beans} element is supplied e.g., {@code <beans profile="p1,p2">}, the
 * {@code beans} element will not be parsed unless at least profile 'p1' or 'p2' has been
 * activated. Likewise, if a {@code @Component} or {@code @Configuration} class is marked
 * with {@code @Profile({"p1", "p2"})}, that class will not be registered or processed unless
 * at least profile 'p1' or 'p2' has been activated.
 *
 * <p>这类似于 Spring XML 中的行为：如果提供了 {@code beans} 元素的 {@code profile} 属性，例如 {@code <beans profile=“p1，p2”>}，
 * 则除非至少激活了配置文件“p1”或“p2”，否则不会解析 {@code beans} 元素。同样，如果 {@code @Component} 或 {@code @Configuration} 类标记为
 * {@code @Profile（{“p1”， “p2”}）}，则除非至少激活了配置文件“p1”或“p2”，否则不会注册或处理该类。
 *
 * <p>If a given profile is prefixed with the NOT operator ({@code !}), the annotated
 * component will be registered if the profile is <em>not</em> active &mdash; for example,
 * given {@code @Profile({"p1", "!p2"})}, registration will occur if profile 'p1' is active
 * or if profile 'p2' is <em>not</em> active.
 * <p>如果给定的配置文件以 NOT 运算符 （{@code ！} 为前缀，则在profile未处于活动状态时将注册带该注解的组件 — 例如，
 * 给定 {@code @Profile（{“p1”， “！p2”}<em><em>）}，如果配置文件“p1”处于活动状态或配置文件“p2”<em>未处于活动状态，则将进行注册<em>。
 *
 * <p>If the {@code @Profile} annotation is omitted, registration will occur regardless
 * of which (if any) profiles are active.
 * <p>如果省略 {@code @Profile} 注释，则无论哪个（如果有）配置文件处于活动状态，都将进行注册。
 *
 * <p><b>NOTE:</b> With {@code @Profile} on {@code @Bean} methods, a special scenario may
 * apply: In the case of overloaded {@code @Bean} methods of the same Java method name
 * (analogous to constructor overloading), an {@code @Profile} condition needs to be
 * consistently declared on all overloaded methods. If the conditions are inconsistent,
 * only the condition on the first declaration among the overloaded methods will matter.
 * {@code @Profile} can therefore not be used to select an overloaded method with a
 * particular argument signature over another; resolution between all factory methods
 * for the same bean follows Spring's constructor resolution algorithm at creation time.
 * <b>Use distinct Java method names pointing to the same {@link Bean#name bean name}
 * if you'd like to define alternative beans with different profile conditions</b>;
 * see {@code ProfileDatabaseConfig} in {@link Configuration @Configuration}'s javadoc.
 *
 * <p><b>注意：<b>在 {@code @Bean} 方法上使用 {@code @Profile} 时，可能会出现特殊情况：
 * 如果重载 {@code @Bean} 方法具有相同的 Java 方法名称（类似于构造函数重载），则需要在所有重载方法上一致地声明 {@code @Profile} 条件。
 * 如果条件不一致，则只有重载方法中第一个声明的条件才有意义。因此，{@code @Profile} 不能用于选择具有特定参数签名的重载方法，而不是另一个方法;
 * 同一 Bean 的所有工厂方法之间的解析在创建时遵循 Spring 的构造函数解析算法。<b>如果要定义具有不同配置文件条件的替代 Bean，
 * 请使用指向同一 {@link Beanname bean name} 的不同 Java 方法名称<b>;
 * 请参阅 {@link Configuration @Configuration} 的 javadoc 中的 {@code ProfileDatabaseConfig}。
 *
 * <p>When defining Spring beans via XML, the {@code "profile"} attribute of the
 * {@code <beans>} element may be used. See the documentation in the
 * {@code spring-beans} XSD (version 3.1 or greater) for details.
 *
 * <p>通过XML定义Spring bean时，可以使用{@code}元素的{@code “profile”<beans>}属性。有关详细信息，请参阅 {@code spring-beans} XSD（版本 3.1 或更高版本）中的文档。
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1
 * @see ConfigurableEnvironment#setActiveProfiles
 * @see ConfigurableEnvironment#setDefaultProfiles
 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
 * @see Conditional
 * @see org.springframework.test.context.ActiveProfiles
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ProfileCondition.class)
public @interface Profile {

	/**
	 * The set of profiles for which the annotated component should be registered.
	 */
	String[] value();

}
