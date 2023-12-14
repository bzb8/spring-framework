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

package org.springframework.context.support;

import java.util.Locale;

import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;

/**
 * Empty {@link MessageSource} that delegates all calls to the parent MessageSource.
 * If no parent is available, it simply won't resolve any message.
 *
 * 空 {@link MessageSource}，将所有调用委托给父 MessageSource。如果没有父级可用，则根本无法解析任何消息。
 *
 * <p>Used as placeholder by AbstractApplicationContext, if the context doesn't
 * define its own MessageSource. Not intended for direct use in applications.
 *
 * 如果上下文未定义自己的 MessageSource，则由 AbstractApplicationContext 用作占位符。不打算在应用中直接使用。
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see AbstractApplicationContext
 */
public class DelegatingMessageSource extends MessageSourceSupport implements HierarchicalMessageSource {

	@Nullable
	private MessageSource parentMessageSource;


	@Override
	public void setParentMessageSource(@Nullable MessageSource parent) {
		this.parentMessageSource = parent;
	}

	@Override
	@Nullable
	public MessageSource getParentMessageSource() {
		return this.parentMessageSource;
	}


	@Override
	@Nullable
	public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
		if (this.parentMessageSource != null) {
			return this.parentMessageSource.getMessage(code, args, defaultMessage, locale);
		}
		else if (defaultMessage != null) {
			return renderDefaultMessage(defaultMessage, args, locale);
		}
		else {
			return null;
		}
	}

	@Override
	public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		if (this.parentMessageSource != null) {
			return this.parentMessageSource.getMessage(code, args, locale);
		}
		else {
			throw new NoSuchMessageException(code, locale);
		}
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		if (this.parentMessageSource != null) {
			return this.parentMessageSource.getMessage(resolvable, locale);
		}
		else {
			if (resolvable.getDefaultMessage() != null) {
				return renderDefaultMessage(resolvable.getDefaultMessage(), resolvable.getArguments(), locale);
			}
			String[] codes = resolvable.getCodes();
			String code = (codes != null && codes.length > 0 ? codes[0] : "");
			throw new NoSuchMessageException(code, locale);
		}
	}


	@Override
	public String toString() {
		return this.parentMessageSource != null ? this.parentMessageSource.toString() : "Empty MessageSource";
	}

}
