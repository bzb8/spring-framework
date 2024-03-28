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

package org.springframework.web.accept;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.ServletContextAware;

/**
 * Factory to create a {@code ContentNegotiationManager} and configure it with
 * {@link ContentNegotiationStrategy} instances.
 * <p>用于创建和配置{@code ContentNegotiationManager}的工厂类。
 *
 * <p>This factory offers properties that in turn result in configuring the
 * underlying strategies. The table below shows the property names, their
 * default settings, as well as the strategies that they help to configure:
 * <p>该工厂提供了一系列属性，用于配置底层的{@link ContentNegotiationStrategy}实例。下表显示了属性名称、
 * 它们的默认设置，以及它们帮助配置的策略：
 *
 * <table>
 * <tr>
 * <th>Property Setter 属性设置器</th>
 * <th>Default Value</th>
 * <th>Underlying Strategy</th>
 * <th>Enabled Or Not</th>
 * </tr>
 * <tr>
 * <td>{@link #setFavorParameter favorParameter 喜爱}</td>
 * <td>false</td>
 * <td>{@link ParameterContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #setFavorPathExtension favorPathExtension}</td>
 * <td>false (as of 5.3)</td>
 * <td>{@link PathExtensionContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #setIgnoreAcceptHeader ignoreAcceptHeader}</td>
 * <td>false</td>
 * <td>{@link HeaderContentNegotiationStrategy}</td>
 * <td>Enabled</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentType defaultContentType}</td>
 * <td>null</td>
 * <td>{@link FixedContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentTypeStrategy defaultContentTypeStrategy}</td>
 * <td>null</td>
 * <td>{@link ContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * </table>
 *
 * <p>Alternatively you can avoid use of the above convenience builder
 * methods and set the exact strategies to use via
 * {@link #setStrategies(List)}.
 * <p>或者，您可以避免使用上述方便的构建器方法，而是通过{@link #setStrategies(List)}设置确切要使用的策略。
 *
 * <p><strong>Deprecation Note:</strong> As of 5.2.4,
 * {@link #setFavorPathExtension(boolean) favorPathExtension} and
 * {@link #setIgnoreUnknownPathExtensions(boolean) ignoreUnknownPathExtensions}
 * are deprecated in order to discourage using path extensions for content
 * negotiation and for request mapping with similar deprecations on
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
 * RequestMappingHandlerMapping}. For further context, please read issue
 * <a href="https://github.com/spring-projects/spring-framework/issues/24179">#24719</a>.
 * <p><strong>弃用说明：</strong>从5.2.4开始，由于使用路径扩展进行内容协商和请求映射可能导致问题，
 * 因此{@link #setFavorPathExtension(boolean) favorPathExtension}和{@link #setIgnoreUnknownPathExtensions(boolean) ignoreUnknownPathExtensions}
 * 被弃用。关于这一决策的更多背景信息，请参阅问题<a href="https://github.com/spring-projects/spring-framework/issues/24179">#24719</a>。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.2
 */
public class ContentNegotiationManagerFactoryBean
		implements FactoryBean<ContentNegotiationManager>, ServletContextAware, InitializingBean {

	@Nullable
	private List<ContentNegotiationStrategy> strategies;


	private boolean favorParameter = false;

	private String parameterName = "format";

	private boolean favorPathExtension = false;

	/**
	 * key为小写
	 */
	private Map<String, MediaType> mediaTypes = new HashMap<>();

	private boolean ignoreUnknownPathExtensions = true;

	@Nullable
	private Boolean useRegisteredExtensionsOnly;

	private boolean ignoreAcceptHeader = false;

	@Nullable
	private ContentNegotiationStrategy defaultNegotiationStrategy;

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	@Nullable
	private ServletContext servletContext;


	/**
	 * Set the exact list of strategies to use.
	 * <p><strong>Note:</strong> use of this method is mutually exclusive with
	 * use of all other setters in this class which customize a default, fixed
	 * set of strategies. See class level doc for more details.
	 * @param strategies the strategies to use
	 * @since 5.0
	 */
	public void setStrategies(@Nullable List<ContentNegotiationStrategy> strategies) {
		this.strategies = (strategies != null ? new ArrayList<>(strategies) : null);
	}

	/**
	 * Whether a request parameter ("format" by default) should be used to
	 * determine the requested media type. For this option to work you must
	 * register {@link #setMediaTypes media type mappings}.
	 * <p>By default this is set to {@code false}.
	 * @see #setParameterName
	 */
	public void setFavorParameter(boolean favorParameter) {
		this.favorParameter = favorParameter;
	}

	/**
	 * Set the query parameter name to use when {@link #setFavorParameter} is on.
	 * <p>The default parameter name is {@code "format"}.
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "parameterName is required");
		this.parameterName = parameterName;
	}

	/**
	 * Whether the path extension in the URL path should be used to determine
	 * the requested media type.
	 * <p>By default this is set to {@code false} in which case path extensions
	 * have no impact on content negotiation.
	 * @deprecated as of 5.2.4. See class-level note on the deprecation of path
	 * extension config options. As there is no replacement for this method,
	 * in 5.2.x it is necessary to set it to {@code false}. In 5.3 the default
	 * changes to {@code false} and use of this property becomes unnecessary.
	 */
	@Deprecated
	public void setFavorPathExtension(boolean favorPathExtension) {
		this.favorPathExtension = favorPathExtension;
	}

	/**
	 * Add a mapping from a key to a MediaType where the key are normalized to
	 * lowercase and may have been extracted from a path extension, a filename
	 * extension, or passed as a query parameter.
	 * <p>The {@link #setFavorParameter(boolean) parameter strategy} requires
	 * such mappings in order to work while the {@link #setFavorPathExtension(boolean)
	 * path extension strategy} can fall back on lookups via
	 * {@link ServletContext#getMimeType} and
	 * {@link org.springframework.http.MediaTypeFactory}.
	 * <p><strong>Note:</strong> Mappings registered here may be accessed via
	 * {@link ContentNegotiationManager#getMediaTypeMappings()} and may be used
	 * not only in the parameter and path extension strategies. For example,
	 * with the Spring MVC config, e.g. {@code @EnableWebMvc} or
	 * {@code <mvc:annotation-driven>}, the media type mappings are also plugged
	 * in to:
	 * <ul>
	 * <li>Determine the media type of static resources served with
	 * {@code ResourceHttpRequestHandler}.
	 * <li>Determine the media type of views rendered with
	 * {@code ContentNegotiatingViewResolver}.
	 * <li>List safe extensions for RFD attack detection (check the Spring
	 * Framework reference docs for details).
	 * </ul>
	 * @param mediaTypes media type mappings
	 * @see #addMediaType(String, MediaType)
	 * @see #addMediaTypes(Map)
	 */
	public void setMediaTypes(Properties mediaTypes) {
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			mediaTypes.forEach((key, value) ->
					addMediaType((String) key, MediaType.valueOf((String) value)));
		}
	}

	/**
	 * An alternative to {@link #setMediaTypes} for programmatic registrations.
	 */
	public void addMediaType(String key, MediaType mediaType) {
		this.mediaTypes.put(key.toLowerCase(Locale.ENGLISH), mediaType);
	}

	/**
	 * An alternative to {@link #setMediaTypes} for programmatic registrations.
	 */
	public void addMediaTypes(@Nullable Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			mediaTypes.forEach(this::addMediaType);
		}
	}

	/**
	 * Whether to ignore requests with path extension that cannot be resolved
	 * to any media type. Setting this to {@code false} will result in an
	 * {@code HttpMediaTypeNotAcceptableException} if there is no match.
	 * <p>By default this is set to {@code true}.
	 * @deprecated as of 5.2.4. See class-level note on the deprecation of path
	 * extension config options.
	 */
	@Deprecated
	public void setIgnoreUnknownPathExtensions(boolean ignore) {
		this.ignoreUnknownPathExtensions = ignore;
	}

	/**
	 * Indicate whether to use the Java Activation Framework as a fallback option
	 * to map from file extensions to media types.
	 * @deprecated as of 5.0, in favor of {@link #setUseRegisteredExtensionsOnly(boolean)},
	 * which has reverse behavior.
	 */
	@Deprecated
	public void setUseJaf(boolean useJaf) {
		setUseRegisteredExtensionsOnly(!useJaf);
	}

	/**
	 * When {@link #setFavorPathExtension favorPathExtension} or
	 * {@link #setFavorParameter(boolean)} is set, this property determines
	 * whether to use only registered {@code MediaType} mappings or to allow
	 * dynamic resolution, e.g. via {@link MediaTypeFactory}.
	 * <p>By default this is not set in which case dynamic resolution is on.
	 * <p>设置当{@link #setFavorPathExtension favorPathExtension}或{@link #setFavorParameter(boolean)}被设置时，
	 * 该属性决定是否仅使用注册的{@code MediaType}映射，或者允许动态解析，例如通过{@link MediaTypeFactory}。
	 * <p>默认情况下，此属性未设置，此时允许动态解析。
	 * @param useRegisteredExtensionsOnly 是否仅使用注册的扩展名。如果为true，则仅使用注册的MediaType映射；
	 *                                   如果为false，则允许使用动态解析，例如通过MediaTypeFactory。
	 */
	public void setUseRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
		this.useRegisteredExtensionsOnly = useRegisteredExtensionsOnly;
	}

	private boolean useRegisteredExtensionsOnly() {
		return (this.useRegisteredExtensionsOnly != null && this.useRegisteredExtensionsOnly);
	}

	/**
	 * Whether to disable checking the 'Accept' request header.
	 * <p>By default this value is set to {@code false}.
	 * <p>设置是否禁用检查'Accept'请求头。
	 * <p>默认情况下，此值设置为{@code false}。
	 *
	 * @param ignoreAcceptHeader 如果为{@code true}，则禁用'Accept'请求头的检查；
	 *                           如果为{@code false}，则启用检查（默认行为）。
	 */

	public void setIgnoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.ignoreAcceptHeader = ignoreAcceptHeader;
	}

	/**
	 * Set the default content type to use when no content type is requested.
	 * <p>By default this is not set.
	 * @see #setDefaultContentTypeStrategy
	 */
	public void setDefaultContentType(MediaType contentType) {
		this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentType);
	}

	/**
	 * Set the default content types to use when no content type is requested.
	 * <p>By default this is not set.
	 * @since 5.0
	 * @see #setDefaultContentTypeStrategy
	 */
	public void setDefaultContentTypes(List<MediaType> contentTypes) {
		this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentTypes);
	}

	/**
	 * Set a custom {@link ContentNegotiationStrategy} to use to determine
	 * the content type to use when no content type is requested.
	 * <p>By default this is not set.
	 * @since 4.1.2
	 * @see #setDefaultContentType
	 */
	public void setDefaultContentTypeStrategy(ContentNegotiationStrategy strategy) {
		this.defaultNegotiationStrategy = strategy;
	}

	/**
	 * Invoked by Spring to inject the ServletContext.
	 * 由 Spring 调用以注入 ServletContext。
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	@Override
	public void afterPropertiesSet() {
		build();
	}

	/**
	 * Create and initialize a {@link ContentNegotiationManager} instance.
	 * @since 5.0
	 */
	@SuppressWarnings("deprecation")
	public ContentNegotiationManager build() {
		List<ContentNegotiationStrategy> strategies = new ArrayList<>();

		if (this.strategies != null) {
			strategies.addAll(this.strategies);
		}
		else {
			if (this.favorPathExtension) {
				PathExtensionContentNegotiationStrategy strategy;
				if (this.servletContext != null && !useRegisteredExtensionsOnly()) {
					strategy = new ServletPathExtensionContentNegotiationStrategy(this.servletContext, this.mediaTypes);
				}
				else {
					strategy = new PathExtensionContentNegotiationStrategy(this.mediaTypes);
				}
				strategy.setIgnoreUnknownExtensions(this.ignoreUnknownPathExtensions);
				if (this.useRegisteredExtensionsOnly != null) {
					strategy.setUseRegisteredExtensionsOnly(this.useRegisteredExtensionsOnly);
				}
				strategies.add(strategy);
			}
			if (this.favorParameter) {
				ParameterContentNegotiationStrategy strategy = new ParameterContentNegotiationStrategy(this.mediaTypes);
				strategy.setParameterName(this.parameterName);
				if (this.useRegisteredExtensionsOnly != null) {
					strategy.setUseRegisteredExtensionsOnly(this.useRegisteredExtensionsOnly);
				}
				else {
					strategy.setUseRegisteredExtensionsOnly(true);  // backwards compatibility 向后兼容性
				}
				strategies.add(strategy);
			}
			if (!this.ignoreAcceptHeader) {
				strategies.add(new HeaderContentNegotiationStrategy());
			}
			if (this.defaultNegotiationStrategy != null) {
				strategies.add(this.defaultNegotiationStrategy);
			}
		}

		this.contentNegotiationManager = new ContentNegotiationManager(strategies);

		// Ensure media type mappings are available via ContentNegotiationManager#getMediaTypeMappings()
		// independent of path extension or parameter strategies.
		// 确保媒体类型映射可通过 ContentNegotiationManager#getMediaTypeMappings() 获得，而与路径扩展或参数策略无关。

		if (!CollectionUtils.isEmpty(this.mediaTypes) && !this.favorPathExtension && !this.favorParameter) {
			this.contentNegotiationManager.addFileExtensionResolvers(
					new MappingMediaTypeFileExtensionResolver(this.mediaTypes));
		}

		return this.contentNegotiationManager;
	}


	@Override
	@Nullable
	public ContentNegotiationManager getObject() {
		return this.contentNegotiationManager;
	}

	@Override
	public Class<?> getObjectType() {
		return ContentNegotiationManager.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
