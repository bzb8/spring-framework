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

package org.springframework.core;

/**
 * Extension of the {@link Ordered} interface, expressing a <em>priority</em>
 * ordering: {@code PriorityOrdered} objects are always applied before
 * <em>plain</em> {@link Ordered} objects regardless of their order values.
 *
 * {@link Ordered} 接口的扩展，表示优先级排序：{@code PriorityOrdered} 对象始终在普通{@link Ordered}对象之前应用，无论其顺序值如何。
 *
 * <p>When sorting a set of {@code Ordered} objects, {@code PriorityOrdered}
 * objects and <em>plain</em> {@code Ordered} objects are effectively treated as
 * two separate subsets, with the set of {@code PriorityOrdered} objects preceding
 * the set of <em>plain</em> {@code Ordered} objects and with relative
 * ordering applied within those subsets.
 *
 * 对一组{@code Ordered}对象进行排序时，{@code PriorityOrdered}对象和普通的{@code Ordered}对象实际上被视为两个单独的子集，
 * 其中一组{@code PriorityOrdered}对象位于一组普通的{@code Ordered}对象之前，并在这些子集中应用相对排序
 *
 * <p>This is primarily a special-purpose interface, used within the framework
 * itself for objects where it is particularly important to recognize
 * <em>prioritized</em> objects first, potentially without even obtaining the
 * remaining objects. A typical example: prioritized post-processors in a Spring
 * {@link org.springframework.context.ApplicationContext}.
 *
 * 这主要是一个特殊用途的接口，在框架本身内用于对象，其中首先识别prioritized对象尤其重要，甚至可能不需要获取剩余的对象。
 * 一个典型的例子：Spring {@link org.springframework.context.ApplicationContext} 中的prioritized post-processors
 *
 * <p>Note: {@code PriorityOrdered} post-processor beans are initialized in
 * a special phase, ahead of other post-processor beans. This subtly
 * affects their autowiring behavior: they will only be autowired against
 * beans which do not require eager initialization for type matching.
 *
 * 注意：{@code PriorityOrdered} 后处理器 bean 在一个特殊阶段初始化，
 * 先于其他后处理器 bean。这巧妙地影响了它们的自动装配行为：它们只会针对不需要急于初始化来进行类型匹配的 bean 进行自动装配。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.beans.factory.config.PropertyOverrideConfigurer
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 */
public interface PriorityOrdered extends Ordered {
}
