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

package org.springframework.core.convert.converter;

import org.springframework.core.convert.TypeDescriptor;

/**
 * Allows a {@link Converter}, {@link GenericConverter} or {@link ConverterFactory} to
 * conditionally execute based on attributes of the {@code source} and {@code target}
 * {@link TypeDescriptor}.
 * <p>允许{@link Converter}、{@link GenericConverter}或{@link ConverterFactory}基于{@code source}和{@code target}的
 * {@link TypeDescriptor}属性有条件地执行。
 *
 * <p>Often used to selectively match custom conversion logic based on the presence of a
 * field or class-level characteristic, such as an annotation or method. For example, when
 * converting from a String field to a Date field, an implementation might return
 * {@code true} if the target field has also been annotated with {@code @DateTimeFormat}.
 * <p>通常用于根据字段或类级别的特征（如注解或方法）选择性地匹配自定义转换逻辑。例如，在将String字段转换为Date字段时，
 * 实现可能会在目标字段也注解了{@code @DateTimeFormat}时返回{@code true}。
 *
 * <p>As another example, when converting from a String field to an {@code Account} field,
 * an implementation might return {@code true} if the target Account class defines a
 * {@code public static findAccount(String)} method.
 * <p>另一个例子是，当将String字段转换为{@code Account}字段时，实现可能会在目标Account类定义了
 * {@code public static findAccount(String)}方法时返回{@code true}。
 *
 * @author Phillip Webb
 * @author Keith Donald
 * @since 3.2
 * @see Converter
 * @see GenericConverter
 * @see ConverterFactory
 * @see ConditionalGenericConverter
 */
public interface ConditionalConverter {

	/**
	 * Should the conversion from {@code sourceType} to {@code targetType} currently under
	 * consideration be selected?
	 * <p>判断当前考虑的从{@code sourceType}到{@code targetType}的转换是否应被选择。
	 *
	 * @param sourceType the type descriptor of the field we are converting from
	 *                   我们正从其转换的字段的类型描述符
	 * @param targetType the type descriptor of the field we are converting to
	 *                   我们正转换到的字段的类型描述符
	 * @return true if conversion should be performed, false otherwise
	 * 如果转换应被执行，则返回true；否则返回false。
	 */
	boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);

}
