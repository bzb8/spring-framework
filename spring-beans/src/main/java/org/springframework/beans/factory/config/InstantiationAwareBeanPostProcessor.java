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

package org.springframework.beans.factory.config;

import java.beans.PropertyDescriptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.lang.Nullable;

/**
 * Subinterface of {@link BeanPostProcessor} that adds a before-instantiation callback,
 * and a callback after instantiation but before explicit properties are set or
 * autowiring occurs.
 *
 * {@link BeanPostProcessor} 的子接口，添加实例化前回调，以及实例化后但在设置显式属性或发生自动装配之前的回调。
 *
 * <p>Typically used to suppress default instantiation for specific target beans,
 * for example to create proxies with special TargetSources (pooling targets,
 * lazily initializing targets, etc), or to implement additional injection strategies
 * such as field injection.
 *
 * 通常用于抑制特定目标 bean 的默认实例化，例如创建具有特殊 TargetSource 的代理（池目标、延迟初始化目标等），或实现其他注入策略（例如字段注入）。
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. It is recommended to implement the plain
 * {@link BeanPostProcessor} interface as far as possible.
 *
 * 注意：该接口是一个特殊用途的接口，主要供框架内部使用。建议尽可能实现简单的{@link BeanPostProcessor}接口。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 1.2
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#setCustomTargetSourceCreators
 * @see org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator
 */
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * Apply this BeanPostProcessor <i>before the target bean gets instantiated</i>.
	 * The returned bean object may be a proxy to use instead of the target bean,
	 * effectively suppressing default instantiation of the target bean.
	 * <p>If a non-null object is returned by this method, the bean creation process
	 * will be short-circuited. The only further processing applied is the
	 * {@link #postProcessAfterInitialization} callback from the configured
	 * {@link BeanPostProcessor BeanPostProcessors}.
	 * <p>This callback will be applied to bean definitions with their bean class,
	 * as well as to factory-method definitions in which case the returned bean type
	 * will be passed in here.
	 * <p>Post-processors may implement the extended
	 * {@link SmartInstantiationAwareBeanPostProcessor} interface in order
	 * to predict the type of the bean object that they are going to return here.
	 * <p>The default implementation returns {@code null}.
	 *
	 * 在目标bean实例化之前应用此BeanPostProcessor。
	 * 返回的bean对象可以是代理对象，用于替代目标bean，从而有效地阻止目标bean的默认实例化。
	 * 如果此方法返回一个非空对象，那么bean的创建过程将被中断。只会应用来自配置的BeanPostProcessors的postProcessAfterInitialization回调。
	 * 此回调将应用于具有其bean类的bean定义，以及工厂方法定义，其中返回的bean类型将在此处传递。
	 * 后置处理器可以实现扩展的SmartInstantiationAwareBeanPostProcessor接口，以便预测它们将在此处返回的bean对象的类型。
	 * 默认实现返回null。
	 *
	 * @param beanClass the class of the bean to be instantiated
	 * @param beanName the name of the bean
	 * @return the bean object to expose instead of a default instance of the target bean,
	 * or {@code null} to proceed with default instantiation
	 * 要暴露的bean对象，而不是目标bean的默认实例，或者返回null以继续使用默认实例化。
	 *
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see #postProcessAfterInstantiation
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getBeanClass()
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getFactoryMethodName()
	 */
	@Nullable
	default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * Perform operations after the bean has been instantiated, via a constructor or factory method,
	 * but before Spring property population (from explicit properties or autowiring) occurs.
	 * <p>This is the ideal callback for performing custom field injection on the given bean
	 * instance, right before Spring's autowiring kicks in.
	 * <p>The default implementation returns {@code true}.
	 * --
	 * 通过构造函数或工厂方法实例化bean后，但在Spring属性填充（从显式属性或自动装配）发生之前执行操作。
	 * 这是在Spring的自动装配启动之前在给定bean实例上执行自定义字段注入的理想回调。
	 * <p>默认实现返回{@code true}。
	 *
	 * @param bean the bean instance created, with properties not having been set yet -- 已创建的 Bean 实例，其属性尚未设置
	 * @param beanName the name of the bean
	 * @return {@code true} if properties should be set on the bean; {@code false}
	 * if property population should be skipped. Normal implementations should return {@code true}.
	 * Returning {@code false} will also prevent any subsequent InstantiationAwareBeanPostProcessor
	 * instances being invoked on this bean instance.
	 * --
	 * 如果应该在bean上设置属性，则返回 `true` ；如果应该跳过属性设置，则返回 `false` 。正常情况下，实现应该返回 `true` 。
	 * 返回 `false` 也将阻止在此bean实例上调用任何后续的InstantiationAwareBeanPostProcessor实例。
	 *
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see #postProcessBeforeInstantiation
	 */
	default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	/**
	 * Post-process the given property values before the factory applies them
	 * to the given bean, without any need for property descriptors.
	 * <p>Implementations should return {@code null} (the default) if they provide a custom
	 * {@link #postProcessPropertyValues} implementation, and {@code pvs} otherwise.
	 * In a future version of this interface (with {@link #postProcessPropertyValues} removed),
	 * the default implementation will return the given {@code pvs} as-is directly.
	 *
	 * 在工厂将给定的属性值应用于给定的 Bean 之前，对它们进行后处理，而无需任何属性描述符。
	 * 如果实现提供自定义 {@link #postProcessPropertyValues} 实现，则应返回 {@code null}（默认值），否则应返回 {@code pvs}。
	 * 在此接口的未来版本中（删除了 {@link #postProcessPropertyValues}），默认实现将直接按原样返回给定的 {@code pvs}。
	 *
	 * @param pvs the property values that the factory is about to apply (never {@code null})
	 * @param bean the bean instance created, but whose properties have not yet been set
	 * @param beanName the name of the bean
	 * @return the actual property values to apply to the given bean (can be the passed-in
	 * PropertyValues instance), or {@code null} which proceeds with the existing properties
	 * but specifically continues with a call to {@link #postProcessPropertyValues}
	 * (requiring initialized {@code PropertyDescriptor}s for the current bean class)
	 *
	 * 应用于给定 Bean 的实际属性值（可以是传入的 PropertyValues 实例），或 {@code null}，它继续处理现有属性，
	 * 但专门继续调用 {@link #postProcessPropertyValues}（需要初始化当前 Bean 类的 {@code PropertyDescriptor}）
	 *
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @since 5.1
	 * @see #postProcessPropertyValues
	 */
	@Nullable
	default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 * Post-process the given property values before the factory applies them
	 * to the given bean. Allows for checking whether all dependencies have been
	 * satisfied, for example based on a "Required" annotation on bean property setters.
	 * <p>Also allows for replacing the property values to apply, typically through
	 * creating a new MutablePropertyValues instance based on the original PropertyValues,
	 * adding or removing specific values.
	 * <p>The default implementation returns the given {@code pvs} as-is.
	 *
	 * 在工厂将给定的属性值应用于给定的 Bean 之前，对它们进行后处理。允许检查是否满足所有依赖项，例如，基于 Bean 属性设置器上的“必需”注解。
	 * 还允许替换要应用的属性值，通常是通过基于原始 PropertyValues 创建新的 MutablePropertyValues 实例，添加或删除特定值。
	 * 默认实现按原样返回给定的 {@code pvs}。
	 *
	 * @param pvs the property values that the factory is about to apply (never {@code null}) 工厂即将应用的属性值 （从不 {@code null}）
	 * @param pds the relevant property descriptors for the target bean (with ignored
	 * dependency types - which the factory handles specifically - already filtered out) 目标 Bean 的相关属性描述符（忽略依赖类型 - 工厂专门处理 - 已经过滤掉）
	 * @param bean the bean instance created, but whose properties have not yet been set 已创建但尚未设置其属性的 Bean 实例
	 * @param beanName the name of the bean
	 * @return the actual property values to apply to the given bean (can be the passed-in
	 * PropertyValues instance), or {@code null} to skip property population
	 *
	 * 要应用于给定 Bean 的实际属性值（可以是传入的 PropertyValues 实例），或者 {@code null} 跳过属性填充
	 *
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see #postProcessProperties
	 * @see org.springframework.beans.MutablePropertyValues
	 * @deprecated as of 5.1, in favor of {@link #postProcessProperties(PropertyValues, Object, String)}
	 */
	@Deprecated
	@Nullable
	default PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		return pvs;
	}

}
