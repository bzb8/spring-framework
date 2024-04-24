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

package org.springframework.beans.factory.support;

import java.util.function.Supplier;

import org.springframework.beans.factory.config.AutowiredPropertyMarker;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Programmatic means of constructing
 * {@link org.springframework.beans.factory.config.BeanDefinition BeanDefinitions}
 * using the builder pattern. Intended primarily for use when implementing Spring 2.0
 * {@link org.springframework.beans.factory.xml.NamespaceHandler NamespaceHandlers}.
 * <p>这是一个用于通过程序方式构建Spring框架的
 * {@link org.springframework.beans.factory.config.BeanDefinition BeanDefinitions}
 * 的类，它使用了建造者模式。这个类主要用途是在实现Spring 2.0
 * {@link org.springframework.beans.factory.xml.NamespaceHandler NamespaceHandlers} 时使用。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public final class BeanDefinitionBuilder {

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link GenericBeanDefinition}.
	 */
	public static BeanDefinitionBuilder genericBeanDefinition() {
		return new BeanDefinitionBuilder(new GenericBeanDefinition());
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link GenericBeanDefinition}.
	 * @param beanClassName the class name for the bean that the definition is being created for
	 */
	public static BeanDefinitionBuilder genericBeanDefinition(String beanClassName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
		builder.beanDefinition.setBeanClassName(beanClassName);
		return builder;
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link GenericBeanDefinition}.
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 */
	public static BeanDefinitionBuilder genericBeanDefinition(Class<?> beanClass) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
		builder.beanDefinition.setBeanClass(beanClass);
		return builder;
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link GenericBeanDefinition}.
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 * @param instanceSupplier a callback for creating an instance of the bean
	 * @since 5.0
	 */
	public static <T> BeanDefinitionBuilder genericBeanDefinition(Class<T> beanClass, Supplier<T> instanceSupplier) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
		builder.beanDefinition.setBeanClass(beanClass);
		builder.beanDefinition.setInstanceSupplier(instanceSupplier);
		return builder;
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
	 * <p>创建一个新的{@code BeanDefinitionBuilder}，用于构建一个{@link RootBeanDefinition}。
	 * 这个方法主要用于创建一个根bean定义，其中bean的类型是通过类名指定的。
	 *
	 * @param beanClassName the class name for the bean that the definition is being created for
	 *                      要为bean创建定义的类名。
	 * @return 返回一个配置了bean类名的{@code BeanDefinitionBuilder}实例，可用于进一步配置bean定义。
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(String beanClassName) {
		return rootBeanDefinition(beanClassName, null);
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
	 * <p>创建一个新的 {@code BeanDefinitionBuilder}，用于构建 {@link RootBeanDefinition}。
	 * 这个方法主要用于配置基于类名和工厂方法名的bean定义。
	 *
	 * @param beanClassName the class name for the bean that the definition is being created for
	 *                      要为其创建定义的bean的类名。
	 * @param factoryMethodName the name of the method to use to construct the bean instance
	 *                          用于构建bean实例的方法名。如果不需要工厂方法，则可以为 {@code null}。
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(String beanClassName, @Nullable String factoryMethodName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new RootBeanDefinition());
		builder.beanDefinition.setBeanClassName(beanClassName);
		builder.beanDefinition.setFactoryMethodName(factoryMethodName);
		return builder;
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass) {
		return rootBeanDefinition(beanClass, (String) null);
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 * @param factoryMethodName the name of the method to use to construct the bean instance
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass, @Nullable String factoryMethodName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new RootBeanDefinition());
		builder.beanDefinition.setBeanClass(beanClass);
		builder.beanDefinition.setFactoryMethodName(factoryMethodName);
		return builder;
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
	 * @param beanType the {@link ResolvableType type} of the bean that the definition is being created for
	 * @param instanceSupplier a callback for creating an instance of the bean
	 * @since 5.3.9
	 */
	public static <T> BeanDefinitionBuilder rootBeanDefinition(ResolvableType beanType, Supplier<T> instanceSupplier) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setTargetType(beanType);
		beanDefinition.setInstanceSupplier(instanceSupplier);
		return new BeanDefinitionBuilder(beanDefinition);
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
	 * <p>创建一个新的 {@code BeanDefinitionBuilder}，用于构建 {@link RootBeanDefinition}。
	 * 这个方法允许通过指定bean的类和一个供应器来创建bean的定义，供应者用于实例化bean。
	 *
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 *                  创建 bean 定义所针对的 bean 的 {@code Class} 对象。
	 * @param instanceSupplier a callback for creating an instance of the bean
	 *                         一个回调，用于创建 bean 的实例。这个供应者应该能够提供 bean 的实例。
	 * @since 5.3.9
	 * @see #rootBeanDefinition(ResolvableType, Supplier)
	 */
	public static <T> BeanDefinitionBuilder rootBeanDefinition(Class<T> beanClass, Supplier<T> instanceSupplier) {
		return rootBeanDefinition(ResolvableType.forClass(beanClass), instanceSupplier);
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link ChildBeanDefinition}.
	 * @param parentName the name of the parent bean
	 */
	public static BeanDefinitionBuilder childBeanDefinition(String parentName) {
		return new BeanDefinitionBuilder(new ChildBeanDefinition(parentName));
	}


	/**
	 * The {@code BeanDefinition} instance we are creating.
	 * 正在创建的bean定义
	 */
	private final AbstractBeanDefinition beanDefinition;

	/**
	 * Our current position with respect to constructor args.
	 */
	private int constructorArgIndex;


	/**
	 * Enforce the use of factory methods.
	 */
	private BeanDefinitionBuilder(AbstractBeanDefinition beanDefinition) {
		this.beanDefinition = beanDefinition;
	}

	/**
	 * Return the current BeanDefinition object in its raw (unvalidated) form.
	 * @see #getBeanDefinition()
	 */
	public AbstractBeanDefinition getRawBeanDefinition() {
		return this.beanDefinition;
	}

	/**
	 * Validate and return the created BeanDefinition object.
	 */
	public AbstractBeanDefinition getBeanDefinition() {
		this.beanDefinition.validate();
		return this.beanDefinition;
	}


	/**
	 * Set the name of the parent definition of this bean definition.
	 */
	public BeanDefinitionBuilder setParentName(String parentName) {
		this.beanDefinition.setParentName(parentName);
		return this;
	}

	/**
	 * Set the name of a static factory method to use for this definition,
	 * to be called on this bean's class.
	 */
	public BeanDefinitionBuilder setFactoryMethod(String factoryMethod) {
		this.beanDefinition.setFactoryMethodName(factoryMethod);
		return this;
	}

	/**
	 * Set the name of a non-static factory method to use for this definition,
	 * including the bean name of the factory instance to call the method on.
	 * @param factoryMethod the name of the factory method
	 * @param factoryBean the name of the bean to call the specified factory method on
	 * @since 4.3.6
	 */
	public BeanDefinitionBuilder setFactoryMethodOnBean(String factoryMethod, String factoryBean) {
		this.beanDefinition.setFactoryMethodName(factoryMethod);
		this.beanDefinition.setFactoryBeanName(factoryBean);
		return this;
	}

	/**
	 * Add an indexed constructor arg value. The current index is tracked internally
	 * and all additions are at the present point.
	 */
	public BeanDefinitionBuilder addConstructorArgValue(@Nullable Object value) {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
				this.constructorArgIndex++, value);
		return this;
	}

	/**
	 * Add a reference to a named bean as a constructor arg.
	 * @see #addConstructorArgValue(Object)
	 */
	public BeanDefinitionBuilder addConstructorArgReference(String beanName) {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
				this.constructorArgIndex++, new RuntimeBeanReference(beanName));
		return this;
	}

	/**
	 * Add the supplied property value under the given property name.
	 */
	public BeanDefinitionBuilder addPropertyValue(String name, @Nullable Object value) {
		this.beanDefinition.getPropertyValues().add(name, value);
		return this;
	}

	/**
	 * Add a reference to the specified bean name under the property specified.
	 * @param name the name of the property to add the reference to
	 * @param beanName the name of the bean being referenced
	 */
	public BeanDefinitionBuilder addPropertyReference(String name, String beanName) {
		this.beanDefinition.getPropertyValues().add(name, new RuntimeBeanReference(beanName));
		return this;
	}

	/**
	 * Add an autowired marker for the specified property on the specified bean.
	 * @param name the name of the property to mark as autowired
	 * @since 5.2
	 * @see AutowiredPropertyMarker
	 */
	public BeanDefinitionBuilder addAutowiredProperty(String name) {
		this.beanDefinition.getPropertyValues().add(name, AutowiredPropertyMarker.INSTANCE);
		return this;
	}

	/**
	 * Set the init method for this definition.
	 */
	public BeanDefinitionBuilder setInitMethodName(@Nullable String methodName) {
		this.beanDefinition.setInitMethodName(methodName);
		return this;
	}

	/**
	 * Set the destroy method for this definition.
	 */
	public BeanDefinitionBuilder setDestroyMethodName(@Nullable String methodName) {
		this.beanDefinition.setDestroyMethodName(methodName);
		return this;
	}


	/**
	 * Set the scope of this definition.
	 * @see org.springframework.beans.factory.config.BeanDefinition#SCOPE_SINGLETON
	 * @see org.springframework.beans.factory.config.BeanDefinition#SCOPE_PROTOTYPE
	 */
	public BeanDefinitionBuilder setScope(@Nullable String scope) {
		this.beanDefinition.setScope(scope);
		return this;
	}

	/**
	 * Set whether this definition is abstract.
	 */
	public BeanDefinitionBuilder setAbstract(boolean flag) {
		this.beanDefinition.setAbstract(flag);
		return this;
	}

	/**
	 * Set whether beans for this definition should be lazily initialized or not.
	 */
	public BeanDefinitionBuilder setLazyInit(boolean lazy) {
		this.beanDefinition.setLazyInit(lazy);
		return this;
	}

	/**
	 * Set the autowire mode for this definition.
	 */
	public BeanDefinitionBuilder setAutowireMode(int autowireMode) {
		this.beanDefinition.setAutowireMode(autowireMode);
		return this;
	}

	/**
	 * Set the dependency check mode for this definition.
	 */
	public BeanDefinitionBuilder setDependencyCheck(int dependencyCheck) {
		this.beanDefinition.setDependencyCheck(dependencyCheck);
		return this;
	}

	/**
	 * Append the specified bean name to the list of beans that this definition
	 * depends on.
	 */
	public BeanDefinitionBuilder addDependsOn(String beanName) {
		if (this.beanDefinition.getDependsOn() == null) {
			this.beanDefinition.setDependsOn(beanName);
		}
		else {
			String[] added = ObjectUtils.addObjectToArray(this.beanDefinition.getDependsOn(), beanName);
			this.beanDefinition.setDependsOn(added);
		}
		return this;
	}

	/**
	 * Set whether this bean is a primary autowire candidate.
	 * @since 5.1.11
	 */
	public BeanDefinitionBuilder setPrimary(boolean primary) {
		this.beanDefinition.setPrimary(primary);
		return this;
	}

	/**
	 * Set the role of this definition.
	 */
	public BeanDefinitionBuilder setRole(int role) {
		this.beanDefinition.setRole(role);
		return this;
	}

	/**
	 * Set whether this bean is 'synthetic', that is, not defined by
	 * the application itself.
	 * @since 5.3.9
	 */
	public BeanDefinitionBuilder setSynthetic(boolean synthetic) {
		this.beanDefinition.setSynthetic(synthetic);
		return this;
	}

	/**
	 * Apply the given customizers to the underlying bean definition.
	 * @since 5.0
	 */
	public BeanDefinitionBuilder applyCustomizers(BeanDefinitionCustomizer... customizers) {
		for (BeanDefinitionCustomizer customizer : customizers) {
			customizer.customize(this.beanDefinition);
		}
		return this;
	}

}
