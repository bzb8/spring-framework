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

package org.springframework.context;

import org.springframework.beans.factory.Aware;
import org.springframework.core.metrics.ApplicationStartup;

/**
 * Interface to be implemented by any object that wishes to be notified
 * of the {@link ApplicationStartup} that it runs with.
 *
 * 由希望收到运行它的 {@link ApplicationStartup} 通知的任何对象实现的接口。
 *
 * @author Brian Clozel
 * @since 5.3
 * @see ApplicationContextAware
 */
public interface ApplicationStartupAware extends Aware {

	/**
	 * Set the ApplicationStartup that this object runs with.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's afterPropertiesSet or a custom init-method.
	 * Invoked before ApplicationContextAware's setApplicationContext.
	 *
	 * 设置运行此对象时使用的 ApplicationStartup。
	 * 在填充正常 Bean 属性之后但在 init 回调（如 InitializingBean 的 afterPropertiesSet 或自定义 init-method）之前调用。
	 * 在 ApplicationContextAware 的 setApplicationContext 之前调用。
	 *
	 * @param applicationStartup application startup to be used by this object
	 */
	void setApplicationStartup(ApplicationStartup applicationStartup);

}
