/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cglib.core;

/**
 * Custom extension of CGLIB's {@link DefaultNamingPolicy}, modifying
 * the tag in generated class names from "ByCGLIB" to "BySpringCGLIB".
 *
 * CGLIB 的 {@link DefaultNamingPolicy} 的自定义扩展，将生成的类名中的标签从“ByCGLIB”修改为“BySpringCGLIB”
 *
 * <p>This is primarily designed to avoid clashes between a regular CGLIB
 * version (used by some other library) and Spring's embedded variant,
 * in case the same class happens to get proxied for different purposes.
 *
 * 这主要是为了避免常规 CGLIB 版本（由其他库使用）和 Spring 的嵌入式变体之间的冲突，以防同一类碰巧出于不同的目的被代理。
 *
 * @author Juergen Hoeller
 * @since 3.2.8
 */
public class SpringNamingPolicy extends DefaultNamingPolicy {

	public static final SpringNamingPolicy INSTANCE = new SpringNamingPolicy();

	@Override
	protected String getTag() {
		return "BySpringCGLIB";
	}

}
