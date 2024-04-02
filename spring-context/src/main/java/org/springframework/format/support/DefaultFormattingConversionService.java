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

package org.springframework.format.support;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.number.NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.CurrencyUnitFormatter;
import org.springframework.format.number.money.Jsr354NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.MonetaryAmountFormatter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

/**
 * A specialization of {@link FormattingConversionService} configured by default with
 * converters and formatters appropriate for most applications.
 * <p>{@link FormattingConversionService}的一个专门化实现，配置了适用于大多数应用程序的转换器和格式化器。
 *
 * <p>Designed for direct instantiation but also exposes the static {@link #addDefaultFormatters}
 * utility method for ad hoc use against any {@code FormatterRegistry} instance, just
 * as {@code DefaultConversionService} exposes its own
 * {@link DefaultConversionService#addDefaultConverters addDefaultConverters} method.
 * <p>此类旨在直接实例化，但也提供了静态的{@link #addDefaultFormatters}工具方法，可用于任意{@code FormatterRegistry}实例的临时配置，
 * 类似于{@code DefaultConversionService}提供的{@link DefaultConversionService#addDefaultConverters addDefaultConverters}方法。
 *
 * <p>Automatically registers formatters for JSR-354 Money &amp; Currency, JSR-310 Date-Time
 * and/or Joda-Time 2.x, depending on the presence of the corresponding API on the classpath.
 * <p>根据类路径中相应API的存在与否，自动注册JSR-354货币和货币格式化器、JSR-310日期时间或Joda-Time 2.x的格式化器。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public class DefaultFormattingConversionService extends FormattingConversionService {

	private static final boolean jsr354Present;

	private static final boolean jodaTimePresent;

	static {
		ClassLoader classLoader = DefaultFormattingConversionService.class.getClassLoader();
		jsr354Present = ClassUtils.isPresent("javax.money.MonetaryAmount", classLoader);
		jodaTimePresent = ClassUtils.isPresent("org.joda.time.YearMonth", classLoader);
	}

	/**
	 * Create a new {@code DefaultFormattingConversionService} with the set of
	 * {@linkplain DefaultConversionService#addDefaultConverters default converters} and
	 * {@linkplain #addDefaultFormatters default formatters}.
	 */
	public DefaultFormattingConversionService() {
		this(null, true);
	}

	/**
	 * Create a new {@code DefaultFormattingConversionService} with the set of
	 * {@linkplain DefaultConversionService#addDefaultConverters default converters} and,
	 * based on the value of {@code registerDefaultFormatters}, the set of
	 * {@linkplain #addDefaultFormatters default formatters}.
	 * @param registerDefaultFormatters whether to register default formatters
	 */
	public DefaultFormattingConversionService(boolean registerDefaultFormatters) {
		this(null, registerDefaultFormatters);
	}

	/**
	 * Create a new {@code DefaultFormattingConversionService} with the set of
	 * {@linkplain DefaultConversionService#addDefaultConverters default converters} and,
	 * based on the value of {@code registerDefaultFormatters}, the set of
	 * {@linkplain #addDefaultFormatters default formatters}.
	 * <p>构建一个新的{@code DefaultFormattingConversionService}实例，该实例包含一组
	 * {@linkplain DefaultConversionService#addDefaultConverters 默认转换器}，并且基于
	 * {@code registerDefaultFormatters}的值，包含一组
	 * {@linkplain #addDefaultFormatters 默认格式化器}。
	 *
	 * @param embeddedValueResolver delegated to {@link #setEmbeddedValueResolver(StringValueResolver)}
	 * prior to calling {@link #addDefaultFormatters}.
	 * <p>用于在调用{@link #addDefaultFormatters}之前委托给
	 * {@link #setEmbeddedValueResolver(StringValueResolver)}的嵌入值解析器。
	 * @param registerDefaultFormatters whether to register default formatters
	 *                                  指定是否注册默认格式化器。
	 */
	public DefaultFormattingConversionService(
			@Nullable StringValueResolver embeddedValueResolver, boolean registerDefaultFormatters) {

		if (embeddedValueResolver != null) {
			setEmbeddedValueResolver(embeddedValueResolver);
		}
		// 注册默认的Converter
		DefaultConversionService.addDefaultConverters(this);
		if (registerDefaultFormatters) {
			// 添加格式化器
			addDefaultFormatters(this);
		}
	}


	/**
	 * Add formatters appropriate for most environments: including number formatters,
	 * JSR-354 Money &amp; Currency formatters, JSR-310 Date-Time and/or Joda-Time formatters,
	 * depending on the presence of the corresponding API on the classpath.
	 * <p>向指定的格式化器注册表中添加适用于大多数环境的格式化器：包括数字格式化器、
	 * JSR-354货币和货币格式化器、JSR-310日期时间和/or Joda-Time格式化器，
	 * 这取决于相应API是否在类路径上存在。
	 * @param formatterRegistry the service to register default formatters with
	 *                          用于注册默认格式化器的服务
	 */
	@SuppressWarnings("deprecation")
	public static void addDefaultFormatters(FormatterRegistry formatterRegistry) {
		// Default handling of number values
		// 为数字值添加默认处理
		formatterRegistry.addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());

		// Default handling of monetary values
		// 为货币值添加默认处理
		if (jsr354Present) {
			formatterRegistry.addFormatter(new CurrencyUnitFormatter());
			formatterRegistry.addFormatter(new MonetaryAmountFormatter());
			formatterRegistry.addFormatterForFieldAnnotation(new Jsr354NumberFormatAnnotationFormatterFactory());
		}

		// Default handling of date-time values
		// 为日期时间值添加默认处理

		// just handling JSR-310 specific date and time types
		// 仅处理JSR-310特定的日期和时间类型
		new DateTimeFormatterRegistrar().registerFormatters(formatterRegistry);

		if (jodaTimePresent) {
			// handles Joda-specific types as well as Date, Calendar, Long
			new org.springframework.format.datetime.joda.JodaTimeFormatterRegistrar().registerFormatters(formatterRegistry);
		}
		else {
			// regular DateFormat-based Date, Calendar, Long converters
			new DateFormatterRegistrar().registerFormatters(formatterRegistry);
		}
	}

}
