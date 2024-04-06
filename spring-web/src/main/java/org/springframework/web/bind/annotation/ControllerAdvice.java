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

package org.springframework.web.bind.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * Specialization of {@link Component @Component} for classes that declare
 * {@link ExceptionHandler @ExceptionHandler}, {@link InitBinder @InitBinder}, or
 * {@link ModelAttribute @ModelAttribute} methods to be shared across
 * multiple {@code @Controller} classes.
 * {@link Component @Component}注解的一个特殊化，用于声明具有{@link ExceptionHandler @ExceptionHandler}、
 * {@link InitBinder @InitBinder}或{@link ModelAttribute @ModelAttribute}方法的类，
 * 这些方法可以在多个{@code @Controller}类之间共享。
 *
 * <p>Classes annotated with {@code @ControllerAdvice} can be declared explicitly
 * as Spring beans or auto-detected via classpath scanning. All such beans are
 * sorted based on {@link org.springframework.core.Ordered Ordered} semantics or
 * {@link org.springframework.core.annotation.Order @Order} /
 * {@link javax.annotation.Priority @Priority} declarations, with {@code Ordered}
 * semantics taking precedence over {@code @Order} / {@code @Priority} declarations.
 * {@code @ControllerAdvice} beans are then applied in that order at runtime.
 * Note, however, that {@code @ControllerAdvice} beans that implement
 * {@link org.springframework.core.PriorityOrdered PriorityOrdered} are <em>not</em>
 * given priority over {@code @ControllerAdvice} beans that implement {@code Ordered}.
 * In addition, {@code Ordered} is not honored for scoped {@code @ControllerAdvice}
 * beans &mdash; for example if such a bean has been configured as a request-scoped
 * or session-scoped bean.  For handling exceptions, an {@code @ExceptionHandler}
 * will be picked on the first advice with a matching exception handler method. For
 * model attributes and data binding initialization, {@code @ModelAttribute} and
 * {@code @InitBinder} methods will follow {@code @ControllerAdvice} order.
 * <p>标记有{@code @ControllerAdvice}的类可以显式声明为Spring Bean，或通过类路径扫描自动检测。
 * 所有这类Bean都会根据{@link org.springframework.core.Ordered Ordered}语义或
 * {@link org.springframework.core.annotation.Order @Order} / {@link javax.annotation.Priority @Priority}声明进行排序。
 * 其中，{@code Ordered}语义优先于{@code @Order} / {@code @Priority}声明。
 * 运行时，{@code @ControllerAdvice} Bean会按照该顺序应用。
 * 但是，实现{@link org.springframework.core.PriorityOrdered PriorityOrdered}的{@code @ControllerAdvice} Bean
 * 不会优先于实现{@code Ordered}的{@code @ControllerAdvice} Bean。
 * 此外，对于有作用域的{@code @ControllerAdvice} Bean（例如，配置为请求作用域或会话作用域的Bean），
 * {@code Ordered}不被尊重。对于异常处理，将选择第一个匹配的异常处理方法。
 * 对于模型属性和数据绑定初始化，{@code @ModelAttribute}和{@code @InitBinder}方法将遵循{@code @ControllerAdvice}的顺序。
 *
 * <p>Note: For {@code @ExceptionHandler} methods, a root exception match will be
 * preferred to just matching a cause of the current exception, among the handler
 * methods of a particular advice bean. However, a cause match on a higher-priority
 * advice will still be preferred over any match (whether root or cause level)
 * on a lower-priority advice bean. As a consequence, please declare your primary
 * root exception mappings on a prioritized advice bean with a corresponding order.
 * <p>对于{@code @ExceptionHandler}方法，默认情况下，将优先选择匹配根异常的方法，而不是仅匹配当前异常的原因。
 * 但是在高优先级的建议中，原因匹配仍然会优先于低优先级建议中的任何匹配（无论是根异常还是原因异常）。
 * 因此，请在具有相应顺序的优先级建议Bean上声明您的主要根异常映射。
 *
 * <p>By default, the methods in an {@code @ControllerAdvice} apply globally to
 * all controllers. Use selectors such as {@link #annotations},
 * {@link #basePackageClasses}, and {@link #basePackages} (or its alias
 * {@link #value}) to define a more narrow subset of targeted controllers.
 * If multiple selectors are declared, boolean {@code OR} logic is applied, meaning
 * selected controllers should match at least one selector. Note that selector checks
 * are performed at runtime, so adding many selectors may negatively impact
 * performance and add complexity.
 * <p>默认情况下，{@code @ControllerAdvice}中的方法适用于所有控制器。
 * 使用选择器（如{@link #annotations}、{@link #basePackageClasses}和{@link #basePackages}（或其别名{@link #value}））
 * 来定义更窄的目标控制器子集。
 * 如果声明了多个选择器，则会应用布尔{@code OR}逻辑，这意味着选定的控制器应匹配至少一个选择器。
 * 请注意，选择器检查在运行时执行，因此添加许多选择器可能会对性能产生负面影响并增加复杂性。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 3.2
 * @see org.springframework.stereotype.Controller
 * @see RestControllerAdvice
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ControllerAdvice {

	/**
	 * Alias for the {@link #basePackages} attribute.
	 * <p>Allows for more concise annotation declarations &mdash; for example,
	 * {@code @ControllerAdvice("org.my.pkg")} is equivalent to
	 * {@code @ControllerAdvice(basePackages = "org.my.pkg")}.
	 * @since 4.0
	 * @see #basePackages
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * Array of base packages.
	 * <p>Controllers that belong to those base packages or sub-packages thereof
	 * will be included &mdash; for example,
	 * {@code @ControllerAdvice(basePackages = "org.my.pkg")} or
	 * {@code @ControllerAdvice(basePackages = {"org.my.pkg", "org.my.other.pkg"})}.
	 * <p>{@link #value} is an alias for this attribute, simply allowing for
	 * more concise use of the annotation.
	 * <p>Also consider using {@link #basePackageClasses} as a type-safe
	 * alternative to String-based package names.
	 * @since 4.0
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages} for specifying the packages
	 * in which to select controllers to be advised by the {@code @ControllerAdvice}
	 * annotated class.
	 * <p>Consider creating a special no-op marker class or interface in each package
	 * that serves no purpose other than being referenced by this attribute.
	 * @since 4.0
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Array of classes.
	 * <p>Controllers that are assignable to at least one of the given types
	 * will be advised by the {@code @ControllerAdvice} annotated class.
	 * @since 4.0
	 */
	Class<?>[] assignableTypes() default {};

	/**
	 * Array of annotation types.
	 * <p>Controllers that are annotated with at least one of the supplied annotation
	 * types will be advised by the {@code @ControllerAdvice} annotated class.
	 * <p>Consider creating a custom composed annotation or use a predefined one,
	 * like {@link RestController @RestController}.
	 * @since 4.0
	 */
	Class<? extends Annotation>[] annotations() default {};

}
