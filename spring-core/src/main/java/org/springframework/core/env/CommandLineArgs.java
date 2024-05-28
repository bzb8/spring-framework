/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;

/**
 * A simple representation of command line arguments, broken into "option arguments" and
 * "non-option arguments".
 * <p>命令行参数的简单表示形式，分为"选项参数"和"非选项参数"。
 *
 * @author Chris Beams
 * @since 3.1
 * @see SimpleCommandLineArgsParser
 */
class CommandLineArgs {

	// 用于存储选项参数，键为选项名称，值为与该选项相关联的值列表。
	private final Map<String, List<String>> optionArgs = new HashMap<>();
	// 用于存储非选项参数的列表。
	private final List<String> nonOptionArgs = new ArrayList<>();

	/**
	 * Add an option argument for the given option name and add the given value to the
	 * list of values associated with this option (of which there may be zero or more).
	 * The given value may be {@code null}, indicating that the option was specified
	 * without an associated value (e.g. "--foo" vs. "--foo=bar").
	 * <p>为给定的选项名称添加一个选项参数，并将给定的值添加到与该选项相关联的值列表中。
	 * 值可以为{@code null}，表示该选项被指定但没有关联的值（例如"--foo"与"--foo=bar"）。
	 *
	 * @param optionName 选项名称。
	 * @param optionValue 与选项关联的值，可以为null。
	 */
	public void addOptionArg(String optionName, @Nullable String optionValue) {
		if (!this.optionArgs.containsKey(optionName)) {
			this.optionArgs.put(optionName, new ArrayList<>());
		}
		if (optionValue != null) {
			this.optionArgs.get(optionName).add(optionValue);
		}
	}

	/**
	 * Return the set of all option arguments present on the command line.
	 * 返回命令行上所有选项参数的集合。
	 */
	public Set<String> getOptionNames() {
		return Collections.unmodifiableSet(this.optionArgs.keySet());
	}

	/**
	 * Return whether the option with the given name was present on the command line.
	 * <p>返回给定名称的选项是否出现在命令行中。
	 */
	public boolean containsOption(String optionName) {
		return this.optionArgs.containsKey(optionName);
	}

	/**
	 * Return the list of values associated with the given option. {@code null} signifies
	 * that the option was not present; empty list signifies that no values were associated
	 * with this option.
	 * 返回与给定选项关联的值列表。{@code null}表示选项不存在；空列表表示没有与该选项关联的值。
	 */
	@Nullable
	public List<String> getOptionValues(String optionName) {
		return this.optionArgs.get(optionName);
	}

	/**
	 * Add the given value to the list of non-option arguments.
	 * 将给定的值添加到非选项参数列表中。
	 */
	public void addNonOptionArg(String value) {
		this.nonOptionArgs.add(value);
	}

	/**
	 * Return the list of non-option arguments specified on the command line.
	 * 返回命令行中指定的非选项参数列表。
	 */
	public List<String> getNonOptionArgs() {
		return Collections.unmodifiableList(this.nonOptionArgs);
	}

}
