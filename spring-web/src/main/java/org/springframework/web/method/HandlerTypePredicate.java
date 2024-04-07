/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.method;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * A {@code Predicate} to match request handling component types if
 * <strong>any</strong> of the following selectors match:
 * <ul>
 * <li>Base packages -- for selecting handlers by their package.
 * <li>Assignable types -- for selecting handlers by supertype.
 * <li>Annotations -- for selecting handlers annotated in a specific way.
 * </ul>
 * 一个{@code Predicate}，用于匹配请求处理组件类型，如果以下任何选择器匹配：
 * <ul>
 * <li>基础包 - 用于按包选择处理器。
 * <li>可分配类型 - 用于按超类型选择处理器。
 * <li>注解 - 用于选择以特定方式注解的处理器。
 * </ul>
 * <p>Composability methods on {@link Predicate} can be used :
 * 可以使用{@link Predicate}上的组合方法：
 * <pre class="code">
 * Predicate&lt;Class&lt;?&gt;&gt; predicate =
 * 		HandlerTypePredicate.forAnnotation(RestController.class)
 * 				.and(HandlerTypePredicate.forBasePackage("org.example"));
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public final class HandlerTypePredicate implements Predicate<Class<?>> {

	private final Set<String> basePackages;

	private final List<Class<?>> assignableTypes;

	private final List<Class<? extends Annotation>> annotations;


	/**
	 * Private constructor. See static factory methods.
	 */
	private HandlerTypePredicate(Set<String> basePackages, List<Class<?>> assignableTypes,
			List<Class<? extends Annotation>> annotations) {

		this.basePackages = Collections.unmodifiableSet(basePackages);
		this.assignableTypes = Collections.unmodifiableList(assignableTypes);
		this.annotations = Collections.unmodifiableList(annotations);
	}


	/**
	 * 测试给定的类是否满足一定的条件。
	 * 该方法首先检查是否有选择器，如果没有，则认为测试通过；
	 * 如果有选择器，则进一步检查给定的类是否属于指定的基本包，
	 * 是否可分配给指定的类型，或者是否包含指定的注解。
	 *
	 * @param controllerType 待测试的类。可以为null。
	 * @return 如果给定的类满足至少一个条件，则返回true；否则返回false。
	 */
	@Override
	public boolean test(@Nullable Class<?> controllerType) {
		// 没有选择器时，直接返回true
		if (!hasSelectors()) {
			return true;
		}
		else if (controllerType != null) {
			// 检查类是否属于指定的基本包
			for (String basePackage : this.basePackages) {
				if (controllerType.getName().startsWith(basePackage)) {
					return true;
				}
			}
			// 检查类是否可分配给指定的类型
			for (Class<?> clazz : this.assignableTypes) {
				if (ClassUtils.isAssignable(clazz, controllerType)) {
					return true;
				}
			}
			// 检查类是否包含指定的注解
			for (Class<? extends Annotation> annotationClass : this.annotations) {
				if (AnnotationUtils.findAnnotation(controllerType, annotationClass) != null) {
					return true;
				}
			}
		}
		// 如果都不满足，则返回false
		return false;
	}

	private boolean hasSelectors() {
		return (!this.basePackages.isEmpty() || !this.assignableTypes.isEmpty() || !this.annotations.isEmpty());
	}


	// Static factory methods

	/**
	 * {@code Predicate} that applies to any handlers.
	 */
	public static HandlerTypePredicate forAnyHandlerType() {
		return new HandlerTypePredicate(
				Collections.emptySet(), Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Match handlers declared under a base package, e.g. "org.example".
	 * @param packages one or more base package names
	 */
	public static HandlerTypePredicate forBasePackage(String... packages) {
		return new Builder().basePackage(packages).build();
	}

	/**
	 * Type-safe alternative to {@link #forBasePackage(String...)} to specify a
	 * base package through a class.
	 * @param packageClasses one or more base package classes
	 */
	public static HandlerTypePredicate forBasePackageClass(Class<?>... packageClasses) {
		return new Builder().basePackageClass(packageClasses).build();
	}

	/**
	 * Match handlers that are assignable to a given type.
	 * @param types one or more handler supertypes
	 */
	public static HandlerTypePredicate forAssignableType(Class<?>... types) {
		return new Builder().assignableType(types).build();
	}

	/**
	 * Match handlers annotated with a specific annotation.
	 * @param annotations one or more annotations to check for
	 */
	@SafeVarargs
	public static HandlerTypePredicate forAnnotation(Class<? extends Annotation>... annotations) {
		return new Builder().annotation(annotations).build();
	}

	/**
	 * Return a builder for a {@code HandlerTypePredicate}.
	 */
	public static Builder builder() {
		return new Builder();
	}


	/**
	 * A {@link HandlerTypePredicate} builder.
	 */
	public static class Builder {
		/**
		 * 添加基础包
		 */
		private final Set<String> basePackages = new LinkedHashSet<>();

		private final List<Class<?>> assignableTypes = new ArrayList<>();

		private final List<Class<? extends Annotation>> annotations = new ArrayList<>();

		/**
		 * Match handlers declared under a base package, e.g. "org.example".
		 * 配置基于给定的基本包搜索匹配的处理器。
		 * 例如，给定"org.example"作为基本包，此方法将匹配该包及其子包下声明的所有处理器。
		 *
		 * @param packages one or more base package classes
		 */
		public Builder basePackage(String... packages) {
			// 对传入的包名数组进行流式处理，过滤掉空或空字符串的包名，并添加到基础包列表中
			Arrays.stream(packages).filter(StringUtils::hasText).forEach(this::addBasePackage);
			return this;
		}

		/**
		 * Type-safe alternative to {@link #forBasePackage(String...)} to specify a
		 * base package through a class.
		 * @param packageClasses one or more base package names
		 */
		public Builder basePackageClass(Class<?>... packageClasses) {
			Arrays.stream(packageClasses).forEach(clazz -> addBasePackage(ClassUtils.getPackageName(clazz)));
			return this;
		}

		private void addBasePackage(String basePackage) {
			this.basePackages.add(basePackage.endsWith(".") ? basePackage : basePackage + ".");
		}

		/**
		 * Match handlers that are assignable to a given type.
		 * @param types one or more handler supertypes
		 */
		public Builder assignableType(Class<?>... types) {
			this.assignableTypes.addAll(Arrays.asList(types));
			return this;
		}

		/**
		 * Match types that are annotated with one of the given annotations.
		 * @param annotations one or more annotations to check for
		 */
		@SuppressWarnings("unchecked")
		public final Builder annotation(Class<? extends Annotation>... annotations) {
			this.annotations.addAll(Arrays.asList(annotations));
			return this;
		}

		public HandlerTypePredicate build() {
			return new HandlerTypePredicate(this.basePackages, this.assignableTypes, this.annotations);
		}
	}

}
