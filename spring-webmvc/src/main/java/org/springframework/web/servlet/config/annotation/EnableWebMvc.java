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

package org.springframework.web.servlet.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Adding this annotation to an {@code @Configuration} class imports the Spring MVC
 * configuration from {@link WebMvcConfigurationSupport}, e.g.:
 * <p>将此注解添加到一个{@code @Configuration}类中，可以导入Spring MVC配置，例如：</p>
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
 * public class MyConfiguration {
 * }
 * </pre>
 *
 * <p>To customize the imported configuration, implement the interface
 * {@link WebMvcConfigurer} and override individual methods, e.g.:
 * <p>要自定义导入的配置，实现接口{@link WebMvcConfigurer}并重写特定的方法，例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
 * public class MyConfiguration implements WebMvcConfigurer {
 *
 *     &#064;Override
 *     public void addFormatters(FormatterRegistry formatterRegistry) {
 *         formatterRegistry.addConverter(new MyConverter());
 *     }
 *
 *     &#064;Override
 *     public void configureMessageConverters(List&lt;HttpMessageConverter&lt;?&gt;&gt; converters) {
 *         converters.add(new MyHttpMessageConverter());
 *     }
 *
 * }
 * </pre>
 *
 * <p><strong>Note:</strong> only one {@code @Configuration} class may have the
 * {@code @EnableWebMvc} annotation to import the Spring Web MVC
 * configuration. There can however be multiple {@code @Configuration} classes
 * implementing {@code WebMvcConfigurer} in order to customize the provided
 * configuration.
 * <p><strong>注意：</strong>只有一个{@code @Configuration}类可以有{@code @EnableWebMvc}注解来导入Spring Web MVC配置。
 * 但是，可以有多个{@code @Configuration}类实现{@code WebMvcConfigurer}以自定义提供的配置。
 *
 * <p>If {@link WebMvcConfigurer} does not expose some more advanced setting that
 * needs to be configured, consider removing the {@code @EnableWebMvc}
 * annotation and extending directly from {@link WebMvcConfigurationSupport}
 * or {@link DelegatingWebMvcConfiguration}, e.g.:
 * <p>如果{@link WebMvcConfigurer}不提供需要配置的更高级设置，可以考虑移除{@code @EnableWebMvc}注解，
 * 并直接扩展自{@link WebMvcConfigurationSupport}或{@link DelegatingWebMvcConfiguration}，例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan(basePackageClasses = { MyConfiguration.class })
 * public class MyConfiguration extends WebMvcConfigurationSupport {
 *
 *     &#064;Override
 *     public void addFormatters(FormatterRegistry formatterRegistry) {
 *         formatterRegistry.addConverter(new MyConverter());
 *     }
 *
 *     &#064;Bean
 *     public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
 *         // Create or delegate to "super" to create and
 *         // customize properties of RequestMappingHandlerAdapter
 *         // 创建或委托给"super"来创建并定制RequestMappingHandlerAdapter的属性
 *     }
 * }
 * </pre>
 *
 * @author Dave Syer
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
 * @see org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebMvcConfiguration.class)
public @interface EnableWebMvc {
}
