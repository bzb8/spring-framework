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

package org.springframework.context.support;

/**
 * MBean operation interface for the {@link LiveBeansView} feature.
 * {@link LiveBeansView} 功能特性的 MBean 操作接口。
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @deprecated as of 5.3, in favor of using Spring Boot actuators for such needs
 * -- 5.3 开始废弃，赞成使用 Spring Boot 执行器来满足此类需求
 */
@Deprecated
public interface LiveBeansViewMBean {

	/**
	 * Generate a JSON snapshot of current beans and their dependencies.
	 * 生成当前 Bean 及其依赖项的 JSON 快照。
	 */
	String getSnapshotAsJson();

}
