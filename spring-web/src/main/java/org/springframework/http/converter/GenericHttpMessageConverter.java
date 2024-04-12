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

package org.springframework.http.converter;

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * A specialization of {@link HttpMessageConverter} that can convert an HTTP request
 * into a target object of a specified generic type and a source object of a specified
 * generic type into an HTTP response.
 * 一个{@link HttpMessageConverter}的特殊化接口，能够将HTTP请求转换为指定泛型类型的目标对象，
 * 以及将指定泛型类型的源对象转换为HTTP响应。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 3.2
 * @param <T> the converted object type
 * @see org.springframework.core.ParameterizedTypeReference
 */
public interface GenericHttpMessageConverter<T> extends HttpMessageConverter<T> {

	/**
	 * Indicates whether the given type can be read by this converter.
	 * This method should perform the same checks as
	 * {@link HttpMessageConverter#canRead(Class, MediaType)} with additional ones
	 * related to the generic type.
	 * 指示给定类型是否可以被此转换器读取。
	 * 该方法应执行与{@link HttpMessageConverter#canRead(Class, MediaType)}相同检查，此外还应涉及泛型类型的相关检查。
	 * @param type the (potentially generic) type to test for readability
	 *             要测试读取能力的（可能为泛型）类型
	 * @param contextClass a context class for the target type, for example a class
	 * in which the target type appears in a method signature (can be {@code null})
	 *                     目标类型的上下文类，例如目标类型出现在方法签名中的类（可以为{@code null}）
	 * @param mediaType the media type to read, can be {@code null} if not specified.
	 * Typically, the value of a {@code Content-Type} header.
	 *                  要读取的媒体类型，可以为{@code null}（如果未指定）。典型地，是{@code Content-Type}头的值。
	 * @return {@code true} if readable; {@code false} otherwise
	 * 如果可读则返回{@code true}，否则返回{@code false}
	 */
	boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType);

	/**
	 * Read an object of the given type form the given input message, and returns it.
	 * 从给定的输入消息中读取指定类型的对象，并返回它。
	 * @param type the (potentially generic) type of object to return. This type must have
	 * previously been passed to the {@link #canRead canRead} method of this interface,
	 * which must have returned {@code true}.
	 *             要返回的（可能为泛型）类型的对象。此类型必须先前已传递给此接口的{@link #canRead canRead}方法，并返回了{@code true}。
	 * @param contextClass a context class for the target type, for example a class
	 * in which the target type appears in a method signature (can be {@code null})
	 *                     目标类型的上下文类，例如目标类型出现在方法签名中的类（可以为{@code null}）
	 * @param inputMessage the HTTP input message to read from
	 *                     HTTP输入消息，从其中读取数据
	 * @return the converted object
	 * 转换后的对象
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotReadableException in case of conversion errors
	 */
	T read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * Indicates whether the given class can be written by this converter.
	 * <p>This method should perform the same checks as
	 * {@link HttpMessageConverter#canWrite(Class, MediaType)} with additional ones
	 * related to the generic type.
	 * 指示给定的类是否可以被此转换器写入。
	 * <p>此方法应执行与{@link HttpMessageConverter#canWrite(Class, MediaType)}相同的检查，此外还应涉及泛型类型的相关检查。
	 * @param type the (potentially generic) type to test for writability
	 * (can be {@code null} if not specified)
	 *             测试写入能力的（可能为泛型）类型（可以为{@code null}，如果未指定）
	 * @param clazz the source object class to test for writability
	 *              要测试写入能力的源对象类
	 * @param mediaType the media type to write (can be {@code null} if not specified);
	 * typically the value of an {@code Accept} header.
	 *                  要写的媒体类型（可以为{@code null}，如果未指定）；典型地，是{@code Accept}头的值。
	 * @return {@code true} if writable; {@code false} otherwise
	 * 如果可写则返回{@code true}，否则返回{@code false}
	 * @since 4.2
	 */
	boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * Write a given object to the given output message.
	 * 将给定对象写入给定的输出消息中
	 * @param t the object to write to the output message. The type of this object must
	 * have previously been passed to the {@link #canWrite canWrite} method of this
	 * interface, which must have returned {@code true}.
	 *          要写入输出消息的对象。此对象的类型必须先前已传递给此接口的{@link #canWrite canWrite}方法，并返回了{@code true}。
	 * @param type the (potentially generic) type of object to write. This type must have
	 * previously been passed to the {@link #canWrite canWrite} method of this interface,
	 * which must have returned {@code true}. Can be {@code null} if not specified.
	 *             要写的（可能为泛型）类型的对象。此类型必须先前已传递给此接口的{@link #canWrite canWrite}方法，并返回了{@code true}。可以为{@code null}（如果未指定）。
	 * @param contentType the content type to use when writing. May be {@code null} to
	 * indicate that the default content type of the converter must be used. If not
	 * {@code null}, this media type must have previously been passed to the
	 * {@link #canWrite canWrite} method of this interface, which must have returned
	 * {@code true}.
	 *                    使用的content type。可以为{@code null}，以指示应使用转换器的默认content type。如果非{@code null}，
	 *                    则此媒体类型必须先前已传递给此接口的{@link #canWrite canWrite}方法，并返回了{@code true}。
	 * @param outputMessage the message to write to
	 *                      要写入的消息
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotWritableException in case of conversion errors
	 * @since 4.2
	 */
	void write(T t, @Nullable Type type, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
