/*
 * Copyright 2002-2016 the original author or authors.
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

package org.aopalliance.intercept;

import javax.annotation.Nonnull;

/**
 * This interface represents an invocation in the program.
 * --
 * 该接口代表程序中的一次调用。
 *
 * <p>An invocation is a joinpoint and can be intercepted by an
 * interceptor.
 * --
 * 调用是一个连接点，可以被拦截器拦截。
 *
 * @author Rod Johnson
 */
public interface Invocation extends Joinpoint {

	/**
	 * Get the arguments as an array object.
	 * It is possible to change element values within this
	 * array to change the arguments.
	 * --
	 * 获取数组对象形式的参数。可以更改此数组中的元素值来更改参数。
	 * 通常用来获取调用目标方法的参数。
	 *
	 * @return the argument of the invocation
	 */
	@Nonnull
	Object[] getArguments();

}
