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

package org.springframework.context.annotation;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Utilities for processing {@link Bean}-annotated methods.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
abstract class BeanAnnotationHelper {
	/**
	 * @Bean 标记的方法 -> bean名称
	 */
	private static final Map<Method, String> beanNameCache = new ConcurrentReferenceHashMap<>();
	/**
	 * @Bean 标记的方法 -> proxyMode == true
	 */
	private static final Map<Method, Boolean> scopedProxyCache = new ConcurrentReferenceHashMap<>();


	public static boolean isBeanAnnotated(Method method) {
		return AnnotatedElementUtils.hasAnnotation(method, Bean.class);
	}

	/**
	 * 从@Bean标记的方法中获取bean名称
	 * @param beanMethod
	 * @return
	 */
	public static String determineBeanNameFor(Method beanMethod) {
		String beanName = beanNameCache.get(beanMethod);
		if (beanName == null) {
			// By default, the bean name is the name of the @Bean-annotated method
			// 缺省情况下，Bean 名称是带 @Bean 注解的方法的名称
			beanName = beanMethod.getName();
			// Check to see if the user has explicitly set a custom bean name...
			// 检查用户是否显式设置了自定义 Bean 名称...
			AnnotationAttributes bean =
					AnnotatedElementUtils.findMergedAnnotationAttributes(beanMethod, Bean.class, false, false);
			if (bean != null) {
				String[] names = bean.getStringArray("name");
				if (names.length > 0) {
					// 获取第一个
					beanName = names[0];
				}
			}
			beanNameCache.put(beanMethod, beanName);
		}
		return beanName;
	}

	/**
	 * @Bean 方法是否被@Scope注解修饰 && proxyMode != ScopedProxyMode.NO
	 * @param beanMethod
	 * @return
	 */
	public static boolean isScopedProxy(Method beanMethod) {
		Boolean scopedProxy = scopedProxyCache.get(beanMethod);
		if (scopedProxy == null) {
			AnnotationAttributes scope =
					AnnotatedElementUtils.findMergedAnnotationAttributes(beanMethod, Scope.class, false, false);
			scopedProxy = (scope != null && scope.getEnum("proxyMode") != ScopedProxyMode.NO);
			scopedProxyCache.put(beanMethod, scopedProxy);
		}
		return scopedProxy;
	}

}
