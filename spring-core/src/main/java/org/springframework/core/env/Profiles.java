/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.env;

import java.util.function.Predicate;

/**
 * Profile predicate that may be {@linkplain Environment#acceptsProfiles(Profiles)
 * accepted} by an {@link Environment}.
 * <p>用于环境条件判断的接口，判断某个环境是否接受特定的配置文件。
 * 可以通过 {@linkplain Environment#acceptsProfiles(Profiles) 环境接受配置文件}的方法进行测试。
 *
 * <p>May be implemented directly or, more usually, created using the
 * {@link #of(String...) of(...)} factory method.
 * <p>可以直接实现此接口，或者更常见地使用 {@link #of(String...) of(...)} 工厂方法创建实例。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.1
 */
@FunctionalInterface
public interface Profiles {

	/**
	 * Test if this {@code Profiles} instance <em>matches</em> against the given
	 * active profiles predicate.
	 * <p>测试当前配置文件实例是否与给定的活动配置文件条件匹配。
	 * @param activeProfiles a predicate that tests whether a given profile is
	 * currently active
	 *                       用于测试给定配置文件是否当前活动的谓词。
	 * @return 如果当前配置文件与给定的活动配置文件条件匹配，则返回 {@code true}。
	 */
	boolean matches(Predicate<String> activeProfiles);


	/**
	 * Create a new {@link Profiles} instance that checks for matches against
	 * the given <em>profile expressions</em>.
	 * <p>The returned instance will {@linkplain Profiles#matches(Predicate) match}
	 * if any one of the given profile expressions matches.
	 * <p>A profile expression may contain a simple profile name (for example
	 * {@code "production"}) or a compound expression. A compound expression allows
	 * for more complicated profile logic to be expressed, for example
	 * {@code "production & cloud"}.
	 * <p>The following operators are supported in profile expressions.
	 * <ul>
	 * <li>{@code !} - A logical <em>NOT</em> of the profile name or compound expression</li>
	 * <li>{@code &} - A logical <em>AND</em> of the profile names or compound expressions</li>
	 * <li>{@code |} - A logical <em>OR</em> of the profile names or compound expressions</li>
	 * </ul>
	 * <p>Please note that the {@code &} and {@code |} operators may not be mixed
	 * without using parentheses. For example, {@code "a & b | c"} is not a valid
	 * expression: it must be expressed as {@code "(a & b) | c"} or
	 * {@code "a & (b | c)"}.
	 * <p>As of Spring Framework 5.1.17, two {@code Profiles} instances returned
	 * by this method are considered equivalent to each other (in terms of
	 * {@code equals()} and {@code hashCode()} semantics) if they are created
	 * with identical <em>profile expressions</em>.
	 * <p>根据给定的<em>配置文件表达式</em>创建一个新的 {@link Profiles} 实例。
	 * <p>返回的实例将匹配任何给定的配置文件表达式。
	 * <p>配置文件表达式可以是简单的配置文件名称（例如 {@code "production"}）或复合表达式。
	 * 复合表达式允许表达更复杂的配置文件逻辑，例如 {@code "production & cloud"}。
	 * <p>配置文件表达式中支持以下操作符。
	 * <ul>
	 * <li>{@code !} - 配置文件名称或复合表达式的逻辑 <em>非</em></li>
	 * <li>{@code &} - 配置文件名称或复合表达式的逻辑 <em>与</em></li>
	 * <li>{@code |} - 配置文件名称或复合表达式的逻辑 <em>或</em></li>
	 * </ul>
	 * <p>请注意，{@code &} 和 {@code |} 操作符在不使用括号的情况下可能不能混用。
	 * 例如，{@code "a & b | c"} 不是一个有效的表达式：它必须表示为 {@code "(a & b) | c"} 或者
	 * {@code "a & (b | c)"}。
	 * <p>自 Spring Framework 5.1.17 起，通过此方法创建的两个 {@code Profiles} 实例被认为是等价的（就 {@code equals()} 和
	 * {@code hashCode()} 的语义而言），如果它们是使用相同的 <em>配置文件表达式</em> 创建的。
	 * @param profileExpressions the <em>profile expressions</em> to include
	 *                           要包括的<em>配置文件表达式</em>
	 * @return a new {@link Profiles} instance
	 * 一个新的 {@link Profiles} 实例
	 */
	static Profiles of(String... profileExpressions) {
		return ProfilesParser.parse(profileExpressions);
	}

}
