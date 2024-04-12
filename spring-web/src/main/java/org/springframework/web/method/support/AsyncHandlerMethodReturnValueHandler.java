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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;

/**
 * A return value handler that supports async types. Such return value types
 * need to be handled with priority so the async value can be "unwrapped".
 * 支持异步类型的返回值处理器。此类返回值类型需要优先处理，以便可以“解包”异步值。
 *
 * <p><strong>Note: </strong> implementing this contract is not required but it
 * should be implemented when the handler needs to be prioritized ahead of others.
 * For example custom (async) handlers, by default ordered after built-in
 * handlers, should take precedence over {@code @ResponseBody} or
 * {@code @ModelAttribute} handling, which should occur once the async value is
 * ready. By contrast, built-in (async) handlers are already ordered ahead of
 * sync handlers.
 * <p><strong>注意： </strong>实现此约定不是必需的，但当处理器需要优先于其他处理器时应实现。
 * 例如，自定义（异步）处理器，默认在内置处理器之后排序，应该优先于{@code @ResponseBody}或
 * {@code @ModelAttribute}处理，这应该在异步值准备好后发生。相比之下，内置（异步）处理器已经排在
 * 同步处理器之前。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface AsyncHandlerMethodReturnValueHandler extends HandlerMethodReturnValueHandler {

	/**
	 * Whether the given return value represents asynchronous computation.
	 * 判断给定的返回值是否表示异步计算。
	 * @param returnValue the value returned from the handler method
	 * @param returnType the return type
	 * @return {@code true} if the return value type represents an async value
	 */
	boolean isAsyncReturnValue(@Nullable Object returnValue, MethodParameter returnType);

}
