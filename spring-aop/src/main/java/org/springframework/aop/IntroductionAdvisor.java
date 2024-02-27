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
 * Superinterface for advisors that perform one or more AOP <b>introductions</b>.
 *
 * <p>This interface cannot be implemented directly; subinterfaces must
 * provide the advice type implementing the introduction.
 *
 * <p>Introduction is the implementation of additional interfaces
 * (not implemented by a target) via AOP advice.
 * --
 * 一个Java类，没有实现A接口，在不修改Java类的情况下，使其具备A接口的功能。可以通过IntroductionAdvisor给目标类引入更多接口的功能，这个功能是不是非常牛逼。
 *
 * 一个Java类，没有实现A接口，在不修改Java类的情况下，使其具备A接口的功能。可以通过IntroductionAdvisor给目标类引入更多接口的功能，这个功能是不是非常牛逼。
 *
 * @author Rod Johnson
 * @since 04.04.2003
 * @see IntroductionInterceptor
 */
public interface IntroductionAdvisor extends Advisor, IntroductionInfo {

	/**
	 * Return the filter determining which target classes this introduction
	 * should apply to.
	 * <p>This represents the class part of a pointcut. Note that method
	 * matching doesn't make sense to introductions.
	 *
	 * 返回过滤器，确定此introduction应适用于哪些目标类。
	 * 这表示切入点的类部分。请注意，方法匹配对于introduction没有意义。
	 *
	 * @return the class filter
	 */
	ClassFilter getClassFilter();

	/**
	 * Can the advised interfaces be implemented by the introduction advice?
	 * Invoked before adding an IntroductionAdvisor.
	 * @throws IllegalArgumentException if the advised interfaces can't be
	 * implemented by the introduction advice
	 */
	void validateInterfaces() throws IllegalArgumentException;

}
