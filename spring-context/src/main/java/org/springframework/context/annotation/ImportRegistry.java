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

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

/**
 * Registry of imported class {@link AnnotationMetadata}.
 *
 * 导入类 {@link AnnotationMetadata} 的注册表。
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
interface ImportRegistry {

	@Nullable
	AnnotationMetadata getImportingClassFor(String importedClass);

	void removeImportingClass(String importingClass);

}
