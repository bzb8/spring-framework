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

package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;

/**
 * Interface responsible for creating instances corresponding to a root bean definition.
 * 负责创建与根 Bean 定义对应的实例的接口。
 *
 * <p>This is pulled out into a strategy as various approaches are possible,
 * including using CGLIB to create subclasses on the fly to support Method Injection.
 *
 * 这被拉入一个策略中，因为各种方法都是可能的，包括使用 CGLIB 动态创建子类以支持方法注入。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 */
public interface InstantiationStrategy {

	/**
	 * Return an instance of the bean with the given name in this factory.
	 * @param bd the bean definition
	 * @param beanName the name of the bean when it is created in this context.
	 * The name can be {@code null} if we are autowiring a bean which doesn't
	 * belong to the factory.
	 * @param owner the owning BeanFactory
	 * @return a bean instance for this bean definition
	 * @throws BeansException if the instantiation attempt failed
	 */
	Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner)
			throws BeansException;

	/**
	 * Return an instance of the bean with the given name in this factory,
	 * creating it via the given constructor.
	 * -- 返回此工厂中具有给定名称的bean的实例，通过给定的构造函数创建它。
	 *
	 * @param bd the bean definition -- bean定义
	 * @param beanName the name of the bean when it is created in this context.
	 * The name can be {@code null} if we are autowiring a bean which doesn't
	 * belong to the factory. -- bean在该上下文中创建时的名称。如果我们正在自动装配一个不属于工厂的bean，则该名称可以为{@code null}。
	 * @param owner the owning BeanFactory -- 拥有的BeanFactory
	 * @param ctor the constructor to use -- 要使用的构造函数
	 * @param args the constructor arguments to apply -- 要应用的构造函数参数
	 * @return a bean instance for this bean definition
	 * @throws BeansException if the instantiation attempt failed
	 */
	Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			Constructor<?> ctor, Object... args) throws BeansException;

	/**
	 * Return an instance of the bean with the given name in this factory,
	 * creating it via the given factory method.
	 * 在此工厂中返回具有给定名称的 Bean 实例，通过给定的工厂方法创建它。
	 *
	 * @param bd the bean definition
	 * @param beanName the name of the bean when it is created in this context.
	 * The name can be {@code null} if we are autowiring a bean which doesn't
	 * belong to the factory. 在此上下文中创建 Bean 时的名称。如果我们要自动注入不属于工厂的 bean，则名称可以是 {@code null}。
	 * @param owner the owning BeanFactory 拥有BeanFactory的
	 * @param factoryBean the factory bean instance to call the factory method on,
	 * or {@code null} in case of a static factory method 要调用工厂方法的工厂 Bean 实例，如果是静态工厂方法，则为 {@code null}
	 * @param factoryMethod the factory method to use
	 * @param args the factory method arguments to apply -- 要使用的工厂方法参数
	 * @return a bean instance for this bean definition
	 * @throws BeansException if the instantiation attempt failed
	 */
	Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			@Nullable Object factoryBean, Method factoryMethod, Object... args)
			throws BeansException;

}
