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
 * Indicates whether a bean is to be lazily initialized.
 *
 * 指示是否要延迟初始化 Bean。
 *
 * <p>May be used on any class directly or indirectly annotated with {@link
 * org.springframework.stereotype.Component @Component} or on methods annotated with
 * {@link Bean @Bean}.
 *
 * 可以直接或间接地用于{@link org.springframework.stereotype.Component @Component}注解的任何类，也可以在{@link Bean @Bean}注解的方法上使用。
 *
 * <p>If this annotation is not present on a {@code @Component} or {@code @Bean} definition,
 * eager initialization will occur. If present and set to {@code true}, the {@code @Bean} or
 * {@code @Component} will not be initialized until referenced by another bean or explicitly
 * retrieved from the enclosing {@link org.springframework.beans.factory.BeanFactory
 * BeanFactory}. If present and set to {@code false}, the bean will be instantiated on
 * startup by bean factories that perform eager initialization of singletons.
 *
 * 如果 {@code @Component} 或 {@code @Bean} 定义上不存在此注解，则将发生预先初始化。如果存在并设置为 {@code true}，则 {@code @Bean} 或
 * {@code @Component} 不会被初始化，直到被另一个 Bean 引用或从封闭的 {@link org.springframework.beans.factory.BeanFactory BeanFactory} 中显式检索。
 * 如果存在并设置为 {@code false}，则 Bean 工厂将在启动时由执行单例快速初始化的 Bean 工厂实例化。
 *
 * <p>If Lazy is present on a {@link Configuration @Configuration} class, this
 * indicates that all {@code @Bean} methods within that {@code @Configuration}
 * should be lazily initialized. If {@code @Lazy} is present and false on a {@code @Bean}
 * method within a {@code @Lazy}-annotated {@code @Configuration} class, this indicates
 * overriding the 'default lazy' behavior and that the bean should be eagerly initialized.
 *
 * 如果 {@link Configuration @Configuration} 类上存在 Lazy，则表示该 {@code @Configuration} 中的所有 {@code @Bean} 方法都应延迟初始化。
 * 如果 {@code @Lazy} 在 {@code @Lazy} 注解的 {@code @Configuration} 类中的 {@code @Bean} 方法上存在且为 false，则表示覆盖了“默认惰性”行为，并且应该急切地初始化 bean。
 *
 * <p>In addition to its role for component initialization, this annotation may also be placed
 * on injection points marked with {@link org.springframework.beans.factory.annotation.Autowired}
 * or {@link javax.inject.Inject}: In that context, it leads to the creation of a
 * lazy-resolution proxy for all affected dependencies, as an alternative to using
 * {@link org.springframework.beans.factory.ObjectFactory} or {@link javax.inject.Provider}.
 * Please note that such a lazy-resolution proxy will always be injected; if the target
 * dependency does not exist, you will only be able to find out through an exception on
 * invocation. As a consequence, such an injection point results in unintuitive behavior
 * for optional dependencies. For a programmatic equivalent, allowing for lazy references
 * with more sophistication, consider {@link org.springframework.beans.factory.ObjectProvider}.
 *
 * 除了组件初始化的作用外，此注解还可以放置在标有{@link org.springframework.beans.factory.annotation.Autowired}或{@link javax.inject.Inject}的注入点上：
 * 在这种情况下，它会导致为所有受影响的依赖项创建一个延迟解析代理，作为使用 {@link org.springframework.beans.factory.ObjectFactory} 或 {@link javax.inject.Provider} 的替代方法。
 * 请注意，这种延迟解析代理将始终被注入;如果目标依赖项不存在，则只能通过调用异常来查找。因此，这样的注入点会导致可选依赖项出现不直观的行为。对于允许更复杂的惰性引用的编程等价物，
 * 请考虑{@link org.springframework.beans.factory.ObjectProvider}。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Primary
 * @see Bean
 * @see Configuration
 * @see org.springframework.stereotype.Component
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lazy {

	/**
	 * Whether lazy initialization should occur.
	 */
	boolean value() default true;

}
