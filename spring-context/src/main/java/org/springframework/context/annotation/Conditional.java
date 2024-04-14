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
 * <p>指示组件只有在所有指定的条件{@linkplain #value}匹配时才有资格进行注册。
 *
 * <p>A <em>condition</em> is any state that can be determined programmatically
 * before the bean definition is due to be registered (see {@link Condition} for details).
 * <p><em>条件</em>是指在注册 bean 定义之前（详见 {@link Condition} 详情）可以通过程序确定的任何状态。
 *
 * <p>The {@code @Conditional} annotation may be used in any of the following ways:
 * <p> {@code @Conditional} 注解可以以以下任何一种方式使用：
 *
 * <ul>
 * <li>as a type-level annotation on any class directly or indirectly annotated with
 * {@code @Component}, including {@link Configuration @Configuration} classes</li>
 * <li>as a meta-annotation, for the purpose of composing custom stereotype
 * annotations</li>
 * <li>as a method-level annotation on any {@link Bean @Bean} method</li>
 * </ul>
 * <ul>
 * <li>作为类型级别的注解，用于直接或间接注解有 {@code @Component} 的任何类，包括 {@link Configuration @Configuration} 类</li>
 * <li>作为元注解，用于组合自定义 stereotype 注解</li>
 * <li>作为方法级别的注解，用于任何 {@link Bean @Bean} 方法</li>
 * </ul>
 *
 * <p>If a {@code @Configuration} class is marked with {@code @Conditional},
 * all of the {@code @Bean} methods, {@link Import @Import} annotations, and
 * {@link ComponentScan @ComponentScan} annotations associated with that
 * class will be subject to the conditions.
 * <p>如果一个 {@code @Configuration} 类被标记为 {@code @Conditional}，那么与该类关联的所有 {@code @Bean} 方法、
 * {@link Import @Import} 注解和 {@link ComponentScan @ComponentScan} 注解都将受到这些条件的限制。
 *
 * <p><strong>NOTE</strong>: Inheritance of {@code @Conditional} annotations
 * is not supported; any conditions from superclasses or from overridden
 * methods will not be considered. In order to enforce these semantics,
 * {@code @Conditional} itself is not declared as
 * {@link java.lang.annotation.Inherited @Inherited}; furthermore, any
 * custom <em>composed annotation</em> that is meta-annotated with
 * {@code @Conditional} must not be declared as {@code @Inherited}.
 * <p><strong>注意</strong>：不支持 {@code @Conditional} 注解的继承；来自超类或覆盖方法的任何条件将不被考虑。为了强制这些语义，
 * {@code @Conditional} 本身未声明为 {@link java.lang.annotation.Inherited @Inherited}；此外，任何自定义的 <em>组合注解</em>，
 * 如果元注解为 {@code @Conditional}，则不能声明为 {@code @Inherited}。
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
	 * 所有必须{@linkplain Condition#matches 匹配}的{@link Condition} 类，以使组件能够注册。
	 */
	Class<? extends Condition>[] value();

}
