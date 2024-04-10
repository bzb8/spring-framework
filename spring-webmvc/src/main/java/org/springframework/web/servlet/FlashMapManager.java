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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * A strategy interface for retrieving and saving FlashMap instances.
 * See {@link FlashMap} for a general overview of flash attributes.
 * 一个策略接口，用于检索和保存FlashMap实例。
 * 有关FlashMap的概述，请参见{@link FlashMap}。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see FlashMap
 */
public interface FlashMapManager {

	/**
	 * Find a FlashMap saved by a previous request that matches to the current
	 * request, remove it from underlying storage, and also remove other
	 * expired FlashMap instances.
	 * <p>This method is invoked in the beginning of every request in contrast
	 * to {@link #saveOutputFlashMap}, which is invoked only when there are
	 * flash attributes to be saved - i.e. before a redirect.
	 * 检索与当前请求匹配的之前保存的FlashMap，从底层存储中移除它，并删除其他过期的FlashMap实例。
	 * <p>此方法与{@link #saveOutputFlashMap}相反，它会在每个请求的开始时被调用，
	 * 而{@link #saveOutputFlashMap}只在有闪现属性需要保存时被调用——即在重定向之前。
	 *
	 * @param request the current request
	 * @param response the current response
	 * @return a FlashMap matching the current request or {@code null}
	 */
	@Nullable
	FlashMap retrieveAndUpdate(HttpServletRequest request, HttpServletResponse response);

	/**
	 * Save the given FlashMap, in some underlying storage and set the start
	 * of its expiration period.
	 * <p><strong>NOTE:</strong> Invoke this method prior to a redirect in order
	 * to allow saving the FlashMap in the HTTP session or in a response
	 * cookie before the response is committed.
	 * 保存给定的FlashMap对象到某种底层存储，并设置其过期时间的开始点。
	 * <p><strong>注意：</strong>为了能够在响应提交之前将FlashMap保存到HTTP会话或响应cookie中，
	 * 应当在重定向之前调用此方法。
	 * @param flashMap the FlashMap to save
	 * @param request the current request
	 * @param response the current response
	 */
	void saveOutputFlashMap(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response);

}
