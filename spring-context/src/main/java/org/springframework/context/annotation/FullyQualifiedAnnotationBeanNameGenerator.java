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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.util.Assert;

/**
 * An extension of {@code AnnotationBeanNameGenerator} that uses the fully qualified
 * class name as the default bean name if an explicit bean name is not supplied via
 * a supported type-level annotation such as {@code @Component} (see
 * {@link AnnotationBeanNameGenerator} for details on supported annotations).
 *
 * {@code AnnotationBeanNameGenerator} 的扩展，如果未通过受支持的类型级注解（如 {@code @Component}）提供显式 Bean 名称，
 * 则使用完全限定的类名作为缺省 Bean 名称（有关支持的注释的详细信息，请参阅 {@link AnnotationBeanNameGenerator}）。
 *
 * <p>Favor this bean naming strategy over {@code AnnotationBeanNameGenerator} if
 * you run into naming conflicts due to multiple autodetected components having the
 * same non-qualified class name (i.e., classes with identical names but residing in
 * different packages).
 *
 * 如果由于多个自动检测的组件具有相同的非限定类名（即具有相同名称但驻留在不同包中的类）而遇到命名冲突，则支持此 Bean 命名策略而不是 {@code AnnotationBeanNameGenerator}。
 *
 * <p>Note that an instance of this class is used by default for configuration-level
 * import purposes; whereas, the default for component scanning purposes is a plain
 * {@code AnnotationBeanNameGenerator}.
 *
 * 请注意，默认情况下，此类的实例用于配置级导入目的;而组件扫描的默认值是普通的 {@code AnnotationBeanNameGenerator}。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.2.3
 * @see org.springframework.beans.factory.support.DefaultBeanNameGenerator
 * @see AnnotationBeanNameGenerator
 * @see ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR
 */
public class FullyQualifiedAnnotationBeanNameGenerator extends AnnotationBeanNameGenerator {

	/**
	 * A convenient constant for a default {@code FullyQualifiedAnnotationBeanNameGenerator}
	 * instance, as used for configuration-level import purposes.
	 * 默认 FullyQualifiedAnnotationBeanNameGenerator 实例的方便常量，用于配置级导入目的。
	 * @since 5.2.11
	 */
	public static final FullyQualifiedAnnotationBeanNameGenerator INSTANCE =
			new FullyQualifiedAnnotationBeanNameGenerator();


	@Override
	protected String buildDefaultBeanName(BeanDefinition definition) {
		String beanClassName = definition.getBeanClassName();
		Assert.state(beanClassName != null, "No bean class name set");
		return beanClassName;
	}

}
