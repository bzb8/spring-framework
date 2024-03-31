/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.context.i18n;

import java.util.Locale;

import org.springframework.lang.Nullable;

/**
 * Strategy interface for determining the current Locale.
 * 用于确定当前Locale的策略接口。
 * <p>A LocaleContext instance can be associated with a thread
 * via the LocaleContextHolder class.
 * <p>可以通过LocaleContextHolder类将LocaleContext实例与线程关联。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see LocaleContextHolder#getLocale()
 * @see TimeZoneAwareLocaleContext
 */
public interface LocaleContext {

	/**
	 * Return the current Locale, which can be fixed or determined dynamically,
	 * depending on the implementation strategy.
	 * <p>获取当前的Locale对象，这个Locale的获取方式可能取决于具体的实现策略。
	 * 它可能是一个固定的Locale，或者根据某些动态条件确定。
	 *
	 * @return the current Locale, or {@code null} if no specific Locale associated
	 * 当前的Locale对象，如果没有与之特定关联的Locale，则返回null。
	 */
	@Nullable
	Locale getLocale();

}
