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

package org.springframework.aop;

import java.lang.reflect.Method;

/**
 * A specialized type of {@link MethodMatcher} that takes into account introductions
 * when matching methods. If there are no introductions on the target class,
 * a method matcher may be able to optimize matching more effectively for example.
 * --
 * 一种特殊类型的 {@link MethodMatcher}，在匹配方法时会考虑introductions。
 * 例如，如果没有关于目标类的introductions，则方法匹配器可能能够更有效地优化匹配。
 *
 * @author Adrian Colyer
 * @since 2.0
 */
public interface IntroductionAwareMethodMatcher extends MethodMatcher {

	/**
	 * Perform static checking whether the given method matches. This may be invoked
	 * instead of the 2-arg {@link #matches(java.lang.reflect.Method, Class)} method
	 * if the caller supports the extended IntroductionAwareMethodMatcher interface.
	 * 执行静态检查，确定给定的方法是否匹配。如果调用方支持扩展的IntroductionAwareMethodMatcher接口，
	 * 则可以调用这个方法，而不是2个参数的matches(java.lang.reflect.Method, Class)方法。
	 *
	 * @param method the candidate method -- 候选方法
	 * @param targetClass the target class -- 目标类
	 * @param hasIntroductions {@code true} if the object on whose behalf we are
	 * asking is the subject on one or more introductions; {@code false} otherwise
	 *                                     -- 如果我们询问的对象是一个或多个介绍的主题，则为true；否则为false
	 * @return whether this method matches statically -- 这个方法是否静态匹配
	 *
	 */
	boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions);

}
