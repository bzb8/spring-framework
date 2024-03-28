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

package org.springframework.web.accept;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Strategy that resolves the requested content type from a query parameter.
 * The default query parameter name is {@literal "format"}.
 * <p>从查询参数中解析请求的内容类型的策略。
 * 默认的查询参数名称是{@literal "format"}。
 *
 * <p>You can register static mappings between keys (i.e. the expected value of
 * the query parameter) and MediaType's via {@link #addMapping(String, MediaType)}.
 * As of 5.0 this strategy also supports dynamic lookups of keys via
 * {@link org.springframework.http.MediaTypeFactory#getMediaType}.
 * <p>您可以通过 {@link #addMapping(String, MediaType)} 注册静态映射关系，即将预期的查询参数值与 MediaType 进行映射。
 * 从5.0版本开始，此策略还支持通过 {@link org.springframework.http.MediaTypeFactory#getMediaType} 对键进行动态查找。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ParameterContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

	private String parameterName = "format";


	/**
	 * Create an instance with the given map of file extensions and media types.
	 */
	public ParameterContentNegotiationStrategy(Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
	}


	/**
	 * Set the name of the parameter to use to determine requested media types.
	 * <p>By default this is set to {@code "format"}.
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "'parameterName' is required");
		this.parameterName = parameterName;
	}

	public String getParameterName() {
		return this.parameterName;
	}


	@Override
	@Nullable
	protected String getMediaTypeKey(NativeWebRequest request) {
		return request.getParameter(getParameterName());
	}

}
