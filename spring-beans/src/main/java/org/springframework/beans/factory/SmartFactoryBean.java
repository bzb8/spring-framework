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
 * FactoryBean接口的扩展。实现可以指示它们是否始终返回独立的实例，
 * 对于它们的{@link #isSingleton()}实现返回false的情况，如果没有明确表明是独立的实例，则可能是这样的。
 *
 * <p>Plain {@link FactoryBean} implementations which do not implement
 * this extended interface are simply assumed to always return independent
 * instances if their {@link #isSingleton()} implementation returns
 * {@code false}; the exposed object is only accessed on demand.
 * 不实现此扩展接口的普通FactoryBean实现被简单地假定在它们的{@link #isSingleton()}实现返回false时始终返回独立的实例；
 * 公开的对象只有在需要时才被访问。
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework and within collaborating frameworks.
 * In general, application-provided FactoryBeans should simply implement
 * the plain {@link FactoryBean} interface. New methods might be added
 * to this extended interface even in point releases.
 *
 *
 *
 * 注意：此接口是一种特殊目的接口，主要用于框架内部和协作框架中。
 * 通常，应用程序提供的FactoryBean应该简单地实现普通的{@link FactoryBean}接口。即使在小版本中添加了新方法，这个扩展接口也可能会被添加。
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
	 * 这个工厂管理的对象是否是原型？也就是说，{@link #getObject()}总是返回一个独立的实例？
	 * FactoryBean本身的原型状态通常由所属的{@link BeanFactory}提供；通常，它需要在那里定义为单例。
	 * 这个方法应该严格检查独立的实例；它不应该返回{@code true}对于作用域对象或其他类型的非单例、非独立对象。因此，这不是简单地反转{@link #isSingleton()}的情况。
	 * 默认情况下，返回false。
	 *
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
	 * 这个FactoryBean是否期望提前初始化，也就是说，它是否也期望提前初始化其单例对象（如果有的话）？
	 * 一个标准的FactoryBean不会期望提前初始化：它的{@link #getObject()}只会在实际访问时被调用，
	 * 即使是单例对象。从这个方法返回true的建议表明，{@link #getObject()}应该提前调用，并且也应该提前应用后处理器。
	 * 这可能在{@link #isSingleton() 单例}对象情况下有意义，特别是在启动时应用后处理器。
	 * 默认情况下，返回false。
	 *
	 * @return 是否适用急切初始化
	 *
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
	 */
	default boolean isEagerInit() {
		return false;
	}

}
