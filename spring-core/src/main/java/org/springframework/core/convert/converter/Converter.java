/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A converter converts a source object of type {@code S} to a target of type {@code T}.
 *
 * 转换器将 {@code S} 类型的源对象转换为 {@code T} 类型的目标。
 *
 * <p>Implementations of this interface are thread-safe and can be shared.
 *
 * 此接口的实现是线程安全的，可以共享。
 *
 * <p>Implementations may additionally implement {@link ConditionalConverter}.
 *
 * 实现还可以实现 {@link ConditionalConverter}。
 *
 * @author Keith Donald
 * @author Josh Cummings
 * @since 3.0
 * @param <S> the source type
 * @param <T> the target type
 */
@FunctionalInterface
public interface Converter<S, T> {

	/**
	 * Convert the source object of type {@code S} to target type {@code T}. 将类型为 {@code S} 的源对象转换为目标类型 {@code T}。
	 * @param source the source object to convert, which must be an instance of {@code S} (never {@code null}) 要转换的源对象，必须是 {@code S} 的实例（从不 {@code null}）
	 * @return the converted object, which must be an instance of {@code T} (potentially {@code null}) 转换后的对象，必须是 {@code T} 的实例（可能为 {@code null}）
	 * @throws IllegalArgumentException if the source cannot be converted to the desired target type
	 */
	@Nullable
	T convert(S source);

	/**
	 * Construct a composed {@link Converter} that first applies this {@link Converter}
	 * to its input, and then applies the {@code after} {@link Converter} to the
	 * result.
	 * @param after the {@link Converter} to apply after this {@link Converter}
	 * is applied
	 * @param <U> the type of output of both the {@code after} {@link Converter}
	 * and the composed {@link Converter}
	 * @return a composed {@link Converter} that first applies this {@link Converter}
	 * and then applies the {@code after} {@link Converter}
	 * @since 5.3
	 */
	default <U> Converter<S, U> andThen(Converter<? super T, ? extends U> after) {
		Assert.notNull(after, "After Converter must not be null");
		return (S s) -> {
			T initialResult = convert(s);
			return (initialResult != null ? after.convert(initialResult) : null);
		};
	}

}
