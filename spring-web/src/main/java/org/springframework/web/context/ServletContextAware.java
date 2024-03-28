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

package org.springframework.web.context;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.Aware;

/**
 * Interface to be implemented by any object that wishes to be notified of the
 * {@link ServletContext} (typically determined by the {@link WebApplicationContext})
 * that it runs in.
 * 任何希望在ServletContext（通常由WebApplicationContext确定）发生变化时被通知的对象都必须实现此接口。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 12.03.2004
 * @see ServletConfigAware
 */
public interface ServletContextAware extends Aware {

	/**
	 * Set the {@link ServletContext} that this object runs in.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's {@code afterPropertiesSet} or a
	 * custom init-method. Invoked after ApplicationContextAware's
	 * {@code setApplicationContext}.
	 * 设置此对象运行其中的{@link ServletContext}。
	 * <p>此方法在正常 bean 属性填充后，但在初始化回调如 InitializingBean 的 {@code afterPropertiesSet} 或自定义的 init-method 方法之前被调用。
	 * 它在 ApplicationContextAware 的 {@code setApplicationContext} 方法之后被调用。
	 *
	 * @param servletContext the ServletContext object to be used by this object 将被此对象使用的 ServletContext 对象。
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 */
	void setServletContext(ServletContext servletContext);

}
