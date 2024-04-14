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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Internal parser used by {@link Profiles#of}.
 * 由{@link Profiles#of}内部使用的解析器。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.1
 */
final class ProfilesParser {

	private ProfilesParser() {
	}

	/**
	 * 解析给定的profile表达式。
	 *
	 * @param expressions at least one profile expression must be specified.
	 * @return a {@link Profiles} instance representing the parsed expressions.
	 * @throws IllegalArgumentException if no expressions are provided.
	 */

	static Profiles parse(String... expressions) {
		Assert.notEmpty(expressions, "Must specify at least one profile expression");
		Profiles[] parsed = new Profiles[expressions.length];
		for (int i = 0; i < expressions.length; i++) {
			parsed[i] = parseExpression(expressions[i]);
		}
		return new ParsedProfiles(expressions, parsed);
	}

	/**
	 * 解析单个profile表达式。
	 *
	 * @param expression the expression to parse.
	 * @return a {@link Profiles} instance representing the parsed expression.
	 * @throws IllegalArgumentException if the expression is invalid.
	 */
	private static Profiles parseExpression(String expression) {
		Assert.hasText(expression, () -> "Invalid profile expression [" + expression + "]: must contain text");
		StringTokenizer tokens = new StringTokenizer(expression, "()&|!", true);
		return parseTokens(expression, tokens);
	}
	/**
	 * 使用给定的tokenizer解析profile表达式。
	 *
	 * @param expression the original expression.
	 * @param tokens     the tokenizer for the expression.
	 * @return a {@link Profiles} instance representing the parsed tokens.
	 */
	private static Profiles parseTokens(String expression, StringTokenizer tokens) {
		return parseTokens(expression, tokens, Context.NONE);
	}
	/**
	 * 使用给定的tokenizer和上下文解析profile表达式。
	 *
	 * @param expression  the original expression.
	 * @param tokens      the tokenizer for the expression.
	 * @param context     the parsing context.
	 * @return a {@link Profiles} instance representing the parsed tokens.
	 */
	private static Profiles parseTokens(String expression, StringTokenizer tokens, Context context) {
		List<Profiles> elements = new ArrayList<>();
		Operator operator = null;
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			if (token.isEmpty()) {
				continue;
			}
			switch (token) {
				case "(":
					Profiles contents = parseTokens(expression, tokens, Context.PARENTHESIS);
					if (context == Context.NEGATE) {
						return contents;
					}
					elements.add(contents);
					break;
				case "&":
					assertWellFormed(expression, operator == null || operator == Operator.AND);
					operator = Operator.AND;
					break;
				case "|":
					assertWellFormed(expression, operator == null || operator == Operator.OR);
					operator = Operator.OR;
					break;
				case "!":
					elements.add(not(parseTokens(expression, tokens, Context.NEGATE)));
					break;
				case ")":
					Profiles merged = merge(expression, elements, operator);
					if (context == Context.PARENTHESIS) {
						return merged;
					}
					elements.clear();
					elements.add(merged);
					operator = null;
					break;
				default:
					Profiles value = equals(token);
					if (context == Context.NEGATE) {
						return value;
					}
					elements.add(value);
			}
		}
		return merge(expression, elements, operator);
	}
	/**
	 * 合并解析的profile元素。
	 *
	 * @param expression  the original expression.
	 * @param elements    the list of parsed elements.
	 * @param operator    the current logical operator.
	 * @return a {@link Profiles} instance representing the merged elements.
	 * @throws IllegalArgumentException if the expression is malformed.
	 */
	private static Profiles merge(String expression, List<Profiles> elements, @Nullable Operator operator) {
		assertWellFormed(expression, !elements.isEmpty());
		if (elements.size() == 1) {
			return elements.get(0);
		}
		Profiles[] profiles = elements.toArray(new Profiles[0]);
		return (operator == Operator.AND ? and(profiles) : or(profiles));
	}
	/**
	 * 确保表达式符合形成规则。
	 *
	 * @param expression the expression being parsed.
	 * @param wellFormed whether the current state is well formed.
	 * @throws IllegalArgumentException if the expression is malformed.
	 */

	private static void assertWellFormed(String expression, boolean wellFormed) {
		Assert.isTrue(wellFormed, () -> "Malformed profile expression [" + expression + "]");
	}
	/**
	 * 创建表示逻辑或的Profiles实例。
	 *
	 * @param profiles the profiles to join with OR.
	 * @return a {@link Profiles} instance representing the logical OR of the given profiles.
	 */
	private static Profiles or(Profiles... profiles) {
		return activeProfile -> Arrays.stream(profiles).anyMatch(isMatch(activeProfile));
	}
	/**
	 * 创建表示逻辑与的Profiles实例。
	 *
	 * @param profiles the profiles to join with AND.
	 * @return a {@link Profiles} instance representing the logical AND of the given profiles.
	 */
	private static Profiles and(Profiles... profiles) {
		return activeProfile -> Arrays.stream(profiles).allMatch(isMatch(activeProfile));
	}

	private static Profiles not(Profiles profiles) {
		return activeProfile -> !profiles.matches(activeProfile);
	}
	/**
	 * 创建表示等于的Profiles实例。
	 *
	 * @param profile the profile to match.
	 * @return a {@link Profiles} instance representing equality with the given profile.
	 */
	private static Profiles equals(String profile) {
		return activeProfile -> activeProfile.test(profile);
	}
	/**
	 * 创建一个匹配给定活动profile的predicate。
	 *
	 * @param activeProfiles the active profile predicate.
	 * @return a predicate that matches the given profiles.
	 */
	private static Predicate<Profiles> isMatch(Predicate<String> activeProfiles) {
		return profiles -> profiles.matches(activeProfiles);
	}

	/**
	 * 表示逻辑操作符的枚举。
	 */
	private enum Operator { AND, OR }
	/**
	 * 解析上下文的枚举。
	 */
	private enum Context { NONE, NEGATE, PARENTHESIS }

	/**
	 * Profile表达式解析结果的内部类。
	 */
	private static class ParsedProfiles implements Profiles {
		// 原始的profiles表达式
		private final Set<String> expressions = new LinkedHashSet<>();
		// 解析后的profiles表达式
		private final Profiles[] parsed;

		ParsedProfiles(String[] expressions, Profiles[] parsed) {
			Collections.addAll(this.expressions, expressions);
			this.parsed = parsed;
		}
		/**
		 * 检查给定的活动profiles是否匹配任何解析的表达式。
		 *
		 * @param activeProfiles the active profiles to match.
		 * @return whether the active profiles match any of the parsed expressions.
		 */
		@Override
		public boolean matches(Predicate<String> activeProfiles) {
			for (Profiles candidate : this.parsed) {
				if (candidate.matches(activeProfiles)) {
					return true;
				}
			}
			return false;
		}
		/**
		 * 用于equals和hashCode实现的表达式集合。
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			ParsedProfiles that = (ParsedProfiles) obj;
			return this.expressions.equals(that.expressions);
		}

		@Override
		public int hashCode() {
			return this.expressions.hashCode();
		}
		/**
		 * 将解析的表达式集合转换为字符串表示。
		 */
		@Override
		public String toString() {
			return StringUtils.collectionToDelimitedString(this.expressions, " or ");
		}
	}

}
