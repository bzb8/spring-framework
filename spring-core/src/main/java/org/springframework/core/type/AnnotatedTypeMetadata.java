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

package org.springframework.core.type;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.annotation.MergedAnnotationCollectors;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotationSelectors;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * Defines access to the annotations of a specific type ({@link AnnotationMetadata class}
 * or {@link MethodMetadata method}), in a form that does not necessarily require the
 * class-loading.
 *
 * 定义对特定类型（{@link AnnotationMetadata 类} 或 {@link MethodMetadata 方法}）的注解的访问，其形式不一定需要类加载。
 * AnnotatedTypeMetadata 是 Spring Framework 中的一个接口，用于表示类或方法的注解元数据。它提供了一种方便的方式来检查和访问类或方法上的注解信息。
 *
 * 什么叫注解元素(AnnotatedElement)？比如我们常见的Class、Method、Constructor、Parameter等等都属于它的子类都属于注解元素。
 * 简单理解：只要能在上面标注注解都属于这种元素。Spring4.0新增的这个接口提供了对注解统一的、便捷的访问，使用起来更加的方便高效了。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see AnnotationMetadata
 * @see MethodMetadata
 */
public interface AnnotatedTypeMetadata {

	/**
	 * Return annotation details based on the direct annotations of the
	 * underlying element.
	 *
	 * 根据基础元素的直接注解返回注解详细信息。·
	 *
	 * @return merged annotations based on the direct annotations
	 * @since 5.2
	 */
	MergedAnnotations getAnnotations();

	/**
	 * Determine whether the underlying element has an annotation or meta-annotation
	 * of the given type defined.
	 *
	 * 确定基础元素是否定义了给定类型的注解或元注解。
	 *
	 * <p>If this method returns {@code true}, then
	 * {@link #getAnnotationAttributes} will return a non-null Map.
	 *
	 * 如果此方法返回 {@code true}，则 {@link #getAnnotationAttributes} 将返回非 null Map。
	 *
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for 要查找的批注类型的完全限定类名
	 * @return whether a matching annotation is defined 是否定义了匹配的注解
	 */
	default boolean isAnnotated(String annotationName) {
		return getAnnotations().isPresent(annotationName);
	}

	/**
	 * Retrieve the attributes of the annotation of the given type, if any (i.e. if
	 * defined on the underlying element, as direct annotation or meta-annotation),
	 * also taking attribute overrides on composed annotations into account.
	 *
	 * 检索给定类型的注解的属性（如果有）（即，如果在基础元素上定义，则为直接注解或元注解），同时考虑组合注解的属性覆盖。
	 *
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for 要查找的注解类型的完全限定类名
	 * @return a Map of attributes, with the attribute name as key (e.g. "value")
	 * and the defined attribute value as Map value. This return value will be
	 * {@code null} if no matching annotation is defined.
	 *
	 * 属性映射，属性名称为键（例如“值”），定义的属性值为映射值。如果未定义匹配的注解，则此返回值将为 {@code null}。
	 */
	@Nullable
	default Map<String, Object> getAnnotationAttributes(String annotationName) {
		return getAnnotationAttributes(annotationName, false);
	}

	/**
	 * Retrieve the attributes of the annotation of the given type, if any (i.e. if
	 * defined on the underlying element, as direct annotation or meta-annotation),
	 * also taking attribute overrides on composed annotations into account.
	 *
	 * 检索给定类型的注解的属性（如果有）（即，如果在基础元素上定义，则为直接注解或元注解），同时考虑组合注释的属性覆盖。
	 *
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @param classValuesAsString whether to convert class references to String
	 * class names for exposure as values in the returned Map, instead of Class
	 * references which might potentially have to be loaded first 是否将类引用转换为 String 类名，以便在返回的 Map 中公开为值，而不是可能必须首先加载的类引用
	 * @return a Map of attributes, with the attribute name as key (e.g. "value")
	 * and the defined attribute value as Map value. This return value will be
	 * {@code null} if no matching annotation is defined.
	 *
	 * 属性映射，属性名称为键（例如“值”），定义的属性值为映射值。如果未定义匹配的注解，则此返回值将为 {@code null}。
	 */
	@Nullable
	default Map<String, Object> getAnnotationAttributes(String annotationName,
			boolean classValuesAsString) {

		MergedAnnotation<Annotation> annotation = getAnnotations().get(annotationName,
				null, MergedAnnotationSelectors.firstDirectlyDeclared());
		if (!annotation.isPresent()) {
			return null;
		}
		return annotation.asAnnotationAttributes(Adapt.values(classValuesAsString, true));
	}

	/**
	 * Retrieve all attributes of all annotations of the given type, if any (i.e. if
	 * defined on the underlying element, as direct annotation or meta-annotation).
	 * Note that this variant does <i>not</i> take attribute overrides into account.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @return a MultiMap of attributes, with the attribute name as key (e.g. "value")
	 * and a list of the defined attribute values as Map value. This return value will
	 * be {@code null} if no matching annotation is defined.
	 * @see #getAllAnnotationAttributes(String, boolean)
	 */
	@Nullable
	default MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName) {
		return getAllAnnotationAttributes(annotationName, false);
	}

	/**
	 * Retrieve all attributes of all annotations of the given type, if any (i.e. if
	 * defined on the underlying element, as direct annotation or meta-annotation).
	 * Note that this variant does <i>not</i> take attribute overrides into account.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @param classValuesAsString  whether to convert class references to String
	 * @return a MultiMap of attributes, with the attribute name as key (e.g. "value")
	 * and a list of the defined attribute values as Map value. This return value will
	 * be {@code null} if no matching annotation is defined.
	 * @see #getAllAnnotationAttributes(String)
	 */
	@Nullable
	default MultiValueMap<String, Object> getAllAnnotationAttributes(
			String annotationName, boolean classValuesAsString) {

		Adapt[] adaptations = Adapt.values(classValuesAsString, true);
		return getAnnotations().stream(annotationName)
				.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
				.map(MergedAnnotation::withNonMergedAttributes)
				.collect(MergedAnnotationCollectors.toMultiValueMap(map ->
						map.isEmpty() ? null : map, adaptations));
	}

}
