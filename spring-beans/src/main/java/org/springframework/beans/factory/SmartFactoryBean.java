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

package org.springframework.beans.factory;

/**
 * Extension of the {@link FactoryBean} interface. Implementations may
 * indicate whether they always return independent instances, for the
 * case where their {@link #isSingleton()} implementation returning
 * {@code false} does not clearly indicate independent instances.
 *
 * @link FactoryBean} 接口的扩展。实现可以指示它们是否总是返回独立实例，因为它们的 {@link isSingleton()} 实现返回 {@code false} 没有明确指示独立实例。
 *
 * <p>Plain {@link FactoryBean} implementations which do not implement
 * this extended interface are simply assumed to always return independent
 * instances if their {@link #isSingleton()} implementation returns
 * {@code false}; the exposed object is only accessed on demand.
 *
 * <p>不实现此扩展接口的普通 {@link FactoryBean} 实现被简单地假定为始终返回独立实例，如果它们的 {@link isSingleton()} 实现返回 {@code false}；公开的对象只能根据需要访问。
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework and within collaborating frameworks.
 * In general, application-provided FactoryBeans should simply implement
 * the plain {@link FactoryBean} interface. New methods might be added
 * to this extended interface even in point releases.
 *
 * <p><b>注意：<b>该接口是一个特殊用途的接口，主要供框架内部和协作框架内部使用。
 * 一般来说，应用程序提供的 FactoryBeans 应该简单地实现简单的 {@link FactoryBean} 接口。即使在单点版本中，也可能会向此扩展接口添加新方法。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @param <T> the bean type
 * @see #isPrototype()
 * @see #isSingleton()
 */
public interface SmartFactoryBean<T> extends FactoryBean<T> {

	/**
	 * Is the object managed by this factory a prototype? That is,
	 * will {@link #getObject()} always return an independent instance?
	 * <p>The prototype status of the FactoryBean itself will generally
	 * be provided by the owning {@link BeanFactory}; usually, it has to be
	 * defined as singleton there.
	 * <p>This method is supposed to strictly check for independent instances;
	 * it should not return {@code true} for scoped objects or other
	 * kinds of non-singleton, non-independent objects. For this reason,
	 * this is not simply the inverted form of {@link #isSingleton()}.
	 * <p>The default implementation returns {@code false}.
	 * @return whether the exposed object is a prototype
	 * @see #getObject()
	 * @see #isSingleton()
	 */
	default boolean isPrototype() {
		return false;
	}

	/**
	 * Does this FactoryBean expect eager initialization, that is,
	 * eagerly initialize itself as well as expect eager initialization
	 * of its singleton object (if any)?
	 * <p>A standard FactoryBean is not expected to initialize eagerly:
	 * Its {@link #getObject()} will only be called for actual access, even
	 * in case of a singleton object. Returning {@code true} from this
	 * method suggests that {@link #getObject()} should be called eagerly,
	 * also applying post-processors eagerly. This may make sense in case
	 * of a {@link #isSingleton() singleton} object, in particular if
	 * post-processors expect to be applied on startup.
	 * <p>The default implementation returns {@code false}.
	 * @return whether eager initialization applies
	 *
	 * 此 FactoryBean 是否期望急切初始化，即急切初始化自身以及期望其单例对象（如果有）急切初始化？
	 * <p>标准 FactoryBean 不应该急切地初始化：它的 {@link getObject()} 只会在实际访问时被调用，即使在单例对象的情况下也是如此。
	 * 从该方法返回 {@code true} 表明应立即调用 {@link getObject()}，同时也应立即应用后处理器。
	 * 这在 {@link isSingleton() singleton} 对象的情况下可能有意义，特别是如果后处理器希望在启动时应用。 <p>默认实现返回 {@code false}。 @return 是否适用急切初始化
	 *
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
	 */
	default boolean isEagerInit() {
		return false;
	}

}
