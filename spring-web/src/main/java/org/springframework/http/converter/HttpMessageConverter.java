/*
 * Copyright 2002-2021 the original author or authors.
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
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * Strategy interface for converting from and to HTTP requests and responses.
 * 用于在 HTTP 请求和响应之间进行转换的策略接口。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @param <T> the converted object type
 */
public interface HttpMessageConverter<T> {

	/**
	 * Indicates whether the given class can be read by this converter.
	 * 判断给定的类是否可以通过此转换器读取。
	 * @param clazz the class to test for readability
	 *              要测试读取能力的类
	 * @param mediaType the media type to read (can be {@code null} if not specified);
	 * typically the value of a {@code Content-Type} header.
	 *                  要读取的媒体类型（如果未指定，可以为{@code null}）；通常是{@code Content-Type}头的值。
	 * @return {@code true} if readable; {@code false} otherwise
	 * 如果可读，则返回{@code true}；否则返回{@code false}。
	 */
	boolean canRead(Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * Indicates whether the given class can be written by this converter.
	 * 判断给定的类是否可以通过此转换器写入。
	 * @param clazz the class to test for writability
	 *              要测试写入能力的类
	 * @param mediaType the media type to write (can be {@code null} if not specified);
	 * typically the value of an {@code Accept} header.
	 * @return {@code true} if writable; {@code false} otherwise
	 */
	boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * Return the list of media types supported by this converter. The list may
	 * not apply to every possible target element type and calls to this method
	 * should typically be guarded via {@link #canWrite(Class, MediaType)
	 * canWrite(clazz, null}. The list may also exclude MIME types supported
	 * only for a specific class. Alternatively, use
	 * {@link #getSupportedMediaTypes(Class)} for a more precise list.
	 * 返回此转换器支持的媒体类型列表。该列表可能不适用于每个可能的目标元素类型，
	 * 调用此方法时通常应通过{@link #canWrite(Class, MediaType) canWrite(clazz, null)}进行保护。
	 * 该列表也可能排除仅支持特定类的MIME类型。或者，可以使用
	 * {@link #getSupportedMediaTypes(Class)} 获取更精确的列表。
	 * @return the list of supported media types
	 * 支持的媒体类型列表
	 */
	List<MediaType> getSupportedMediaTypes();

	/**
	 * Return the list of media types supported by this converter for the given
	 * class. The list may differ from {@link #getSupportedMediaTypes()} if the
	 * converter does not support the given Class or if it supports it only for
	 * a subset of media types.
	 * 返回此转换器支持的给定类的媒体类型列表。该列表可能与{@link #getSupportedMediaTypes()}不同，
	 * 如果转换器不支持给定的类，或者仅支持该类的一子集媒体类型。
	 * @param clazz the type of class to check
	 *              要检查的类类型
	 * @return the list of media types supported for the given class
	 * 支持给定类的媒体类型列表
	 * @since 5.3.4
	 */
	default List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
		return (canRead(clazz, null) || canWrite(clazz, null) ?
				getSupportedMediaTypes() : Collections.emptyList());
	}

	/**
	 * Read an object of the given type from the given input message, and returns it.
	 * 从给定的输入消息中读取指定类型的对象，并返回它。
	 * @param clazz the type of object to return. This type must have previously been passed to the
	 * {@link #canRead canRead} method of this interface, which must have returned {@code true}.
	 * 要返回的对象的类型。此类型必须先前已传递给此接口的
	 * {@link #canRead canRead} 方法，且必须返回{@code true}。
	 * @param inputMessage the HTTP input message to read from
	 *                     从中读取的HTTP输入消息
	 * @return the converted object
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotReadableException in case of conversion errors
	 */
	T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * Write a given object to the given output message.
	 * 将给定对象写入给定的输出消息中
	 * @param t the object to write to the output message. The type of this object must have previously been
	 * passed to the {@link #canWrite canWrite} method of this interface, which must have returned {@code true}.
	 * @param contentType the content type to use when writing. May be {@code null} to indicate that the
	 * default content type of the converter must be used. If not {@code null}, this media type must have
	 * previously been passed to the {@link #canWrite canWrite} method of this interface, which must have
	 * returned {@code true}.
	 * @param outputMessage the message to write to
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotWritableException in case of conversion errors
	 */
	void write(T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
