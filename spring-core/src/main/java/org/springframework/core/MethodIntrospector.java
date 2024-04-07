/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Defines the algorithm for searching for metadata-associated methods exhaustively
 * including interfaces and parent classes while also dealing with parameterized methods
 * as well as common scenarios encountered with interface and class-based proxies.
 * <p>用于详尽地搜索与元数据相关联的方法的算法，包括接口和父类，同时处理参数化方法以及接口和类代理常见场景。
 *
 * <p>Typically, but not necessarily, used for finding annotated handler methods.
 * <p>通常（但不限于）用于查找注解处理器方法。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 4.2.3
 */
public final class MethodIntrospector {

	private MethodIntrospector() {
	}


	/**
	 * Select methods on the given target type based on the lookup of associated metadata.
	 * <p>Callers define methods of interest through the {@link MetadataLookup} parameter,
	 * allowing to collect the associated metadata into the result map.
	 * 基于关联元数据的查找，选择目标类型上的方法。
	 * <p>调用者通过 {@link MetadataLookup} 参数定义感兴趣的方法，允许将关联元数据收集到结果映射中。
	 *
	 * @param targetType the target type to search methods on
	 *                   要搜索方法的目标类型
	 * @param metadataLookup a {@link MetadataLookup} callback to inspect methods of interest,
	 * returning non-null metadata to be associated with a given method if there is a match,
	 * or {@code null} for no match
	 *                       一个 {@link MetadataLookup} 回调，用于检查感兴趣的方法，如果匹配，则返回非空元数据与给定方法相关联，或返回 {@code null} 表示没有匹配
	 * @return the selected methods associated with their metadata (in the order of retrieval),
	 * or an empty map in case of no match
	 * 选中的方法及其元数据映射（按检索顺序），如果没有匹配，则返回空映射
	 * 获取目标类型的所有方法，并返回关联元数据，Method -> MetadataLookup的结果
	 */
	public static <T> Map<Method, T> selectMethods(Class<?> targetType, final MetadataLookup<T> metadataLookup) {
		final Map<Method, T> methodMap = new LinkedHashMap<>();
		Set<Class<?>> handlerTypes = new LinkedHashSet<>();
		// 非jdk动态代理的目标类
		Class<?> specificHandlerType = null;

		// 获取代理的目标类型
		if (!Proxy.isProxyClass(targetType)) {
			specificHandlerType = ClassUtils.getUserClass(targetType);
			handlerTypes.add(specificHandlerType);
		}
		// 获取它的所有接口
		handlerTypes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetType));

		// 迭代目标类和它的所有接口
		for (Class<?> currentHandlerType : handlerTypes) {
			final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);

			ReflectionUtils.doWithMethods(currentHandlerType, method -> {
				Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
				T result = metadataLookup.inspect(specificMethod);
				if (result != null) {
					Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
					if (bridgedMethod == specificMethod || bridgedMethod == method ||
							metadataLookup.inspect(bridgedMethod) == null) {
						methodMap.put(specificMethod, result);
					}
				}
			}, ReflectionUtils.USER_DECLARED_METHODS);
		}

		return methodMap;
	}

	/**
	 * Select methods on the given target type based on a filter.
	 * <p>Callers define methods of interest through the {@code MethodFilter} parameter.
	 * 根据过滤器在给定的目标类型上选择方法。
	 * <p>调用者通过{@code MethodFilter}参数定义感兴趣的方法。
	 * @param targetType the target type to search methods on
	 * @param methodFilter a {@code MethodFilter} to help
	 * recognize handler methods of interest
	 *                     一个{@code MethodFilter}，帮助识别感兴趣的手柄方法
	 * @return the selected methods, or an empty set in case of no match
	 * 选择的方法集合，如果没有匹配则返回空集合
	 */
	public static Set<Method> selectMethods(Class<?> targetType, final ReflectionUtils.MethodFilter methodFilter) {
		// 使用方法过滤器来识别目标类型中感兴趣的方法，并返回这些方法的集合
		return selectMethods(targetType,
				(MetadataLookup<Boolean>) method -> (methodFilter.matches(method) ? Boolean.TRUE : null)).keySet();
	}

	/**
	 * Select an invocable method on the target type: either the given method itself
	 * if actually exposed on the target type, or otherwise a corresponding method
	 * on one of the target type's interfaces or on the target type itself.
	 * <p>Matches on user-declared interfaces will be preferred since they are likely
	 * to contain relevant metadata that corresponds to the method on the target class.
	 * @param method the method to check
	 * @param targetType the target type to search methods on
	 * (typically an interface-based JDK proxy)
	 * @return a corresponding invocable method on the target type
	 * @throws IllegalStateException if the given method is not invocable on the given
	 * target type (typically due to a proxy mismatch)
	 */
	public static Method selectInvocableMethod(Method method, Class<?> targetType) {
		if (method.getDeclaringClass().isAssignableFrom(targetType)) {
			return method;
		}
		try {
			String methodName = method.getName();
			Class<?>[] parameterTypes = method.getParameterTypes();
			// 在目标类型接口中查找对应的方法
			for (Class<?> ifc : targetType.getInterfaces()) {
				try {
					return ifc.getMethod(methodName, parameterTypes);
				}
				catch (NoSuchMethodException ex) {
					// Alright, not on this interface then...
				}
			}
			// A final desperate attempt on the proxy class itself...
			return targetType.getMethod(methodName, parameterTypes);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(String.format(
					"Need to invoke method '%s' declared on target class '%s', " +
					"but not found in any interface(s) of the exposed proxy type. " +
					"Either pull the method up to an interface or switch to CGLIB " +
					"proxies by enforcing proxy-target-class mode in your configuration.",
					method.getName(), method.getDeclaringClass().getSimpleName()));
		}
	}


	/**
	 * A callback interface for metadata lookup on a given method.
	 * 用于在给定方法上查找元数据的回调接口。
	 * @param <T> the type of metadata returned
	 */
	@FunctionalInterface
	public interface MetadataLookup<T> {

		/**
		 * Perform a lookup on the given method and return associated metadata, if any.
		 * 对给定的方法进行查找，并返回相关的元数据（如果有）。
		 * @param method the method to inspect
		 *               要检查的方法
		 * @return non-null metadata to be associated with a method if there is a match,
		 * or {@code null} for no match
		 * 如果有匹配，返回与方法相关的非空元数据；如果没有匹配，则返回{@code null}。
		 */
		@Nullable
		T inspect(Method method);
	}

}
