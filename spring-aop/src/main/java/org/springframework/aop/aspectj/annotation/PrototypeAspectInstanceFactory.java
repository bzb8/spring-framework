/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;

import org.springframework.beans.factory.BeanFactory;

/**
 * {@link org.springframework.aop.aspectj.AspectInstanceFactory} backed by a
 * {@link BeanFactory}-provided prototype, enforcing prototype semantics.
 *
 * {@link org.springframework.aop.aspectj.AspectInstanceFactory}由{@link BeanFactory}提供的原型支持，强制执行原型语义。
 *
 * <p>Note that this may instantiate multiple times, which probably won't give the
 * semantics you expect. Use a {@link LazySingletonAspectInstanceFactoryDecorator}
 * to wrap this to ensure only one new aspect comes back.
 *
 * 请注意，这可能会实例化多次，这可能不会提供您期望的语义。使用 {@link LazySingletonAspectInstanceFactoryDecorator} 来包装它，以确保只返回一个新aspect。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.BeanFactory
 * @see LazySingletonAspectInstanceFactoryDecorator
 */
@SuppressWarnings("serial")
public class PrototypeAspectInstanceFactory extends BeanFactoryAspectInstanceFactory implements Serializable {

	/**
	 * Create a PrototypeAspectInstanceFactory. AspectJ will be called to
	 * introspect to create AJType metadata using the type returned for the
	 * given bean name from the BeanFactory.
	 * @param beanFactory the BeanFactory to obtain instance(s) from
	 * @param name the name of the bean
	 */
	public PrototypeAspectInstanceFactory(BeanFactory beanFactory, String name) {
		super(beanFactory, name);
		if (!beanFactory.isPrototype(name)) {
			throw new IllegalArgumentException(
					"Cannot use PrototypeAspectInstanceFactory with bean named '" + name + "': not a prototype");
		}
	}

}
