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

/**
 * Indicates that a component is only eligible for registration when all
 * {@linkplain #value specified conditions} match.
 *
 * 指示只有当所有{@linkplain #value 指定条件}都匹配时，组件才有资格注册。
 *
 * <p>A <em>condition</em> is any state that can be determined programmatically
 * before the bean definition is due to be registered (see {@link Condition} for details).
 *
 * 条件是在bean定义被注册之前可以通过编程确定的任何状态（有关详细信息，请参阅{@link Condition}）。
 *
 * <p>The {@code @Conditional} annotation may be used in any of the following ways:
 *
 * {@code @Conditional} 注解可以通过以下任意方式使用：
 *
 * <ul>
 * <li>as a type-level annotation on any class directly or indirectly annotated with
 * {@code @Component}, including {@link Configuration @Configuration} classes</li>
 *
 * 作为直接或间接使用 {@code @Component} 注解的任何类的类型级注释，包括 {@link Configuration @Configuration} 类
 *
 * <li>as a meta-annotation, for the purpose of composing custom stereotype
 * annotations</li>
 *
 * 作为元注解，用于编写自定义构造型注解
 *
 * <li>as a method-level annotation on any {@link Bean @Bean} method</li>
 *
 * 作为任何 {@link Bean @Bean} 方法上的方法级注解
 *
 * </ul>
 *
 * <p>If a {@code @Configuration} class is marked with {@code @Conditional},
 * all of the {@code @Bean} methods, {@link Import @Import} annotations, and
 * {@link ComponentScan @ComponentScan} annotations associated with that
 * class will be subject to the conditions.
 *
 * 如果 {@code @Configuration} 类标有 {@code @Conditional}，则与关联的所有 {@code @Bean} 方法、{@link Import @Import} 注解和
 * {@link ComponentScan @ComponentScan} 注解该类将受条件限制。
 *
 * <p><strong>NOTE</strong>: Inheritance of {@code @Conditional} annotations
 * is not supported; any conditions from superclasses or from overridden
 * methods will not be considered. In order to enforce these semantics,
 * {@code @Conditional} itself is not declared as
 * {@link java.lang.annotation.Inherited @Inherited}; furthermore, any
 * custom <em>composed annotation</em> that is meta-annotated with
 * {@code @Conditional} must not be declared as {@code @Inherited}.
 *
 * 注意：不支持继承{@code @Conditional}注解；来自超类或重写方法的任何条件都不会被考虑。为了强制执行这些语义，{@code @Conditional}
 * 本身没有声明为 {@link java.lang.annotation.Inherited @Inherited}；此外，任何使用 {@code @Conditional} 进行注解的自定义组合注释都不得声明为 {@code @Inherited}。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see Condition
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {

	/**
	 * All {@link Condition} classes that must {@linkplain Condition#matches match}
	 * in order for the component to be registered.
	 */
	Class<? extends Condition>[] value();

}
