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

/**
 * Enables support for handling components marked with AspectJ's {@code @Aspect} annotation,
 * similar to functionality found in Spring's {@code <aop:aspectj-autoproxy>} XML element.
 * To be used on @{@link Configuration} classes as follows:
 *
 * 支持处理标有 AspectJ 的 {@code @Aspect} 注解的组件，类似于 Spring 的 {@code <aop:aspectj-autoproxy>} XML 元素中的功能。
 * 用于 @{@link Configuration} 类，如下所示：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService();
 *     }
 *
 *     &#064;Bean
 *     public MyAspect myAspect() {
 *         return new MyAspect();
 *     }
 * }</pre>
 *
 * Where {@code FooService} is a typical POJO component and {@code MyAspect} is an
 * {@code @Aspect}-style aspect:
 *
 * <pre class="code">
 * public class FooService {
 *
 *     // various methods
 * }</pre>
 *
 * <pre class="code">
 * &#064;Aspect
 * public class MyAspect {
 *
 *     &#064;Before("execution(* FooService+.*(..))")
 *     public void advice() {
 *         // advise FooService methods as appropriate
 *     }
 * }</pre>
 *
 * In the scenario above, {@code @EnableAspectJAutoProxy} ensures that {@code MyAspect}
 * will be properly processed and that {@code FooService} will be proxied mixing in the
 * advice that it contributes.
 *
 * 在上面的场景中，{@code @EnableAspectJAutoProxy} 确保 {@code MyAspect} 将得到正确处理，并且 {@code FooService} 将被代理，
 * 并混合到它提供的建议中。
 *
 * <p>Users can control the type of proxy that gets created for {@code FooService} using
 * the {@link #proxyTargetClass()} attribute. The following enables CGLIB-style 'subclass'
 * proxies as opposed to the default interface-based JDK proxy approach.
 *
 * 用户可以使用 {@link #proxyTargetClass()} 属性控制为 {@code FooService} 创建的代理类型。
 * 下面启用 CGLIB 样式的“子类”代理，而不是默认的基于接口的 JDK 代理方法。
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy(proxyTargetClass=true)
 * public class AppConfig {
 *     // ...
 * }</pre>
 *
 * <p>Note that {@code @Aspect} beans may be component-scanned like any other.
 * Simply mark the aspect with both {@code @Aspect} and {@code @Component}:
 *
 * 请注意，{@code @Aspect} 的Bean 可以像其他任何 Bean 一样进行组件扫描。只需用 {@code @Aspect} 和 {@code @Component} 标记aspect：
 *
 * <pre class="code">
 * package com.foo;
 *
 * &#064;Component
 * public class FooService { ... }
 *
 * &#064;Aspect
 * &#064;Component
 * public class MyAspect { ... }</pre>
 *
 * Then use the @{@link ComponentScan} annotation to pick both up:
 *
 * 然后使用 @{@link ComponentScan} 注解来获取这两个注解：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan("com.foo")
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     // no explicit &#064;Bean definitions required
 * }</pre>
 *
 * <b>Note: {@code @EnableAspectJAutoProxy} applies to its local application context only,
 * allowing for selective proxying of beans at different levels.</b> Please redeclare
 * {@code @EnableAspectJAutoProxy} in each individual context, e.g. the common root web
 * application context and any separate {@code DispatcherServlet} application contexts,
 * if you need to apply its behavior at multiple levels.
 *
 * 注意：{@code @EnableAspectJAutoProxy} 仅适用于其本地应用程序上下文，允许在不同级别选择性代理 bean。
 * 如果您需要在多个级别应用其行为，请在每个单独的上下文中重新声明 {@code @EnableAspectJAutoProxy}，
 * 例如公共根 Web 应用程序上下文和任何单独的 {@code DispatcherServlet} 应用程序上下文。
 *
 * <p>This feature requires the presence of {@code aspectjweaver} on the classpath.
 * While that dependency is optional for {@code spring-aop} in general, it is required
 * for {@code @EnableAspectJAutoProxy} and its underlying facilities.
 *
 * 此功能要求类路径上存在 {@code aspectjweaver}。虽然该依赖关系对于 {@code spring-aop} 通常是可选的，
 * 但对于 {@code @EnableAspectJAutoProxy} 及其底层设施是必需的。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see org.aspectj.lang.annotation.Aspect
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies. The default is {@code false}.
	 *
	 * 指示是否要创建基于子类 （CGLIB） 的代理，而不是基于标准 Java 接口的代理。默认值为 {@code false}。
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate that the proxy should be exposed by the AOP framework as a {@code ThreadLocal}
	 * for retrieval via the {@link org.springframework.aop.framework.AopContext} class.
	 * Off by default, i.e. no guarantees that {@code AopContext} access will work.
	 *
	 * 指示 AOP 框架应将代理公开为 {@code ThreadLocal}，以便通过 {@link org.springframework.aop.framework.AopContext} 类进行检索。
	 * 默认情况下关闭，即不保证 {@code AopContext} 访问将起作用。
	 *
	 * @since 4.3.1
	 */
	boolean exposeProxy() default false;

}
