/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Generic converter interface for converting between two or more types.
 * <p>用于两种或多种类型之间转换的通用转换器接口。
 *
 * <p>This is the most flexible of the Converter SPI interfaces, but also the most complex.
 * It is flexible in that a GenericConverter may support converting between multiple source/target
 * type pairs (see {@link #getConvertibleTypes()}). In addition, GenericConverter implementations
 * have access to source/target {@link TypeDescriptor field context} during the type conversion
 * process. This allows for resolving source and target field metadata such as annotations and
 * generics information, which can be used to influence the conversion logic.
 * <p>这是转换器SPI接口中最灵活的一种，但也是最复杂的。它的灵活性体现在一个GenericConverter可以支持多个源/目标类型对之间的转换（见{@link #getConvertibleTypes()}）。
 * 此外，GenericConverter实现可以在类型转换过程中访问源和目标{@link TypeDescriptor 字段上下文}。
 * 这允许解析源和目标字段元数据，如注解和泛型信息，可用于影响转换逻辑。
 *
 * <p>This interface should generally not be used when the simpler {@link Converter} or
 * {@link ConverterFactory} interface is sufficient.
 * 当更简单的{@link Converter}或{@link ConverterFactory}接口足够使用时，一般不建议使用此接口。
 * <p>Implementations may additionally implement {@link ConditionalConverter}.
 * 实现类还可以实现{@link ConditionalConverter}。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see TypeDescriptor
 * @see Converter
 * @see ConverterFactory
 * @see ConditionalConverter
 */
public interface GenericConverter {

	/**
	 * Return the source and target types that this converter can convert between.
	 * <p>Each entry is a convertible source-to-target type pair.
	 * <p>For {@link ConditionalConverter conditional converters} this method may return
	 * {@code null} to indicate all source-to-target pairs should be considered.
	 */
	@Nullable
	Set<ConvertiblePair> getConvertibleTypes();

	/**
	 * Convert the source object to the targetType described by the {@code TypeDescriptor}.
	 * @param source the source object to convert (may be {@code null})
	 * @param sourceType the type descriptor of the field we are converting from
	 * @param targetType the type descriptor of the field we are converting to
	 * @return the converted object
	 */
	@Nullable
	Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType);


	/**
	 * Holder for a source-to-target class pair.
	 * 源到目标类对的持有者。
	 */
	final class ConvertiblePair {

		private final Class<?> sourceType;

		private final Class<?> targetType;

		/**
		 * Create a new source-to-target pair.
		 * @param sourceType the source type
		 * @param targetType the target type
		 */
		public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
			Assert.notNull(sourceType, "Source type must not be null");
			Assert.notNull(targetType, "Target type must not be null");
			this.sourceType = sourceType;
			this.targetType = targetType;
		}

		public Class<?> getSourceType() {
			return this.sourceType;
		}

		public Class<?> getTargetType() {
			return this.targetType;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || other.getClass() != ConvertiblePair.class) {
				return false;
			}
			ConvertiblePair otherPair = (ConvertiblePair) other;
			return (this.sourceType == otherPair.sourceType && this.targetType == otherPair.targetType);
		}

		@Override
		public int hashCode() {
			return (this.sourceType.hashCode() * 31 + this.targetType.hashCode());
		}

		@Override
		public String toString() {
			return (this.sourceType.getName() + " -> " + this.targetType.getName());
		}
	}

}
