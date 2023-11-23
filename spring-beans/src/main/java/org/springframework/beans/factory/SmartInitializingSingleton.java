/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.beans.factory;

/**
 * Callback interface triggered at the end of the singleton pre-instantiation phase
 * during {@link BeanFactory} bootstrap. This interface can be implemented by
 * singleton beans in order to perform some initialization after the regular
 * singleton instantiation algorithm, avoiding side effects with accidental early
 * initialization (e.g. from {@link ListableBeanFactory#getBeansOfType} calls).
 * In that sense, it is an alternative to {@link InitializingBean} which gets
 * triggered right at the end of a bean's local construction phase.
 *
 * 在 {@link BeanFactory} 引导期间，在单例预实例化阶段结束时触发的回调接口。此接口可以由单例 Bean 实现，以便在常规单例实例化算法之后执行一些初始化，
 * 从而避免意外早期初始化（例如来自 {@link ListableBeanFactory#getBeansOfType} 调用）的副作用。
 * 从这个意义上说，它是 {@link InitializingBean} 的替代方案，后者在 Bean 的本地构建阶段结束时被触发。
 *
 * <p>This callback variant is somewhat similar to
 * {@link org.springframework.context.event.ContextRefreshedEvent} but doesn't
 * require an implementation of {@link org.springframework.context.ApplicationListener},
 * with no need to filter context references across a context hierarchy etc.
 * It also implies a more minimal dependency on just the {@code beans} package
 * and is being honored by standalone {@link ListableBeanFactory} implementations,
 * not just in an {@link org.springframework.context.ApplicationContext} environment.
 *
 * 此回调变体有点类似于{@link org.springframework.context.event.ContextRefreshedEvent}，但不需要{@link org.springframework.context.ApplicationListener}的实现，
 * 也不需要跨上下文层次结构等过滤上下文引用。它还意味着对{@code beans}包的依赖性更小，并且由独立的{@link ListableBeanFactory}实现实现，
 * 而不仅仅是在{@link org.springframework.context.ApplicationContext}环境中
 *
 * <p><b>NOTE:</b> If you intend to start/manage asynchronous tasks, preferably
 * implement {@link org.springframework.context.Lifecycle} instead which offers
 * a richer model for runtime management and allows for phased startup/shutdown.
 *
 * 注意：如果您打算启动管理异步任务，最好实现{@link org.springframework.context.Lifecycle}，它为运行时管理提供了更丰富的模型，并允许分阶段启动关闭。
 *
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
 */
public interface SmartInitializingSingleton {

	/**
	 * Invoked right at the end of the singleton pre-instantiation phase,
	 * with a guarantee that all regular singleton beans have been created
	 * already. {@link ListableBeanFactory#getBeansOfType} calls within
	 * this method won't trigger accidental side effects during bootstrap.
	 * <p><b>NOTE:</b> This callback won't be triggered for singleton beans
	 * lazily initialized on demand after {@link BeanFactory} bootstrap,
	 * and not for any other bean scope either. Carefully use it for beans
	 * with the intended bootstrap semantics only.
	 *
	 * 在单例预实例化阶段结束时调用，保证所有常规单例 bean 都已创建。此方法中的 {@link ListableBeanFactory#getBeansOfType} 调用不会在引导期间触发意外的副作用。
	 * 注意： 对于在 {@link BeanFactory} 引导之后按需延迟初始化的单例 bean，不会触发此回调，也不会为任何其他 bean 作用域触发此回调。仅将其小心地用于具有预期引导语义的 bean。
	 */
	void afterSingletonsInstantiated();

}
