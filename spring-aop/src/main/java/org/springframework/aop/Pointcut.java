/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop;

/**
 * Core Spring pointcut abstraction.
 * --
 * 核心 Spring 切入点抽象。
 *
 * <p>A pointcut is composed of a {@link ClassFilter} and a {@link MethodMatcher}.
 * Both these basic terms and a Pointcut itself can be combined to build up combinations
 * (e.g. through {@link org.springframework.aop.support.ComposablePointcut}).
 * 切入点由 {@link ClassFilter} 和 {@link MethodMatcher} 组成。
 * 这些基本术语和 Pointcut 本身都可以组合起来构建组合（例如，通过 {@link org.springframework.aop.support.ComposablePointcut}）。
 * --
 * Pointcut只是一种筛选规则
 * PointCut依赖了ClassFilter（类过滤器）和MethodMatcher（方法匹配器）,ClassFilter检查当前筛选规则与目标类是否匹配，
 * MethodMatcher检查当前筛选规则与目标方法是否匹配
 *
 * @author Rod Johnson
 * @see ClassFilter
 * @see MethodMatcher
 * @see org.springframework.aop.support.Pointcuts
 * @see org.springframework.aop.support.ClassFilters
 * @see org.springframework.aop.support.MethodMatchers
 */
public interface Pointcut {

	/**
	 * Return the ClassFilter for this pointcut.
	 * @return the ClassFilter (never {@code null})
	 * 类过滤器, 可以知道哪些类需要拦截
	 */
	ClassFilter getClassFilter();

	/**
	 * Return the MethodMatcher for this pointcut.
	 * @return the MethodMatcher (never {@code null})
	 * 方法匹配器, 可以知道哪些方法需要拦截
	 */
	MethodMatcher getMethodMatcher();


	/**
	 * Canonical Pointcut instance that always matches.
	 * --
	 * 始终匹配的规范 Pointcut 实例。
	 * 匹配所有对象的 Pointcut，内部的2个过滤器默认都会返回true
	 */
	Pointcut TRUE = TruePointcut.INSTANCE;

}
