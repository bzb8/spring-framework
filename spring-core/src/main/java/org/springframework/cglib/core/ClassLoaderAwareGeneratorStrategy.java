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

package org.springframework.cglib.core;

/**
 * CGLIB GeneratorStrategy variant which exposes the application ClassLoader
 * as current thread context ClassLoader for the time of class generation.
 * The ASM ClassWriter in Spring's ASM variant will pick it up when doing
 * common superclass resolution.
 *
 * CGLIB GeneratorStrategy 变体，它将应用程序 ClassLoader 公开为类生成时的当前线程上下文 ClassLoader。Spring 的 ASM 变体中的 ASM ClassWriter 在执行公共超类解析时会拾取它。
 *
 * @author Juergen Hoeller
 * @since 5.2
 */
public class ClassLoaderAwareGeneratorStrategy extends DefaultGeneratorStrategy {

	private final ClassLoader classLoader;

	public ClassLoaderAwareGeneratorStrategy(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public byte[] generate(ClassGenerator cg) throws Exception {
		if (this.classLoader == null) {
			return super.generate(cg);
		}

		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader;
		try {
			threadContextClassLoader = currentThread.getContextClassLoader();
		}
		catch (Throwable ex) {
			// Cannot access thread context ClassLoader - falling back...
			return super.generate(cg);
		}

		boolean overrideClassLoader = !this.classLoader.equals(threadContextClassLoader);
		if (overrideClassLoader) {
			currentThread.setContextClassLoader(this.classLoader);
		}
		try {
			return super.generate(cg);
		}
		finally {
			if (overrideClassLoader) {
				// Reset original thread context ClassLoader.
				currentThread.setContextClassLoader(threadContextClassLoader);
			}
		}
	}

}
