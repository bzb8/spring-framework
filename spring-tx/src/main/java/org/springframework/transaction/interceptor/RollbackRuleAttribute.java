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

package org.springframework.transaction.interceptor;

import java.io.Serializable;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Rule determining whether a given exception should cause a rollback.
 *
 * <p>Multiple such rules can be applied to determine whether a transaction
 * should commit or rollback after an exception has been thrown.
 *
 * <p>Each rule is based on an exception pattern which can be a fully qualified
 * class name or a substring of a fully qualified class name for an exception
 * type (which must be a subclass of {@code Throwable}), with no wildcard support
 * at present. For example, a value of {@code "javax.servlet.ServletException"}
 * or {@code "ServletException"} would match {@code javax.servlet.ServletException}
 * and its subclasses.
 *
 * <p>An exception pattern can be specified as a {@link Class} reference or a
 * {@link String} in {@link #RollbackRuleAttribute(Class)} and
 * {@link #RollbackRuleAttribute(String)}, respectively. When an exception type
 * is specified as a class reference its fully qualified name will be used as the
 * pattern. See the javadocs for
 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
 * for further details on rollback rule semantics, patterns, and warnings regarding
 * possible unintentional matches.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 09.04.2003
 * @see NoRollbackRuleAttribute
 */
@SuppressWarnings("serial")
public class RollbackRuleAttribute implements Serializable{

	/**
	 * The {@linkplain RollbackRuleAttribute rollback rule} for
	 * {@link RuntimeException RuntimeExceptions}.
	 */
	public static final RollbackRuleAttribute ROLLBACK_ON_RUNTIME_EXCEPTIONS =
			new RollbackRuleAttribute(RuntimeException.class);


	/**
	 * Could hold exception, resolving class name but would always require FQN.
	 * This way does multiple string comparisons, but how often do we decide
	 * whether to roll back a transaction following an exception?
	 * 该字段用于存储异常的模式，这个模式是通过异常类的全限定名（FQN）来指定的。
	 * 当我们需要决定是否因异常而回滚事务时，会使用到这个模式进行异常类的匹配。
	 * 虽然这个过程中会进行多次字符串比较，但由于决定回滚事务的操作不常发生，
	 * 因此这种设计是可接受的。
	 *
	 */
	private final String exceptionPattern;


	/**
	 * Create a new instance of the {@code RollbackRuleAttribute} class
	 * for the given {@code exceptionType}.
	 * <p>This is the preferred way to construct a rollback rule that matches
	 * the supplied exception type, its subclasses, and its nested classes.
	 * <p>See the javadocs for
	 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
	 * for further details on rollback rule semantics, patterns, and warnings regarding
	 * possible unintentional matches.
	 * 为给定的{@code exceptionType}创建一个新的{@code RollbackRuleAttribute}类实例。
	 * <p>这是构建匹配给定异常类型、其子类和嵌套类的回滚规则的首选方式。
	 * <p>有关回滚规则语义、模式以及关于可能的意外匹配的警告的进一步详细信息，请参阅
	 * {@link org.springframework.transaction.annotation.Transactional @Transactional}的javadoc。
	 *
	 * @param exceptionType exception type; must be {@link Throwable} or a subclass
	 * of {@code Throwable} -- 异常类型；必须是{@link Throwable}或{@code Throwable}的子类
	 * @throws IllegalArgumentException if the supplied {@code exceptionType} is
	 * not a {@code Throwable} type or is {@code null}
	 * 如果提供的{@code exceptionType}不是{@code Throwable}类型或为{@code null}
	 *
	 */
	public RollbackRuleAttribute(Class<?> exceptionType) {
		Assert.notNull(exceptionType, "'exceptionType' cannot be null");
		if (!Throwable.class.isAssignableFrom(exceptionType)) {
			throw new IllegalArgumentException(
					"Cannot construct rollback rule from [" + exceptionType.getName() + "]: it's not a Throwable");
		}
		this.exceptionPattern = exceptionType.getName();
	}

	/**
	 * Create a new instance of the {@code RollbackRuleAttribute} class
	 * for the given {@code exceptionPattern}.
	 * <p>See the javadocs for
	 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
	 * for further details on rollback rule semantics, patterns, and warnings regarding
	 * possible unintentional matches.
	 * @param exceptionPattern the exception name pattern; can also be a fully
	 * package-qualified class name
	 * @throws IllegalArgumentException if the supplied {@code exceptionPattern}
	 * is {@code null} or empty
	 */
	public RollbackRuleAttribute(String exceptionPattern) {
		Assert.hasText(exceptionPattern, "'exceptionPattern' cannot be null or empty");
		this.exceptionPattern = exceptionPattern;
	}


	/**
	 * Get the configured exception name pattern that this rule uses for matching.
	 * @see #getDepth(Throwable)
	 */
	public String getExceptionName() {
		return this.exceptionPattern;
	}

	/**
	 * Return the depth of the superclass matching, with the following semantics.
	 * <ul>
	 * <li>{@code -1} means this rule does not match the supplied {@code exception}.</li>
	 * <li>{@code 0} means this rule matches the supplied {@code exception} directly.</li>
	 * <li>Any other positive value means this rule matches the supplied {@code exception}
	 * within the superclass hierarchy, where the value is the number of levels in the
	 * class hierarchy between the supplied {@code exception} and the exception against
	 * which this rule matches directly.</li>
	 * </ul>
	 * <p>When comparing roll back rules that match against a given exception, a rule
	 * with a lower matching depth wins. For example, a direct match ({@code depth == 0})
	 * wins over a match in the superclass hierarchy ({@code depth > 0}).
	 * <p>A match against a nested exception type or similarly named exception type
	 * will return a depth signifying a match at the corresponding level in the
	 * class hierarchy as if there had been a direct match.
	 * 返回匹配的超类深度，具有以下语义。
	 * <ul>
	 * <li>-1 表示此规则不匹配提供的 {@code exception}。</li>
	 * <li>0 表示此规则直接匹配提供的 {@code exception}。</li>
	 * <li>其他任何正整数值表示此规则在超类层次结构中匹配提供的 {@code exception}，
	 * 其中该值是提供的 {@code exception} 和此规则直接匹配的异常之间类层次结构中的级别数。</li>
	 * </ul>
	 * <p>比较匹配给定异常的回滚规则时，匹配深度较低的规则获胜。例如，直接匹配（{@code depth == 0}）
	 * 胜过在超类层次结构中匹配（{@code depth > 0}）。
	 * <p>匹配嵌套异常类型或同名异常类型将返回一个深度，表示在相应的类层次结构级别上进行了匹配，
	 * 就像存在直接匹配一样。
	 *
	 * @param exception 提供的异常，用于与规则进行匹配。
	 * @return 根据异常匹配的深度，参见方法详细描述中的语义部分。
	 */
	public int getDepth(Throwable exception) {
		return getDepth(exception.getClass(), 0);
	}


	private int getDepth(Class<?> exceptionType, int depth) {
		if (exceptionType.getName().contains(this.exceptionPattern)) {
			// Found it!
			return depth;
		}
		// If we've gone as far as we can go and haven't found it...
		// 如果我们已经走了很远，却没有找到它......
		if (exceptionType == Throwable.class) {
			return -1;
		}
		return getDepth(exceptionType.getSuperclass(), depth + 1);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RollbackRuleAttribute)) {
			return false;
		}
		RollbackRuleAttribute otherAttr = (RollbackRuleAttribute) other;
		return this.exceptionPattern.equals(otherAttr.exceptionPattern);
	}

	@Override
	public int hashCode() {
		return this.exceptionPattern.hashCode();
	}

	@Override
	public String toString() {
		return "RollbackRuleAttribute with pattern [" + this.exceptionPattern + "]";
	}

}
