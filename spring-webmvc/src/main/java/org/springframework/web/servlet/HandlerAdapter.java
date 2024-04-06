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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * MVC framework SPI, allowing parameterization of the core MVC workflow.
 * MVC框架的SPI（Service Provider Interface），允许对核心MVC工作流进行参数化。
 *
 * <p>Interface that must be implemented for each handler type to handle a request.
 * This interface is used to allow the {@link DispatcherServlet} to be indefinitely
 * extensible. The {@code DispatcherServlet} accesses all installed handlers through
 * this interface, meaning that it does not contain code specific to any handler type.
 * <p>每个处理器类型必须实现此接口，以处理请求。该接口用于使{@link DispatcherServlet}具有无限的可扩展性。
 * {@code DispatcherServlet}通过此接口访问所有已安装的处理器，这意味着它不包含任何特定于处理器类型的代码。
 *
 * <p>Note that a handler can be of type {@code Object}. This is to enable
 * handlers from other frameworks to be integrated with this framework without
 * custom coding, as well as to allow for annotation-driven handler objects that
 * do not obey any specific Java interface.
 * <p>请注意，处理器可以是{@code Object}类型。这是为了启用与其他框架的处理器集成，而无需进行定制编码，
 * 以及启用不遵守任何特定Java接口的注解驱动的处理器对象。
 *
 * <p>This interface is not intended for application developers. It is available
 * to handlers who want to develop their own web workflow.
 * <p>此接口不适用于应用程序开发人员。它可用于想要开发自己的Web工作流的处理器。
 *
 * <p>Note: {@code HandlerAdapter} implementors may implement the {@link
 * org.springframework.core.Ordered} interface to be able to specify a sorting
 * order (and thus a priority) for getting applied by the {@code DispatcherServlet}.
 * Non-Ordered instances get treated as the lowest priority.
 * <p>注意：{@code HandlerAdapter}实现者可以实现{@link org.springframework.core.Ordered}接口，
 * 以便能够指定应用顺序（从而指定优先级）由{@code DispatcherServlet}处理。非Ordered实例被视为最低优先级。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter
 * @see org.springframework.web.servlet.handler.SimpleServletHandlerAdapter
 */
public interface HandlerAdapter {

	/**
	 * Given a handler instance, return whether this {@code HandlerAdapter}
	 * can support it. Typical HandlerAdapters will base the decision on the handler
	 * type. HandlerAdapters will usually only support one handler type each.
	 * <p>A typical implementation:
	 * <p>{@code
	 * return (handler instanceof MyHandler);
	 * }
	 * @param handler the handler object to check
	 * @return whether this object can use the given handler
	 */
	boolean supports(Object handler);

	/**
	 * Use the given handler to handle this request.
	 * The workflow that is required may vary widely.
	 * 使用给定的处理器处理此请求。
	 * 所需的工作流程可能会有很大差异。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the handler to use. This object must have previously been passed
	 * to the {@code supports} method of this interface, which must have
	 * returned {@code true}.
	 *                要使用的处理器。此对象必须先前已被传递给此接口的{@code supports}方法，并且必须返回{@code true}。
	 * @return a ModelAndView object with the name of the view and the required
	 * model data, or {@code null} if the request has been handled directly
	 * 一个ModelAndView对象，包含视图名称和所需模型数据，或者如果请求已直接处理，则返回{@code null}
	 * @throws Exception in case of errors
	 */
	@Nullable
	ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;

	/**
	 * Same contract as for HttpServlet's {@code getLastModified} method.
	 * Can simply return -1 if there's no support in the handler class.
	 * 此方法遵循HttpServlet的{@code getLastModified}方法的相同约定。
	 * 如果处理器类不支持，可以直接返回-1。
	 * @param request current HTTP request
	 * @param handler the handler to use
	 * @return the lastModified value for the given handler
	 * 定处理器的lastModified值
	 * @deprecated as of 5.3.9 along with
	 * {@link org.springframework.web.servlet.mvc.LastModified}.
	 */
	@Deprecated
	long getLastModified(HttpServletRequest request, Object handler);

}
