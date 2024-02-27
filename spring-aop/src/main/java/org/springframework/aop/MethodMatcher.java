/*
 * Copyright 2002-2019 the original author or authors.
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
 * Part of a {@link Pointcut}: Checks whether the target method is eligible for advice.
 *
 * {@link Pointcut} 的一部分：检查目标方法是否有资格获得advice。
 *
 * <p>A MethodMatcher may be evaluated <b>statically</b> or at <b>runtime</b> (dynamically).
 * Static matching involves method and (possibly) method attributes. Dynamic matching
 * also makes arguments for a particular call available, and any effects of running
 * previous advice applying to the joinpoint.
 *
 * MethodMatcher 可以静态计算，也可以在运行时（动态）计算。静态匹配涉及方法和（可能）方法属性。动态匹配还使特定调用的参数可用，并且运行先前advice的任何效果都应用于联接点。
 *
 * <p>If an implementation returns {@code false} from its {@link #isRuntime()}
 * method, evaluation can be performed statically, and the result will be the same
 * for all invocations of this method, whatever their arguments. This means that
 * if the {@link #isRuntime()} method returns {@code false}, the 3-arg
 * {@link #matches(java.lang.reflect.Method, Class, Object[])} method will never be invoked.
 *
 * 如果实现从其 {@link #isRuntime()} 方法返回 {@code false}，则可以静态执行计算，并且无论其参数如何，此方法的所有调用的结果都是相同的。
 * 这意味着，如果 {@link #isRuntime()} 方法返回 {@code false}，则永远不会调用 3-arg {@link #matches(java.lang.reflect.Method, Class, Object[])} 方法。
 *
 * <p>If an implementation returns {@code true} from its 2-arg
 * {@link #matches(java.lang.reflect.Method, Class)} method and its {@link #isRuntime()} method
 * returns {@code true}, the 3-arg {@link #matches(java.lang.reflect.Method, Class, Object[])}
 * method will be invoked <i>immediately before each potential execution of the related advice</i>,
 * to decide whether the advice should run. All previous advice, such as earlier interceptors
 * in an interceptor chain, will have run, so any state changes they have produced in
 * parameters or ThreadLocal state will be available at the time of evaluation.
 *
 * 如果实现从其 2-arg {@link #matches(java.lang.reflect.Method, Class)} 方法返回 {@code true}，
 * 并且其 {@link #isRuntime())} 方法返回 {@code true}，则在每次可能执行相关advice之前，将立即调用 3-arg {@link #matches(java.lang.reflect.Method, Class, Object[])} 方法，
 * 以决定是否应运行该advice。所有以前的advice（例如拦截器链中的早期拦截器）都将运行，因此它们在参数或 ThreadLocal 状态中产生的任何状态更改都将在评估时可用。
 *
 * <p>Concrete implementations of this interface typically should provide proper
 * implementations of {@link Object#equals(Object)} and {@link Object#hashCode()}
 * in order to allow the matcher to be used in caching scenarios &mdash; for
 * example, in proxies generated by CGLIB.
 *
 * 此接口的具体实现通常应提供 {@link Object#equals(Object)} 和 {@link Object#hashCode()} 的正确实现，以便允许在缓存方案中使用匹配器，例如，在 CGLIB 生成的代理中。
 *
 * @author Rod Johnson
 * @since 11.11.2003
 * @see Pointcut
 * @see ClassFilter
 */
public interface MethodMatcher {

	/**
	 * Perform static checking whether the given method matches.
	 * <p>If this returns {@code false} or if the {@link #isRuntime()}
	 * method returns {@code false}, no runtime check (i.e. no
	 * {@link #matches(java.lang.reflect.Method, Class, Object[])} call)
	 * will be made.
	 *
	 * 执行静态检查给定方法是否匹配。
	 * 如果返回 {@code false} 或 {@link #isRuntime()} 方法返回 {@code false}，则不进行运行时检查（即没有 {@link #matches(java.lang.reflect.Method, Class, Object[]) 调用} 。
	 *
	 * @param method the candidate method -- 目标方法
	 * @param targetClass the target class -- 目标对象类型
	 * @return whether this method matches statically
	 */
	boolean matches(Method method, Class<?> targetClass);

	/**
	 * Is this MethodMatcher dynamic, that is, must a final call be made on the
	 * {@link #matches(java.lang.reflect.Method, Class, Object[])} method at
	 * runtime even if the 2-arg matches method returns {@code true}?
	 * <p>Can be invoked when an AOP proxy is created, and need not be invoked
	 * again before each method invocation,
	 *
	 * 这个 MethodMatcher 是动态的吗？也就是说，必须在运行时对 {@link #matches(java.lang.reflect.Method, Class, Object[])} 方法进行最终调用，
	 * 即使 2-arg matches 方法返回 true？
	 * 可以在创建AOP代理时调用，不需要在每次方法调用前再次调用，
	 * --
	 * 是否是动态匹配，即是否每次执行目标方法的时候都去验证一下
	 *
	 * @return whether a runtime match via the 3-arg
	 * {@link #matches(java.lang.reflect.Method, Class, Object[])} method
	 * is required if static matching passed
	 */
	boolean isRuntime();

	/**
	 * Check whether there a runtime (dynamic) match for this method,
	 * which must have matched statically.
	 * <p>This method is invoked only if the 2-arg matches method returns
	 * {@code true} for the given method and target class, and if the
	 * {@link #isRuntime()} method returns {@code true}. Invoked
	 * immediately before potential running of the advice, after any
	 * advice earlier in the advice chain has run.
	 *
	 * 检查此方法是否存在运行时（动态）匹配，该方法必须静态匹配。
	 * 仅当 2-arg matches 方法为给定方法和目标类返回 {@code true}，并且 {@link #isRuntime()} 方法返回 {@code true} 时，才会调用此方法。
	 * 在建议链中较早的任何advice运行之后，在可能运行advice之前立即调用。
	 * --
	 * 动态匹配验证的方法，比第一个matches方法多了一个参数args，这个参数是调用目标方法传入的参数
	 * 1.调用matches(Method method, Class<?> targetClass)方法，验证方法是否匹配
	 * 2.isRuntime方法是否为true，如果为false，则以第一步的结果为准，否则继续向下
	 * 3.调用matches(Method method, Class<?> targetClass, Object... args)方法继续验证，这个方法多了一个参数，可以对目标方法传入的参数进行校验。
	 * 通过上面的过程，大家可以看出来，如果isRuntime为false的时候，只需要对方法名称进行校验，当目标方法调用多次的时候，实际上第一步的验证结果是一样的，
	 * 所以如果isRuntime为false的情况，可以将验证结果放在缓存中，提升效率，而spring内部就是这么做的，isRuntime为false的时候，需要每次都进行校验，效率会低一些，不过对性能的影响基本上可以忽略。
	 *
	 * @param method the candidate method
	 * @param targetClass the target class
	 * @param args arguments to the method
	 * @return whether there's a runtime match
	 * @see MethodMatcher#matches(Method, Class)
	 */
	boolean matches(Method method, Class<?> targetClass, Object... args);


	/**
	 * Canonical instance that matches all methods.
	 * 匹配所有方法，这个内部的2个matches方法任何时候都返回true
	 */
	MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;

}
