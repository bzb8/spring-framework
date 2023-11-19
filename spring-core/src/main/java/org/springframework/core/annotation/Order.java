/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.Ordered;

/**
 * {@code @Order} defines the sort order for an annotated component.
 *
 * {@code @Order} 定义带注释的组件的排序顺序。
 *
 * <p>The {@link #value} is optional and represents an order value as defined in the
 * {@link Ordered} interface. Lower values have higher priority. The default value is
 * {@code Ordered.LOWEST_PRECEDENCE}, indicating the lowest priority (losing to any
 * other specified order value).
 *
 * {@link #value}是可选的，表示{@link Ordered}接口中定义的order值。值越低，优先级越高。默认值为{@code Ordered.LOWEST_PRECEDENCE}，表示最低优先级（输给任何其他指定的order值）
 *
 * <p><b>NOTE:</b> Since Spring 4.0, annotation-based ordering is supported for many
 * kinds of components in Spring, even for collection injection where the order values
 * of the target components are taken into account (either from their target class or
 * from their {@code @Bean} method). While such order values may influence priorities
 * at injection points, please be aware that they do not influence singleton startup
 * order which is an orthogonal concern determined by dependency relationships and
 * {@code @DependsOn} declarations (influencing a runtime-determined dependency graph).
 *
 * 注意：从Spring 4.0开始，Spring中的许多组件都支持基于注解的排序，即使是考虑目标组件的顺序值的集合注入（来自它们的目标类或它们的 {@code @Bean} 方法）。
 * 虽然这些顺序值可能会影响注入点的优先级，但请注意，它们不会影响单例启动顺序，单例启动顺序是由依赖关系和 {@code @DependsOn} 声明（影响运行时确定的依赖关系图）确定的正交关注点。
 *
 * <p>Since Spring 4.1, the standard {@link javax.annotation.Priority} annotation
 * can be used as a drop-in replacement for this annotation in ordering scenarios.
 * Note that {@code @Priority} may have additional semantics when a single element
 * has to be picked (see {@link AnnotationAwareOrderComparator#getPriority}).
 *
 * 从 Spring 4.1 开始，在排序场景中，标准的 {@link javax.annotation.Priority} 注解可以用作此注解的直接替代品。
 * 请注意，当必须选取单个元素时，{@code @Priority} 可能具有其他语义（请参阅 {@link AnnotationAwareOrderComparator#getPriority}）。
 *
 * <p>Alternatively, order values may also be determined on a per-instance basis
 * through the {@link Ordered} interface, allowing for configuration-determined
 * instance values instead of hard-coded values attached to a particular class.
 *
 * 或者，也可以通过 {@link Ordered} 接口按实例确定顺序值，从而允许配置确定的实例值，而不是附加到特定类的硬编码值
 *
 * <p>Consult the javadoc for {@link org.springframework.core.OrderComparator
 * OrderComparator} for details on the sort semantics for non-ordered objects.
 * 有关无序对象的排序语义的详细信息，请参阅 javadoc 以获取 {@link org.springframework.core.OrderComparator OrderComparator}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.core.Ordered
 * @see AnnotationAwareOrderComparator
 * @see OrderUtils
 * @see javax.annotation.Priority
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface Order {

	/**
	 * The order value.
	 * <p>Default is {@link Ordered#LOWEST_PRECEDENCE}.
	 * @see Ordered#getOrder()
	 */
	int value() default Ordered.LOWEST_PRECEDENCE;

}
