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

import java.lang.annotation.Annotation;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Convenient base class for {@link ImportSelector} implementations that select imports
 * based on an {@link AdviceMode} value from an annotation (such as the {@code @Enable*}
 * annotations).
 * <p>{@link ImportSelector} 实现的方便基类，这些实现根据注解（如 {@code @Enable*} 注解）中的 {@link AdviceMode} 值选择导入。
 *
 * @author Chris Beams
 * @since 3.1
 * @param <A> annotation containing {@linkplain #getAdviceModeAttributeName() AdviceMode attribute}
 */
public abstract class AdviceModeImportSelector<A extends Annotation> implements ImportSelector {

	/**
	 * The default advice mode attribute name.
	 */
	public static final String DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME = "mode";


	/**
	 * The name of the {@link AdviceMode} attribute for the annotation specified by the
	 * generic type {@code A}. The default is {@value #DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME},
	 * but subclasses may override in order to customize.
	 */
	protected String getAdviceModeAttributeName() {
		return DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME;
	}

	/**
	 * This implementation resolves the type of annotation from generic metadata and
	 * validates that (a) the annotation is in fact present on the importing
	 * {@code @Configuration} class and (b) that the given annotation has an
	 * {@linkplain #getAdviceModeAttributeName() advice mode attribute} of type
	 * {@link AdviceMode}.
	 * <p>此实现从泛型元数据解析注解类型，并验证（a）该注解实际上存在于导入的{@code @Configuration}类上，
	 * 以及（b）给定的注解具有类型为{@link AdviceMode}的{@linkplain #getAdviceModeAttributeName() 建议模式属性}。
	 * <p>The {@link #selectImports(AdviceMode)} method is then invoked, allowing the
	 * concrete implementation to choose imports in a safe and convenient fashion.
	 * @throws IllegalArgumentException if expected annotation {@code A} is not present
	 * on the importing {@code @Configuration} class or if {@link #selectImports(AdviceMode)}
	 * returns {@code null}
	 * <p>然后调用{@link #selectImports(AdviceMode)}方法，允许具体实现以安全和方便的方式选择导入内容。
	 * @throws IllegalArgumentException 如果预期注解{@code A}未在导入的{@code @Configuration}类上存在，
	 * 或如果{@link #selectImports(AdviceMode)}返回{@code null}
	 *
	@Override
	 */
	@Override
	public final String[] selectImports(AnnotationMetadata importingClassMetadata) {
		// 获取泛型参数，AdviceModeImportSelector<EnableTransactionManagement>中的EnableTransactionManagement注解
		Class<?> annType = GenericTypeResolver.resolveTypeArgument(getClass(), AdviceModeImportSelector.class);
		Assert.state(annType != null, "Unresolvable type argument for AdviceModeImportSelector");

		// 获取@EnableTransactionManagement注解的属性
		AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
		if (attributes == null) {
			throw new IllegalArgumentException(String.format(
					"@%s is not present on importing class '%s' as expected",
					annType.getSimpleName(), importingClassMetadata.getClassName()));
		}

		// 获取AdviceMode
		AdviceMode adviceMode = attributes.getEnum(getAdviceModeAttributeName());
		// 向容器中注入AutoProxyRegistrar和ProxyTransactionManagementConfiguration
		String[] imports = selectImports(adviceMode);
		if (imports == null) {
			throw new IllegalArgumentException("Unknown AdviceMode: " + adviceMode);
		}
		return imports;
	}

	/**
	 * Determine which classes should be imported based on the given {@code AdviceMode}.
	 * 确定应根据给定的{@code AdviceMode}导入哪些类。
	 * <p>Returning {@code null} from this method indicates that the {@code AdviceMode}
	 * could not be handled or was unknown and that an {@code IllegalArgumentException}
	 * should be thrown.
	 * <p>从此方法返回{@code null}指示无法处理{@code AdviceMode}或未知，因此应抛出{@code IllegalArgumentException}。
	 * @param adviceMode the value of the {@linkplain #getAdviceModeAttributeName()
	 * advice mode attribute} for the annotation specified via generics.
	 * @return array containing classes to import (empty array if none;
	 * {@code null} if the given {@code AdviceMode} is unknown)
	 */
	@Nullable
	protected abstract String[] selectImports(AdviceMode adviceMode);

}
