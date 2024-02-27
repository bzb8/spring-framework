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

package org.springframework.core;

/**
 * Interface to be implemented by decorating proxies, in particular Spring AOP
 * proxies but potentially also custom proxies with decorator semantics.
 *
 * 装饰代理来实现的接口，特别是 Spring AOP 代理，但也可能是具有装饰器语义的自定义代理。
 *
 * <p>Note that this interface should just be implemented if the decorated class
 * is not within the hierarchy of the proxy class to begin with. In particular,
 * a "target-class" proxy such as a Spring AOP CGLIB proxy should not implement
 * it since any lookup on the target class can simply be performed on the proxy
 * class there anyway.
 *
 * 请注意，如果装饰类最初不在代理类的层次结构内，则应仅实现此接口。特别是，“目标类”代理（例如 Spring AOP CGLIB 代理）不应实现它，因为对目标类的任何查找都可以简单地在代理类上执行。
 *
 * <p>Defined in the core module in order to allow
 * {@link org.springframework.core.annotation.AnnotationAwareOrderComparator}
 * (and potential other candidates without spring-aop dependencies) to use it
 * for introspection purposes, in particular annotation lookups.
 *
 * 在核心模块中定义，以允许 {@link org.springframework.core.annotation.AnnotationAwareOrderComparator} （以及没有 spring-aop 依赖项的潜在其他候选者）将其用于内省目的，特别是注释查找。
 *
 * @author Juergen Hoeller
 * @since 4.3
 */
public interface DecoratingProxy {

	/**
	 * Return the (ultimate) decorated class behind this proxy.
	 * <p>In case of an AOP proxy, this will be the ultimate target class,
	 * not just the immediate target (in case of multiple nested proxies).
	 * @return the decorated class (never {@code null}) 装饰类（绝不是 {@code null}）
	 * --
	 * 返回此代理后面的（最终）装饰类。
	 * 对于 AOP 代理，这将是最终目标类，而不仅仅是直接目标（在多个嵌套代理的情况下）。
	 */
	Class<?> getDecoratedClass();

}
