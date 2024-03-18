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

package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract base regular expression pointcut bean. JavaBean properties are:
 * <ul>
 * <li>pattern: regular expression for the fully-qualified method names to match.
 * The exact regexp syntax will depend on the subclass (e.g. Perl5 regular expressions)
 * <li>patterns: alternative property taking a String array of patterns.
 * The result will be the union of these patterns.
 * </ul>
 *
 * <p>Note: the regular expressions must be a match. For example,
 * {@code .*get.*} will match com.mycom.Foo.getBar().
 * {@code get.*} will not.
 *
 * <p>This base class is serializable. Subclasses should declare all fields transient;
 * the {@link #initPatternRepresentation} method will be invoked again on deserialization.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 1.1
 * @see JdkRegexpMethodPointcut
 */
@SuppressWarnings("serial")
public abstract class AbstractRegexpMethodPointcut extends StaticMethodMatcherPointcut
		implements Serializable {

	/**
	 * Regular expressions to match.
	 * 匹配的正则表达式。如.*test.* 第一个.*表示所有路径，第二个.*表示的所有方法名，get表示以get开始的方法
	 */
	private String[] patterns = new String[0];

	/**
	 * Regular expressions <strong>not</strong> to match.
	 * 排除的正则表达式
	 */
	private String[] excludedPatterns = new String[0];


	/**
	 * Convenience method when we have only a single pattern.
	 * Use either this method or {@link #setPatterns}, not both.
	 * @see #setPatterns
	 */
	public void setPattern(String pattern) {
		setPatterns(pattern);
	}

	/**
	 * Set the regular expressions defining methods to match.
	 * Matching will be the union of all these; if any match, the pointcut matches.
	 * 设置要匹配的定义方法的正则表达式。匹配将是所有这些的并集；如果有匹配，则切入点匹配。
	 * @see #setPattern
	 */
	public void setPatterns(String... patterns) {
		Assert.notEmpty(patterns, "'patterns' must not be empty");
		this.patterns = new String[patterns.length];
		for (int i = 0; i < patterns.length; i++) {
			this.patterns[i] = StringUtils.trimWhitespace(patterns[i]);
		}
		initPatternRepresentation(this.patterns);
	}

	/**
	 * Return the regular expressions for method matching.
	 */
	public String[] getPatterns() {
		return this.patterns;
	}

	/**
	 * Convenience method when we have only a single exclusion pattern.
	 * Use either this method or {@link #setExcludedPatterns}, not both.
	 * @see #setExcludedPatterns
	 */
	public void setExcludedPattern(String excludedPattern) {
		setExcludedPatterns(excludedPattern);
	}

	/**
	 * Set the regular expressions defining methods to match for exclusion.
	 * Matching will be the union of all these; if any match, the pointcut matches.
	 * @see #setExcludedPattern
	 */
	public void setExcludedPatterns(String... excludedPatterns) {
		Assert.notEmpty(excludedPatterns, "'excludedPatterns' must not be empty");
		this.excludedPatterns = new String[excludedPatterns.length];
		for (int i = 0; i < excludedPatterns.length; i++) {
			this.excludedPatterns[i] = StringUtils.trimWhitespace(excludedPatterns[i]);
		}
		initExcludedPatternRepresentation(this.excludedPatterns);
	}

	/**
	 * Returns the regular expressions for exclusion matching.
	 */
	public String[] getExcludedPatterns() {
		return this.excludedPatterns;
	}


	/**
	 * Try to match the regular expression against the fully qualified name
	 * of the target class as well as against the method's declaring class,
	 * plus the name of the method.
	 * 尝试将正则表达式与目标类的完全限定名称以及方法的声明类以及方法的名称进行匹配。
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return (matchesPattern(ClassUtils.getQualifiedMethodName(method, targetClass)) ||
				(targetClass != method.getDeclaringClass() &&
						matchesPattern(ClassUtils.getQualifiedMethodName(method, method.getDeclaringClass()))));
	}

	/**
	 * Match the specified candidate against the configured patterns.
	 * 将指定的候选者与配置的模式进行匹配。
	 * @param signatureString "java.lang.Object.hashCode" style signature
	 * @return whether the candidate matches at least one of the specified patterns
	 */
	protected boolean matchesPattern(String signatureString) {
		for (int i = 0; i < this.patterns.length; i++) {
			// 如果给定的正则表达式匹配的话
			boolean matched = matches(signatureString, i);
			if (matched) {
				// 只要有一个排除的正则表达式不匹配就返回false
				for (int j = 0; j < this.excludedPatterns.length; j++) {
					boolean excluded = matchesExclusion(signatureString, j);
					if (excluded) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}


	/**
	 * Subclasses must implement this to initialize regexp pointcuts.
	 * Can be invoked multiple times.
	 * <p>This method will be invoked from the {@link #setPatterns} method,
	 * and also on deserialization.
	 * @param patterns the patterns to initialize
	 * @throws IllegalArgumentException in case of an invalid pattern
	 */
	protected abstract void initPatternRepresentation(String[] patterns) throws IllegalArgumentException;

	/**
	 * Subclasses must implement this to initialize regexp pointcuts.
	 * Can be invoked multiple times.
	 * <p>This method will be invoked from the {@link #setExcludedPatterns} method,
	 * and also on deserialization.
	 * @param patterns the patterns to initialize
	 * @throws IllegalArgumentException in case of an invalid pattern
	 */
	protected abstract void initExcludedPatternRepresentation(String[] patterns) throws IllegalArgumentException;

	/**
	 * Does the pattern at the given index match the given String?
	 * 给定索引处的模式是否与给定字符串匹配？
	 * @param pattern the {@code String} pattern to match -- 要匹配的模式
	 * @param patternIndex index of pattern (starting from 0) -- 模式索引（从0开始）
	 * @return {@code true} if there is a match, {@code false} otherwise
	 */
	protected abstract boolean matches(String pattern, int patternIndex);

	/**
	 * Does the exclusion pattern at the given index match the given String?
	 * @param pattern the {@code String} pattern to match
	 * @param patternIndex index of pattern (starting from 0)
	 * @return {@code true} if there is a match, {@code false} otherwise
	 */
	protected abstract boolean matchesExclusion(String pattern, int patternIndex);


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AbstractRegexpMethodPointcut)) {
			return false;
		}
		AbstractRegexpMethodPointcut otherPointcut = (AbstractRegexpMethodPointcut) other;
		return (Arrays.equals(this.patterns, otherPointcut.patterns) &&
				Arrays.equals(this.excludedPatterns, otherPointcut.excludedPatterns));
	}

	@Override
	public int hashCode() {
		int result = 27;
		for (String pattern : this.patterns) {
			result = 13 * result + pattern.hashCode();
		}
		for (String excludedPattern : this.excludedPatterns) {
			result = 13 * result + excludedPattern.hashCode();
		}
		return result;
	}

	@Override
	public String toString() {
		return getClass().getName() + ": patterns " + ObjectUtils.nullSafeToString(this.patterns) +
				", excluded patterns " + ObjectUtils.nullSafeToString(this.excludedPatterns);
	}

}
