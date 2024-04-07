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

package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Iterator;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Provides methods to support various naming and other conventions used
 * throughout the framework. Mainly for internal use within the framework.
 *
 * 提供支持整个框架中使用的各种命名和其他约定的方法。主要供框架内的内部使用。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 2.0
 */
public final class Conventions {

	/**
	 * Suffix added to names when using arrays.
	 */
	private static final String PLURAL_SUFFIX = "List";


	private Conventions() {
	}


	/**
	 * Determine the conventional variable name for the supplied {@code Object}
	 * based on its concrete type. The convention used is to return the
	 * un-capitalized short name of the {@code Class}, according to JavaBeans
	 * property naming rules.
	 * <p>For example:<br>
	 * {@code com.myapp.Product} becomes {@code "product"}<br>
	 * {@code com.myapp.MyProduct} becomes {@code "myProduct"}<br>
	 * {@code com.myapp.UKProduct} becomes {@code "UKProduct"}<br>
	 * <p>For arrays the pluralized version of the array component type is used.
	 * For {@code Collection}s an attempt is made to 'peek ahead' to determine
	 * the component type and return its pluralized version.
	 * @param value the value to generate a variable name for
	 * @return the generated variable name
	 */
	/**
	 * 根据提供的{@code Object}确定其常规变量名，基于其具体类型。使用的方法是返回根据JavaBeans属性命名规则未大写的短类名。
	 * <p>例如：<br>
	 * {@code com.myapp.Product} 变为 {@code "product"}<br>
	 * {@code com.myapp.MyProduct} 变为 {@code "myProduct"}<br>
	 * {@code com.myapp.UKProduct} 变为 {@code "UKProduct"}<br>
	 * <p>对于数组，使用数组组件类型的复数形式。对于{@code Collection}，尝试“向前看”以确定组件类型并返回其复数形式。
	 * @param value 为其生成变量名的值
	 * @return 生成的变量名
	 * @throws IllegalArgumentException 如果提供的Collection为空，因为无法为一个空的Collection生成变量名
	 * @throws NullPointerException 如果提供的值为null，则抛出此异常
	 */
	public static String getVariableName(Object value) {
		Assert.notNull(value, "Value must not be null");
		Class<?> valueClass;
		boolean pluralize = false;

		if (value.getClass().isArray()) {
			valueClass = value.getClass().getComponentType();
			pluralize = true;
		}
		else if (value instanceof Collection) {
			Collection<?> collection = (Collection<?>) value;
			if (collection.isEmpty()) {
				throw new IllegalArgumentException(
						"Cannot generate variable name for an empty Collection");
			}
			Object valueToCheck = peekAhead(collection);
			valueClass = getClassForValue(valueToCheck);
			pluralize = true;
		}
		else {
			valueClass = getClassForValue(value);
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass);
		return (pluralize ? pluralize(name) : name);
	}

	/**
	 * Determine the conventional variable name for the given parameter taking
	 * the generic collection type, if any, into account.
	 * <p>As of 5.0 this method supports reactive types:<br>
	 * {@code Mono<com.myapp.Product>} becomes {@code "productMono"}<br>
	 * {@code Flux<com.myapp.MyProduct>} becomes {@code "myProductFlux"}<br>
	 * {@code Observable<com.myapp.MyProduct>} becomes {@code "myProductObservable"}<br>
	 * @param parameter the method or constructor parameter
	 * @return the generated variable name
	 */
	/**
	 * 根据给定的参数确定传统的变量名称，考虑通用集合类型（如果有的话）。
	 * <p>5.0版本起，本方法支持响应式类型：<br>
	 * {@code Mono<com.myapp.Product>} 变为 {@code "productMono"}<br>
	 * {@code Flux<com.myapp.MyProduct>} 变为 {@code "myProductFlux"}<br>
	 * {@code Observable<com.myapp.MyProduct>} 变为 {@code "myProductObservable"}<br>
	 * @param parameter 方法或构造函数参数
	 * @return 生成的变量名称
	 * @throws IllegalArgumentException 如果参数是非泛型的Collection类型且无法确定其元素类型时抛出
	 */
	public static String getVariableNameForParameter(MethodParameter parameter) {
		Assert.notNull(parameter, "MethodParameter must not be null"); // 确保MethodParameter对象不为null
		Class<?> valueClass;
		boolean pluralize = false; // 默认不使用复数形式
		String reactiveSuffix = ""; // 默认无响应式后缀

		// 处理数组类型参数
		if (parameter.getParameterType().isArray()) {
			valueClass = parameter.getParameterType().getComponentType(); // 获取数组元素类型
			pluralize = true; // 使用复数形式
		}
		// 处理集合类型参数
		else if (Collection.class.isAssignableFrom(parameter.getParameterType())) {
			valueClass = ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric(); // 获取集合元素类型
			if (valueClass == null) {
				throw new IllegalArgumentException(
						"Cannot generate variable name for non-typed Collection parameter type"); // 如果无法确定元素类型，抛出异常
			}
			pluralize = true; // 使用复数形式
		}
		else {
			// 处理非数组和集合类型参数，考虑响应式类型
			valueClass = parameter.getParameterType(); // 直接使用参数类型
			ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(valueClass);
			if (adapter != null && !adapter.getDescriptor().isNoValue()) {
				reactiveSuffix = ClassUtils.getShortName(valueClass); // 获取响应式后缀
				valueClass = parameter.nested().getNestedParameterType(); // 获取真实的非响应式类型
			}
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass); // 将类名转换为属性名
		return (pluralize ? pluralize(name) : name + reactiveSuffix); // 根据是否需要复数形式或包含响应式后缀，返回变量名
	}


	/**
	 * Determine the conventional variable name for the return type of the
	 * given method, taking the generic collection type, if any, into account.
	 * @param method the method to generate a variable name for
	 * @return the generated variable name
	 */
	public static String getVariableNameForReturnType(Method method) {
		return getVariableNameForReturnType(method, method.getReturnType(), null);
	}

	/**
	 * Determine the conventional variable name for the return type of the given
	 * method, taking the generic collection type, if any, into account, falling
	 * back on the given actual return value if the method declaration is not
	 * specific enough, e.g. {@code Object} return type or untyped collection.
	 * @param method the method to generate a variable name for
	 * @param value the return value (may be {@code null} if not available)
	 * @return the generated variable name
	 */
	public static String getVariableNameForReturnType(Method method, @Nullable Object value) {
		return getVariableNameForReturnType(method, method.getReturnType(), value);
	}

	/**
	 * Determine the conventional variable name for the return type of the given
	 * method, taking the generic collection type, if any, into account, falling
	 * back on the given return value if the method declaration is not specific
	 * enough, e.g. {@code Object} return type or untyped collection.
	 * <p>As of 5.0 this method supports reactive types:<br>
	 * {@code Mono<com.myapp.Product>} becomes {@code "productMono"}<br>
	 * {@code Flux<com.myapp.MyProduct>} becomes {@code "myProductFlux"}<br>
	 * {@code Observable<com.myapp.MyProduct>} becomes {@code "myProductObservable"}<br>
	 * @param method the method to generate a variable name for
	 * @param resolvedType the resolved return type of the method
	 * @param value the return value (may be {@code null} if not available)
	 * @return the generated variable name
	 */
	public static String getVariableNameForReturnType(Method method, Class<?> resolvedType, @Nullable Object value) {
		Assert.notNull(method, "Method must not be null");

		if (Object.class == resolvedType) {
			if (value == null) {
				throw new IllegalArgumentException(
						"Cannot generate variable name for an Object return type with null value");
			}
			return getVariableName(value);
		}

		Class<?> valueClass;
		boolean pluralize = false;
		String reactiveSuffix = "";

		if (resolvedType.isArray()) {
			valueClass = resolvedType.getComponentType();
			pluralize = true;
		}
		else if (Collection.class.isAssignableFrom(resolvedType)) {
			valueClass = ResolvableType.forMethodReturnType(method).asCollection().resolveGeneric();
			if (valueClass == null) {
				if (!(value instanceof Collection)) {
					throw new IllegalArgumentException("Cannot generate variable name " +
							"for non-typed Collection return type and a non-Collection value");
				}
				Collection<?> collection = (Collection<?>) value;
				if (collection.isEmpty()) {
					throw new IllegalArgumentException("Cannot generate variable name " +
							"for non-typed Collection return type and an empty Collection value");
				}
				Object valueToCheck = peekAhead(collection);
				valueClass = getClassForValue(valueToCheck);
			}
			pluralize = true;
		}
		else {
			valueClass = resolvedType;
			ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(valueClass);
			if (adapter != null && !adapter.getDescriptor().isNoValue()) {
				reactiveSuffix = ClassUtils.getShortName(valueClass);
				valueClass = ResolvableType.forMethodReturnType(method).getGeneric().toClass();
			}
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass);
		return (pluralize ? pluralize(name) : name + reactiveSuffix);
	}

	/**
	 * Convert {@code String}s in attribute name format (e.g. lowercase, hyphens
	 * separating words) into property name format (camel-case). For example
	 * {@code transaction-manager} becomes {@code "transactionManager"}.
	 */
	public static String attributeNameToPropertyName(String attributeName) {
		Assert.notNull(attributeName, "'attributeName' must not be null");
		if (!attributeName.contains("-")) {
			return attributeName;
		}
		char[] result = new char[attributeName.length() -1]; // not completely accurate but good guess
		int currPos = 0;
		boolean upperCaseNext = false;
		for (int i = 0; i < attributeName.length(); i++ ) {
			char c = attributeName.charAt(i);
			if (c == '-') {
				upperCaseNext = true;
			}
			else if (upperCaseNext) {
				result[currPos++] = Character.toUpperCase(c);
				upperCaseNext = false;
			}
			else {
				result[currPos++] = c;
			}
		}
		return new String(result, 0, currPos);
	}

	/**
	 * Return an attribute name qualified by the given enclosing {@link Class}.
	 * For example the attribute name '{@code foo}' qualified by {@link Class}
	 * '{@code com.myapp.SomeClass}' would be '{@code com.myapp.SomeClass.foo}'
	 * --
	 * 返回由给定的封闭式 {@link Class} 限定的属性名称。
	 * 例如，由 {@link Class} “{@code com.myapp.SomeClass}” 限定的属性名称“{@code foo}”将为“{@code com.myapp.SomeClass.foo}”
	 * 全限定类名.属性名称
	 */
	public static String getQualifiedAttributeName(Class<?> enclosingClass, String attributeName) {
		Assert.notNull(enclosingClass, "'enclosingClass' must not be null");
		Assert.notNull(attributeName, "'attributeName' must not be null");
		return enclosingClass.getName() + '.' + attributeName;
	}


	/**
	 * Determine the class to use for naming a variable containing the given value.
	 * <p>Will return the class of the given value, except when encountering a
	 * JDK proxy, in which case it will determine the 'primary' interface
	 * implemented by that proxy.
	 * @param value the value to check
	 * @return the class to use for naming a variable
	 */
	private static Class<?> getClassForValue(Object value) {
		Class<?> valueClass = value.getClass();
		if (Proxy.isProxyClass(valueClass)) {
			Class<?>[] ifcs = valueClass.getInterfaces();
			for (Class<?> ifc : ifcs) {
				if (!ClassUtils.isJavaLanguageInterface(ifc)) {
					return ifc;
				}
			}
		}
		else if (valueClass.getName().lastIndexOf('$') != -1 && valueClass.getDeclaringClass() == null) {
			// '$' in the class name but no inner class -
			// assuming it's a special subclass (e.g. by OpenJPA)
			valueClass = valueClass.getSuperclass();
		}
		return valueClass;
	}

	/**
	 * Pluralize the given name.
	 */
	private static String pluralize(String name) {
		return name + PLURAL_SUFFIX;
	}

	/**
	 * Retrieve the {@code Class} of an element in the {@code Collection}.
	 * The exact element for which the {@code Class} is retrieved will depend
	 * on the concrete {@code Collection} implementation.
	 */
	private static <E> E peekAhead(Collection<E> collection) {
		Iterator<E> it = collection.iterator();
		if (!it.hasNext()) {
			throw new IllegalStateException(
					"Unable to peek ahead in non-empty collection - no element found");
		}
		E value = it.next();
		if (value == null) {
			throw new IllegalStateException(
					"Unable to peek ahead in non-empty collection - only null element found");
		}
		return value;
	}

}
